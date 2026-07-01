"""
FastAPI server for the web-attack detection ensemble model.

Run weight_optimizer.py first to create AI/model_bundle_ultimate.pkl.
"""

from pathlib import Path
from urllib.parse import unquote

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import joblib
import pandas as pd
from pydantic import BaseModel


BASE_DIR = Path(__file__).resolve().parent
# 1. 최종 글로벌 최적화 번들 파일명 적용 완료!
BUNDLE_PATH = BASE_DIR / "model_bundle_ultimate.pkl"

ATTACK_KEYWORDS = [
    "select",
    "insert",
    "update",
    "delete",
    "drop",
    "union",
    "exec",
    "script",
    "alert",
    "../",
]
SQL_KEYWORDS = [
    "select",
    "insert",
    "update",
    "delete",
    "drop",
    "union",
    "where",
    "from",
    "exec",
    "sleep",
    "benchmark",
]
XSS_KEYWORDS = [
    "<script",
    "script",
    "alert",
    "onerror",
    "onload",
    "javascript:",
    "<img",
    "<svg",
]
PATH_TRAVERSAL_PATTERNS = ["../", "..\\", "%2e%2e", "etc/passwd", "boot.ini"]
SPECIAL_CHARS = ["'", '"', "<", ">", "--", ";", "%", "(", ")", "="]

app = FastAPI(
    title="Security Threat Detection API",
    description="Detects anomalous HTTP requests with an ensemble ML model.",
    version="3.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

model = None
encoders = {}
features = []
threshold = 0.5
model_type = "unknown"
# 2. 최적 가중치를 동적으로 할당받을 전역 변수
optimized_weights = None


class HttpRequest(BaseModel):
    method: str
    url_path: str
    query_params: str = ""
    body_content: str = ""
    user_agent: str = ""
    ip_address: str = ""    # 요청 IP
    timestamp: str = ""     # 요청 시각


class ThreatResponse(BaseModel):
    threat_score: float   # 공격일 확률 그대로 (0.0 ~ 1.0)
    ip_address:   str     # 공격이면 실제 IP, 정상이면 "0.0.0.0"
    reason:       str     # 왜 위협인지 (정상이면 "-")


def count_matches(text: str, patterns: list[str]) -> int:
    text_lower = (text or "").lower()
    return sum(text_lower.count(pattern.lower()) for pattern in patterns)


def has_any(text: str, patterns: list[str]) -> int:
    return int(count_matches(text, patterns) > 0)


def safe_ratio(part: int, total: int) -> float:
    return part / total if total else 0.0


def safe_encode(column: str, value: str) -> int:
    encoder = encoders[column]
    value = value if value in encoder.classes_ else "UNKNOWN"
    return int(encoder.transform([value])[0])


def build_feature_row(req: HttpRequest) -> dict[str, object]:
    method = (req.method or "UNKNOWN").upper()
    url_path = req.url_path or ""
    query_params = req.query_params or ""
    body_content = req.body_content or ""
    user_agent = req.user_agent or ""

    full_url = url_path + (f"?{query_params}" if query_params else "")
    decoded_query = unquote(query_params)
    decoded_body = unquote(body_content)
    decoded_url = unquote(full_url)
    combined = f"{decoded_url} {decoded_body}"

    url_len = len(full_url)
    query_len = len(query_params)
    body_len = len(body_content)
    total_len = url_len + body_len
    special_char_count = sum(full_url.count(c) for c in SPECIAL_CHARS) + sum(
        body_content.count(c) for c in SPECIAL_CHARS
    )
    encoded_char_count = full_url.count("%") + body_content.count("%")
    digit_count = sum(ch.isdigit() for ch in full_url + body_content)
    alpha_count = sum(ch.isalpha() for ch in full_url + body_content)
    param_count = query_params.count("=") + body_content.count("=")
    path_depth = len([part for part in url_path.split("/") if part])
    file_extension = Path(url_path).suffix.lower().lstrip(".") or "NONE"

    return {
        "method": method,
        "user_agent": user_agent,
        "url_path": url_path,
        "file_extension": file_extension,
        "url_len": url_len,
        "query_len": query_len,
        "body_len": body_len,
        "total_len": total_len,
        "path_depth": path_depth,
        "param_count": param_count,
        "special_char_count": special_char_count,
        "special_char_ratio": safe_ratio(special_char_count, total_len),
        "encoded_char_count": encoded_char_count,
        "digit_ratio": safe_ratio(digit_count, total_len),
        "alpha_ratio": safe_ratio(alpha_count, total_len),
        "has_keywords_query": has_any(decoded_query, ATTACK_KEYWORDS),
        "has_keywords_body": has_any(decoded_body, ATTACK_KEYWORDS),
        "sql_keyword_count": count_matches(combined, SQL_KEYWORDS),
        "xss_keyword_count": count_matches(combined, XSS_KEYWORDS),
        "path_traversal_count": count_matches(combined, PATH_TRAVERSAL_PATTERNS),
        "has_script_tag": int("<script" in combined.lower()),
        "has_union_select": int("union" in combined.lower() and "select" in combined.lower()),
        "has_comment_pattern": int("--" in combined or "/*" in combined or "*/" in combined),
    }


def extract_features(req: HttpRequest) -> pd.DataFrame:
    row = build_feature_row(req)
    if "method" in encoders:
        row["method_encoded"] = safe_encode("method", row["method"])
    if "user_agent" in encoders:
        row["user_agent_encoded"] = safe_encode("user_agent", row["user_agent"])
    if "url_path" in encoders:
        row["url_path_encoded"] = safe_encode("url_path", row["url_path"])
    if "file_extension" in encoders:
        row["file_extension_encoded"] = safe_encode("file_extension", row["file_extension"])
    return pd.DataFrame([{feature: row[feature] for feature in features}], columns=features)


def get_risk_level(probability: float) -> str:
    if probability >= 0.7:
        return "HIGH"
    if probability >= 0.4:
        return "MEDIUM"
    return "LOW"


def get_attack_reason(probability: float, row: dict) -> str:
    if probability < threshold:
        return "-"

    reasons = []

    if row["has_keywords_query"]:
        reasons.append("URL 공격 키워드")
    if row["has_keywords_body"]:
        reasons.append("Body 공격 키워드")
    if row["sql_keyword_count"] > 0:
        reasons.append(f"SQL 패턴({row['sql_keyword_count']}개)")
    if row["xss_keyword_count"] > 0:
        reasons.append(f"XSS 패턴({row['xss_keyword_count']}개)")
    if row["path_traversal_count"] > 0:
        reasons.append("디렉토리 탈출 패턴")
    if row["has_union_select"]:
        reasons.append("UNION SELECT 패턴")
    if row["has_script_tag"]:
        reasons.append("<script> 태그")
    if row["has_comment_pattern"]:
        reasons.append("SQL 주석 패턴")
    if row["special_char_count"] > 5:
        reasons.append(f"특수문자 과다({row['special_char_count']}개)")

    if not reasons:
        reasons.append("복합적인 패턴 이상 (Path/Method/UA)")

    return ", ".join(reasons)


@app.on_event("startup")
def load_model() -> None:
    global model, encoders, features, threshold, model_type, optimized_weights

    if not BUNDLE_PATH.exists():
        print(f"Model bundle not found: {BUNDLE_PATH}")
        return

    bundle = joblib.load(BUNDLE_PATH)
    
    # 3. Ultimate 번들 구조(모델 딕셔너리 + 가중치) 파싱
    if "optimized_weights" in bundle:
        model = bundle["models"]  
        optimized_weights = bundle["optimized_weights"] 
    else:
        model = bundle.get("model")
        optimized_weights = None

    encoders = bundle.get("encoders")
    if encoders is None:
        encoders = {
            "method": bundle["le_method"],
            "user_agent": bundle["le_ua"],
            "url_path": bundle["le_path"],
        }
    features = bundle["features"]
    threshold = float(bundle.get("threshold", 0.5))
    model_type = bundle.get("model_type", "unknown")
    print(f"Loaded Ultimate model bundle: {BUNDLE_PATH}")
    print(f"Applied Weights: {optimized_weights}")


@app.get("/")
def root():
    return {"status": "running", "message": "Security threat detection API is running."}


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "model_loaded": model is not None,
        "model_type": model_type,
        "threshold": threshold,
        "feature_count": len(features),
    }


