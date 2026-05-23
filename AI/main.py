"""
보안 위협 탐지 AI 모델 - FastAPI 서버 (최종본)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
데이터셋  : CSIC 2010
모델      : LightGBM (model_bundle.pkl)
피처      : 7개 (노트북 scis2010.ipynb와 동일)

[팀 합의 연동 구조]
  Spring → FastAPI : 파싱된 HTTP 요청 데이터 7개 피처 + ip_address + timestamp
  FastAPI → Spring : threat_score(위험지수), ip_address, reason(이유) 3가지 반환
                                 정상 요청이면 ip_address = "0.0.0.0", reason = "-"

[Spring 서버 연동 엔드포인트]
  POST /predict         단건 분석  ← 백엔드가 호출할 주소
  GET  /health          서버 상태 확인
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import joblib
import numpy as np
import os

# ─────────────────────────────────────────────────────────
# Kafka 설정 부분
import json
import threading
import time
from kafka import KafkaConsumer, KafkaProducer

kafka_stop_event = threading.Event()
kafka_consumer = None
kafka_producer = None

# AI 서버 로컬 실행 -> localhost:9092 (현재)
# AI 서버 Docker 실행 -> kafka:29092 (AI 서버도 Docker 컨테이너에 올릴 시 이걸로 변경할 수도 있음)
KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
AI_REQUEST_TOPIC = "ai-request-topic"
AI_RESULT_TOPIC = "ai-result-topic"
AI_CONSUMER_GROUP = "ai-service-group"
# ─────────────────────────────────────────────────────────

# ─────────────────────────────────────────────────────────
# 공격 판단 기준 상수
# (train_and_save_model.py 실행 후 출력되는 값으로 교체)
# ─────────────────────────────────────────────────────────
ATTACK_KEYWORDS        = ['select', 'insert', 'drop', 'script',
                           'alert', 'union', 'exec', '../']
SPECIAL_CHARS          = ["'", '"', '<', '>', '--', ';', '%']
SPECIAL_CHAR_THRESHOLD = 5      # 특수문자 5개 초과 시 "과다"로 판단
URL_LEN_MEAN           = 77.1   # ← train_and_save_model.py 실행 후 출력값으로 교체
URL_LEN_STD            = 70.2   # ← train_and_save_model.py 실행 후 출력값으로 교체

# ─────────────────────────────────────────────────────────
# FastAPI 앱
# ─────────────────────────────────────────────────────────
app = FastAPI(
    title="보안 위협 탐지 API",
    description="Spring 서버로부터 HTTP 요청 데이터를 받아 위협 여부를 판별합니다.",
    version="4.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],  # Spring 서버 주소
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─────────────────────────────────────────────────────────
# 모델 번들 로딩 (서버 시작 시 1회)
# model_bundle.pkl = model + le_method + le_ua + le_path
# ─────────────────────────────────────────────────────────
BUNDLE_PATH = "model_bundle.pkl"
model     = None
le_method = None
le_ua     = None
le_path   = None

@app.on_event("startup")
def load_model():
    global model, le_method, le_ua, le_path
    if os.path.exists(BUNDLE_PATH):
        bundle    = joblib.load(BUNDLE_PATH)
        model     = bundle["model"]
        le_method = bundle["le_method"]
        le_ua     = bundle["le_ua"]
        le_path   = bundle["le_path"]
        print(f"✅ 모델 번들 로드 완료: {BUNDLE_PATH}")
    else:
        print(f"⚠️  모델 번들 없음: {BUNDLE_PATH}")
        print(f"   → train_and_save_model.py 를 먼저 실행하세요")
    # ─────────────────────────────────────────────────────────
    # Kafka worker thread
    # -> FastAPI 서버가 켜지면, Kafka consumer도 백그라운드에서 같이 켜짐.
    thread = threading.Thread(target=kafka_worker,daemon=True)
    thread.start()
    print("[Kafka AI] background worker started")
    # ─────────────────────────────────────────────────────────


# ─────────────────────────────────────────────────────────
# LabelEncoder 헬퍼
# 학습 때 없던 URL/UA는 'UNKNOWN' 클래스로 대체
# ─────────────────────────────────────────────────────────
def safe_encode(encoder, value: str) -> int:
    if value in encoder.classes_:
        return int(encoder.transform([value])[0])
    return int(encoder.transform(["UNKNOWN"])[0])

# ─────────────────────────────────────────────────────────
# 요청 스키마  (Spring boot → FastAPI)
#
# 나균씨가 Nginx 로그를 파싱해서 이 JSON 형태로 보내줍니다.
# url_len / special_char_count : 나균씨가 계산해서 보내도 되고,
#                                0으로 보내면 FastAPI에서 자동 계산합니다.
# ─────────────────────────────────────────────────────────
class HttpRequestData(BaseModel):
    log_id: int | None = None       # 추가된 데이터 컬럼 (Spring <-> Kafka <-> AI Server 통신을 위해 필요함)
    # ── 7개 피처 (이미지의 train 데이터 컬럼과 동일) ──────
    method:             str         # "GET" | "POST" | "PUT" | "DELETE"
    url_path:           str         # "/tienda1/publico/login.jsp"
    query_params:       str = ""    # URL 파라미터 (?뒤)
    body_content:       str = ""    # POST body 내용
    user_agent:         str = ""    # User-Agent 헤더값
    url_len:            int = 0     # URL 전체 길이 (0이면 자동 계산)
    special_char_count: int = 0     # 특수문자 개수  (0이면 자동 계산)
    # ── 추가 컬럼 ──────────────────────────────────────
    ip_address:         str = ""    # 요청 IP  (ex. "192.168.0.10")
    timestamp:          str = ""    # 요청 시각 (ex. "2026-03-31T14:00:00")

# ─────────────────────────────────────────────────────────
# 응답 스키마  (현욱 → 나균)
#
# 팀 합의 3가지만 반환합니다.
#   threat_score : 공격일 확률 그대로 (0.0 ~ 1.0)
#   ip_address   : 공격이면 실제 IP, 정상이면 "0.0.0.0"
#   reason       : 왜 위협인지 (정상이면 "-")
# ─────────────────────────────────────────────────────────
class ThreatResponse(BaseModel):
    log_id: int | None = None  # 추가된 데이터 컬럼 (Spring <-> Kafka <-> AI Server 통신을 위해 필요함)
    threat_score: float   # ex) 0.8700
    ip_address:   str     # ex) "192.168.0.10"  또는  "0.0.0.0"
    reason:       str     # ex) "URL 공격 키워드, 특수문자 과다(7개)"  또는  "-"

# ─────────────────────────────────────────────────────────
# 피처 추출  (노트북 7개 피처 순서와 완전히 일치)
# ─────────────────────────────────────────────────────────
def extract_features(req: HttpRequestData) -> np.ndarray:
    full_url = req.url_path + ("?" + req.query_params if req.query_params else "")

    # url_len, special_char_count : 나균씨가 보내준 값 우선, 0이면 직접 계산
    url_len = req.url_len if req.url_len > 0 else len(full_url)

    special_char_count = req.special_char_count if req.special_char_count > 0 else (
        sum(full_url.count(c)        for c in SPECIAL_CHARS) +
        sum(req.body_content.count(c) for c in SPECIAL_CHARS)
    )

    has_keywords_query = 1 if any(kw in req.query_params.lower()   for kw in ATTACK_KEYWORDS) else 0
    has_keywords_body  = 1 if any(kw in req.body_content.lower()   for kw in ATTACK_KEYWORDS) else 0

    method_enc = safe_encode(le_method, req.method.upper())
    ua_enc     = safe_encode(le_ua,     req.user_agent)
    path_enc   = safe_encode(le_path,   req.url_path)

    # 노트북 FEATURES 리스트 순서와 반드시 일치
    # ['method_encoded', 'user_agent_encoded', 'url_path_encoded',
    #  'url_len', 'special_char_count', 'has_keywords_query', 'has_keywords_body']
    return np.array([[
        method_enc,
        ua_enc,
        path_enc,
        url_len,
        special_char_count,
        has_keywords_query,
        has_keywords_body,
    ]])

# ─────────────────────────────────────────────────────────
# 공격 이유 생성  (노트북 Cell 26, 39의 get_attack_reason 로직 그대로)
# ─────────────────────────────────────────────────────────
def get_attack_reason(
    threat_score:       float,
    has_keywords_query: int,
    has_keywords_body:  int,
    special_char_count: int,
    url_len:            int,
) -> str:
    # 정상 판단 (50% 미만) → 이유 없음
    if threat_score < 0.5:
        return "-"

    reasons = []

    if has_keywords_query == 1:
        reasons.append("URL 공격 키워드")
    if has_keywords_body == 1:
        reasons.append("Body 공격 키워드")
    if special_char_count > SPECIAL_CHAR_THRESHOLD:
        reasons.append(f"특수문자 과다({special_char_count}개)")
    if url_len > (URL_LEN_MEAN + URL_LEN_STD):
        reasons.append("비정상적 URL 길이")

    # 위 조건 없이도 점수가 높으면 → 복합 패턴
    if not reasons:
        reasons.append("복합적인 패턴 이상 (Path/Method/UA)")

    return ", ".join(reasons)

# ─────────────────────────────────────────────────────────
# 공통 예측 로직 (단건 / 배치 공통 사용)
# ─────────────────────────────────────────────────────────
def run_predict(req: HttpRequestData) -> ThreatResponse:
    features    = extract_features(req)
    probability = float(model.predict_proba(features)[0][1])
    is_attack   = probability >= 0.5

    # 이유 판단에 필요한 값 계산
    full_url = req.url_path + ("?" + req.query_params if req.query_params else "")
    url_len  = req.url_len if req.url_len > 0 else len(full_url)
    scc      = req.special_char_count if req.special_char_count > 0 else (
        sum(full_url.count(c)         for c in SPECIAL_CHARS) +
        sum(req.body_content.count(c) for c in SPECIAL_CHARS)
    )
    hkq = 1 if any(kw in req.query_params.lower()  for kw in ATTACK_KEYWORDS) else 0
    hkb = 1 if any(kw in req.body_content.lower()  for kw in ATTACK_KEYWORDS) else 0

    return ThreatResponse(   
        log_id       = req.log_id,  # 추가된 부분 (Spring <-> Kafka <-> AI Server 통신을 위해 필요함)
        threat_score = round(probability, 4),
        ip_address   = req.ip_address if is_attack else "0.0.0.0",
        reason       = get_attack_reason(probability, hkq, hkb, scc, url_len),
    )


# ─────────────────────────────────────────────────────────
# Kafka에서 받은 JSON dict를 HttpRequestData로 검증합고, 예측한 다음 dict로 바꿔 반환합니다.
# 즉, Kafka 메시지 하나를 처리하는 최소 단위.
def handle_kafka_message(message: dict) -> dict:
    request = HttpRequestData(**message)
    result = run_predict(request)
    return result.dict()
# ─────────────────────────────────────────────────────────

@app.on_event("shutdown")
def stop_kafka_worker():
    kafka_stop_event.set()
    if kafka_consumer is not None:
        try:
            kafka_consumer.close()
        except Exception:
            pass
    if kafka_producer is not None:
        try:
            kafka_producer.close()
        except Exception:
            pass
    print("[Kafka AI] background worker stopping")

def kafka_worker():
    global kafka_consumer, kafka_producer

    while model is None and not kafka_stop_event.is_set():
        print("[Kafka AI] 모델 로딩 대기 중 ...")
        time.sleep(1)

    while not kafka_stop_event.is_set():
        try:
            # 1) Kafka Consumer 생성
            kafka_consumer = KafkaConsumer(
                AI_REQUEST_TOPIC,
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                group_id=AI_CONSUMER_GROUP,
                auto_offset_reset="earliest",
                enable_auto_commit=True,
                value_deserializer=lambda v: json.loads(v.decode("utf-8")),
            )
            # 2) Kafka Producer 생성
            kafka_producer = KafkaProducer(
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
            )

            print(f"[Kafka AI] consume start: {AI_REQUEST_TOPIC}")

            # 3) 메시지를 하나씩 처리
            for record in kafka_consumer:
                if kafka_stop_event.is_set():
                    break

                try:
                    # ai-request-topic 메시지 수신
                    # -> 예측
                    # -> ai-result-topic으로 결과 전송
                    request_message = record.value  
                    result_message = handle_kafka_message(request_message)

                    kafka_producer.send(
                        AI_RESULT_TOPIC,
                        key=str(result_message.get("log_id", "")).encode("utf-8"),
                        value=result_message,
                    )
                    kafka_producer.flush()

                    print(
                        f"[Kafka AI] result sent. "
                        f"log_id={result_message.get('log_id')} "
                        f"score={result_message.get('threat_score')}"
                    )
                except Exception as e:
                    print(f"[Kafka AI Error] message 처리 실패: {e}")

        except Exception as e:
            print(f"[Kafka AI Error] Kafka worker exception: {e}")

        finally:
            if kafka_consumer is not None:
                try:
                    kafka_consumer.close()
                except Exception:
                    pass
                kafka_consumer = None
            if kafka_producer is not None:
                try:
                    kafka_producer.close()
                except Exception:
                    pass
                kafka_producer = None

        if kafka_stop_event.is_set():
                break
        
        time.sleep(2)


# ─────────────────────────────────────────────────────────

# ─────────────────────────────────────────────────────────
# API 엔드포인트
# ─────────────────────────────────────────────────────────
@app.get("/")
def root():
    return {"status": "running", "message": "보안 위협 탐지 API 서버 (최종본) 실행 중"}

@app.get("/health")
def health_check():
    """나균씨가 FastAPI 서버 생존 여부를 확인할 때 사용"""
    return {"status": "healthy", "model_loaded": model is not None}

@app.post("/predict", response_model=ThreatResponse)
def predict(request: HttpRequestData):
    """
    [나균 → 현욱]  파싱된 HTTP 요청 데이터 수신
    [현욱 → 나균]  threat_score / ip_address / reason 반환

    ※ 나균씨가 호출할 주소 : POST http://localhost:8000/predict
    """
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="모델이 로드되지 않았습니다. model_bundle.pkl 확인 후 서버를 재시작하세요.",
        )
    return run_predict(request)

@app.post("/predict/batch")
def predict_batch(requests: list[HttpRequestData]):
    """
    여러 요청을 한꺼번에 분석 (배치)
    나균씨가 로그를 모아서 한 번에 보낼 때 사용
    """
    if model is None:
        raise HTTPException(status_code=503, detail="모델이 로드되지 않았습니다.")
    results = [run_predict(req).dict() for req in requests]
    return {"total": len(results), "results": results}
