"""
모델 학습 및 저장 스크립트 (최종본)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
데이터셋 : CSIC 2010 (cisc_*.txt 3개)
피처     : 노트북(scis2010.ipynb)과 동일한 7개

실행 후 반드시 할 것:
  출력되는 URL_LEN_MEAN, URL_LEN_STD 값을
  main.py 상단 상수에 복붙하세요.

저장 파일:
  model_bundle.pkl  ← model + le_method + le_ua + le_path + url_len 통계
"""

import pandas as pd
import re
import joblib
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, recall_score, f1_score, roc_auc_score
import lightgbm as lgb

# ─────────────────────────────────────────────────────────
# 공격 키워드 (노트북 Cell 12와 동일)
# ─────────────────────────────────────────────────────────
ATTACK_KEYWORDS = ['select', 'insert', 'drop', 'script',
                   'alert', 'union', 'exec', '../']

def has_attack_keyword(text: str) -> int:
    if not text or text == "null":
        return 0
    return 1 if any(kw in str(text).lower() for kw in ATTACK_KEYWORDS) else 0

# ─────────────────────────────────────────────────────────
# CSIC 2010 파일 파싱 (노트북 parse_csic_file과 동일)
# ─────────────────────────────────────────────────────────
def parse_csic_file(file_path: str) -> pd.DataFrame:
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    requests = re.findall(r"Start - Id:.*?\n(.*?)\nEnd - Id:", content, re.DOTALL)
    data_list = []

    for req in requests:
        # 레이블
        cls_match = re.search(r"class: (Valid|Attack)", req)
        label = 1 if cls_match and cls_match.group(1) == "Attack" else 0

        # HTTP 메서드 & URL
        first_line = req.split("\n")[1] if "class:" in req else req.split("\n")[0]
        method_match = re.search(r"(GET|POST|PUT|DELETE)", first_line)
        method = method_match.group(1) if method_match else "UNKNOWN"

        full_url_match = re.search(r"http://[^\s]+", first_line)
        full_url = full_url_match.group(0) if full_url_match else ""

        url_path = (full_url.split("?")[0]
                    .replace("http://localhost:8080", "")
                    .replace("http://localhost:9090", ""))
        query_params = full_url.split("?")[1] if "?" in full_url else ""

        # User-Agent
        ua_match = re.search(r"User-Agent: (.*)", req)
        user_agent = ua_match.group(1).strip() if ua_match else ""

        # Body
        body_content = ""
        if "\n\n" in req:
            body_content = req.split("\n\n")[1].strip()
            if body_content == "null":
                body_content = ""

        # 수치형 피처
        special_chars = ["'", '"', '<', '>', '--', ';', '%']
        special_char_count = (
            sum(full_url.count(c)        for c in special_chars) +
            sum(body_content.count(c)    for c in special_chars)
        )

        data_list.append({
            "method":             method,
            "user_agent":         user_agent,
            "url_path":           url_path,
            "url_len":            len(full_url),
            "special_char_count": special_char_count,
            "has_keywords_query": has_attack_keyword(query_params),
            "has_keywords_body":  has_attack_keyword(body_content),
            "class":              label,
        })

    return pd.DataFrame(data_list)

# ─────────────────────────────────────────────────────────
# 데이터 로드
# ─────────────────────────────────────────────────────────
print("📂 데이터 로딩 중...")
df = pd.concat([
    parse_csic_file("cisc_normalTraffic_train.txt"),
    parse_csic_file("cisc_normalTraffic_test.txt"),
    parse_csic_file("cisc_anomalousTraffic_test.txt"),
], ignore_index=True)
print(f"   총 샘플 수  : {len(df):,}")
print(f"   공격(1)    : {df['class'].sum():,}")
print(f"   정상(0)    : {(df['class'] == 0).sum():,}")

# ─────────────────────────────────────────────────────────
# url_len 통계 출력 → main.py 상수에 복붙!
# ─────────────────────────────────────────────────────────
url_len_mean = df["url_len"].mean()
url_len_std  = df["url_len"].std()
print(f"\n📏 main.py 상단에 복붙하세요:")
print(f"   URL_LEN_MEAN = {url_len_mean:.1f}")
print(f"   URL_LEN_STD  = {url_len_std:.1f}")

# ─────────────────────────────────────────────────────────
# LabelEncoder 학습
# 'UNKNOWN' 클래스를 미리 추가해서 추론 시 미등록 값 처리 가능
# ─────────────────────────────────────────────────────────
print("\n🔤 LabelEncoder 학습 중...")
le_method = LabelEncoder()
le_ua     = LabelEncoder()
le_path   = LabelEncoder()

le_method.fit(list(df["method"].unique())     + ["UNKNOWN"])
le_ua.fit(list(df["user_agent"].unique())     + ["UNKNOWN"])
le_path.fit(list(df["url_path"].unique())     + ["UNKNOWN"])

df["method_encoded"]     = le_method.transform(df["method"])
df["user_agent_encoded"] = le_ua.transform(df["user_agent"])
df["url_path_encoded"]   = le_path.transform(df["url_path"])

# ─────────────────────────────────────────────────────────
# 학습 / 검증 분리 (노트북과 동일: 80:20 stratified)
# ─────────────────────────────────────────────────────────
FEATURES = [
    "method_encoded",       # 0
    "user_agent_encoded",   # 1
    "url_path_encoded",     # 2
    "url_len",              # 3
    "special_char_count",   # 4
    "has_keywords_query",   # 5
    "has_keywords_body",    # 6
]

X = df[FEATURES]
y = df["class"]

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)
print(f"\n📊 학습 데이터: {len(X_train):,}개  /  검증 데이터: {len(X_test):,}개")

# ─────────────────────────────────────────────────────────
# LightGBM 학습
# ─────────────────────────────────────────────────────────
print("\n🤖 LightGBM 학습 중...")
model = lgb.LGBMClassifier(
    n_estimators=100,
    learning_rate=0.1,
    max_depth=-1,
    random_state=42,
    importance_type="gain",
    verbose=-1,
)
model.fit(X_train, y_train)

# ─────────────────────────────────────────────────────────
# 평가
# ─────────────────────────────────────────────────────────
y_pred = model.predict(X_test)
y_prob = model.predict_proba(X_test)[:, 1]

print("\n📈 평가 결과:")
print(f"   Accuracy  : {accuracy_score(y_test, y_pred):.4f}")
print(f"   Recall    : {recall_score(y_test, y_pred):.4f}")
print(f"   F1-Score  : {f1_score(y_test, y_pred):.4f}")
print(f"   AUC-ROC   : {roc_auc_score(y_test, y_prob):.4f}")

# ─────────────────────────────────────────────────────────
# 번들 저장
# main.py는 이 파일 하나만 로드하면 모든 게 들어있음
# ─────────────────────────────────────────────────────────
bundle = {
    "model":        model,
    "le_method":    le_method,
    "le_ua":        le_ua,
    "le_path":      le_path,
    "features":     FEATURES,
    "url_len_mean": url_len_mean,
    "url_len_std":  url_len_std,
}
joblib.dump(bundle, "model_bundle.pkl")

print("\n✅ 저장 완료: model_bundle.pkl")
print("   포함 항목: model / le_method / le_ua / le_path / features / url_len 통계")
print("\n⚠️  위의 URL_LEN_MEAN, URL_LEN_STD 값을 main.py 상단에 복붙하세요!")