@app.post("/predict", response_model=ThreatResponse)
def predict(request: HttpRequest):
    if model is None:
        raise HTTPException(status_code=503, detail="Model is not loaded.")

    feature_frame = extract_features(request)
    
    # 4. 수제 Soft Voting 확률 결합 알고리즘 구현
    if optimized_weights:
        probability = sum(
            weight * m.predict_proba(feature_frame)[0][1]
            for name, m in model.items()
            for weight in [optimized_weights[name]]
        )
    else:
        probability = float(model.predict_proba(feature_frame)[0][1])
        
    is_attack = probability >= threshold
    row = build_feature_row(request)

    return ThreatResponse(
        threat_score = round(probability, 4),
        ip_address   = request.ip_address if is_attack else "0.0.0.0",
        reason       = get_attack_reason(probability, row),
    )


@app.post("/predict/batch")
def predict_batch(requests: list[HttpRequest]):
    if model is None:
        raise HTTPException(status_code=503, detail="Model is not loaded.")

    results = []
    for req in requests:
        feature_frame = extract_features(req)
        
        if optimized_weights:
            probability = sum(
                weight * m.predict_proba(feature_frame)[0][1]
                for name, m in model.items()
                for weight in [optimized_weights[name]]
            )
        else:
            probability = float(model.predict_proba(feature_frame)[0][1])
            
        is_attack = probability >= threshold
        row = build_feature_row(req)
        results.append(
            ThreatResponse(
                threat_score = round(probability, 4),
                ip_address   = req.ip_address if is_attack else "0.0.0.0",
                reason       = get_attack_reason(probability, row),
            ).dict()
        )

    return {"total": len(results), "results": results}