"""
보안 위협 탐지 AI 모델 - FastAPI 서버
CSIC 2010 데이터셋 기반 HTTP 요청 분류 API
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import joblib
import numpy as np
import re
import os
from typing import Optional

# ──────────────────────────────────────────────
# 1. FastAPI 앱 생성
# ──────────────────────────────────────────────
app = FastAPI(
    title="보안 위협 탐지 API",
    description="HTTP 요청을 분석하여 공격(Attack) 여부를 판별하는 AI 모델 서버",
    version="1.0.0"
)

# ──────────────────────────────────────────────
# 2. CORS 설정 (Spring 서버가 호출할 수 있도록)
#    Spring 서버 주소를 allow_origins에 추가하세요
# ──────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],  # Spring 서버 주소
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ──────────────────────────────────────────────
# 3. 모델 로딩 (서버 시작 시 1회만 실행)
# ──────────────────────────────────────────────
MODEL_PATH = "lightgbm_model.pkl"
model = None

@app.on_event("startup")
def load_model():
    """서버 시작 시 학습된 모델을 메모리에 로드"""
    global model
    if os.path.exists(MODEL_PATH):
        model = joblib.load(MODEL_PATH)
        print(f"✅ 모델 로드 완료: {MODEL_PATH}")
    else:
        print(f"⚠️  모델 파일 없음: {MODEL_PATH} — /train 엔드포인트로 학습 필요")

# ──────────────────────────────────────────────
# 4. 요청/응답 데이터 스키마 정의 (Pydantic)
#    Spring 서버가 JSON으로 이 형태를 보내야 함
# ──────────────────────────────────────────────
class HttpRequest(BaseModel):
    """Spring 서버에서 보내는 HTTP 요청 정보"""
    method: str               # "GET" or "POST"
    url_path: str             # "/tienda1/publico/login.jsp"
    query_params: str = ""    # "id=1&name=test"
    body_content: str = ""    # POST body
    user_agent: str = ""      # User-Agent 헤더값

class PredictionResponse(BaseModel):
    """FastAPI가 Spring에 돌려주는 분석 결과"""
    is_attack: bool           # True = 공격, False = 정상
    label: int                # 1 = 공격, 0 = 정상
    probability: float        # 공격일 확률 (0.0 ~ 1.0)
    risk_level: str           # "HIGH" / "MEDIUM" / "LOW"
    url_len: int              # 분석에 사용된 URL 길이
    special_char_count: int   # 탐지된 특수문자 수

# ──────────────────────────────────────────────
# 5. 피처 추출 함수 (노트북의 parse_csic_file 로직과 동일)
# ──────────────────────────────────────────────
def extract_features(req: HttpRequest) -> np.ndarray:
    """
    HTTP 요청 객체 → 모델 입력 피처 배열 변환
    노트북에서 사용한 8개 컬럼 중 수치형 피처만 추출
    """
    # URL 전체 조합
    full_url = req.url_path
    if req.query_params:
        full_url += "?" + req.query_params

    # url_len: URL 전체 길이
    url_len = len(full_url)

    # special_char_count: SQL인젝션/XSS 의심 특수문자 수
    special_chars = ["'", '"', '<', '>', '--', ';', '%']
    special_char_count = (
        sum(full_url.count(c) for c in special_chars) +
        sum(req.body_content.count(c) for c in special_chars)
    )

    # method를 숫자로 (GET=0, POST=1)
    method_num = 1 if req.method.upper() == "POST" else 0

    # body 길이
    body_len = len(req.body_content)

    # 피처 배열 반환 (노트북 학습 시 사용한 컬럼 순서와 일치해야 함)
    return np.array([[method_num, url_len, special_char_count, body_len]])

def get_risk_level(probability: float) -> str:
    if probability >= 0.7:
        return "HIGH"
    elif probability >= 0.4:
        return "MEDIUM"
    else:
        return "LOW"

# ──────────────────────────────────────────────
# 6. API 엔드포인트 정의
# ──────────────────────────────────────────────

@app.get("/")
def root():
    """서버 상태 확인용 헬스체크"""
    return {"status": "running", "message": "보안 위협 탐지 API 서버가 실행 중입니다"}

@app.get("/health")
def health_check():
    """Spring 서버가 FastAPI 서버 생존 여부를 주기적으로 확인할 때 사용"""
    return {
        "status": "healthy",
        "model_loaded": model is not None
    }

@app.post("/predict", response_model=PredictionResponse)
def predict(request: HttpRequest):
    """
    HTTP 요청 정보를 받아 공격 여부를 예측
    
    Spring 서버에서 이 엔드포인트를 POST로 호출합니다.
    """
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="모델이 로드되지 않았습니다. 서버 관리자에게 문의하세요."
        )

    # 피처 추출
    features = extract_features(request)

    # 모델 예측
    label = int(model.predict(features)[0])
    probability = float(model.predict_proba(features)[0][1])  # 공격일 확률

    # 응답 구성
    full_url = request.url_path + ("?" + request.query_params if request.query_params else "")
    special_chars = ["'", '"', '<', '>', '--', ';', '%']
    special_char_count = (
        sum(full_url.count(c) for c in special_chars) +
        sum(request.body_content.count(c) for c in special_chars)
    )

    return PredictionResponse(
        is_attack=(label == 1),
        label=label,
        probability=round(probability, 4),
        risk_level=get_risk_level(probability),
        url_len=len(full_url),
        special_char_count=special_char_count
    )

@app.post("/predict/batch")
def predict_batch(requests: list[HttpRequest]):
    """
    여러 요청을 한꺼번에 분석 (배치 처리)
    트래픽 로그를 일괄 검사할 때 유용
    """
    if model is None:
        raise HTTPException(status_code=503, detail="모델이 로드되지 않았습니다.")

    results = []
    for req in requests:
        features = extract_features(req)
        label = int(model.predict(features)[0])
        probability = float(model.predict_proba(features)[0][1])
        results.append({
            "url_path": req.url_path,
            "is_attack": label == 1,
            "probability": round(probability, 4),
            "risk_level": get_risk_level(probability)
        })
    return {"total": len(results), "results": results}
