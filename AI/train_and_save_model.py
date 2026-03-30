"""
모델 학습 및 저장 스크립트
노트북에서 학습한 LightGBM 모델을 .pkl 파일로 저장합니다.
FastAPI 서버가 이 파일을 로드해서 예측에 사용합니다.
"""

import pandas as pd
import re
import joblib
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import lightgbm as lgb

# ──────────────────────────────────────────────
# 노트북의 parse_csic_file 함수 (동일하게 재사용)
# ──────────────────────────────────────────────
def parse_csic_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    requests = re.findall(r'Start - Id:.*?\n(.*?)\nEnd - Id:', content, re.DOTALL)
    data_list = []
    for req in requests:
        cls_match = re.search(r'class: (Valid|Attack)', req)
        label = 1 if cls_match and cls_match.group(1) == 'Attack' else 0

        first_line = req.split('\n')[1] if 'class:' in req else req.split('\n')[0]
        method_match = re.search(r'(GET|POST|PUT|DELETE)', first_line)
        method = method_match.group(1) if method_match else "UNKNOWN"

        full_url_match = re.search(r'http://[^\s]+', first_line)
        full_url = full_url_match.group(0) if full_url_match else ""

        url_path = full_url.split('?')[0].replace('http://localhost:8080', '').replace('http://localhost:9090', '')
        query_params = full_url.split('?')[1] if '?' in full_url else ""

        body_content = ""
        if '\n\n' in req:
            body_content = req.split('\n\n')[1].strip()
            if body_content == "null": body_content = ""

        url_len = len(full_url)
        special_chars = ["'", '"', '<', '>', '--', ';', '%']
        special_char_count = sum(full_url.count(c) for c in special_chars) + sum(body_content.count(c) for c in special_chars)
        method_num = 1 if method == "POST" else 0
        body_len = len(body_content)

        data_list.append({
            'method_num': method_num,
            'url_len': url_len,
            'special_char_count': special_char_count,
            'body_len': body_len,
            'class': label
        })
    return pd.DataFrame(data_list)

# ──────────────────────────────────────────────
# 데이터 로드 및 모델 학습
# ──────────────────────────────────────────────
print("📂 데이터 로딩 중...")
df_train_normal   = parse_csic_file('cisc_normalTraffic_train.txt')
df_test_normal    = parse_csic_file('cisc_normalTraffic_test.txt')
df_test_anomalous = parse_csic_file('cisc_anomalousTraffic_test.txt')
df = pd.concat([df_train_normal, df_test_normal, df_test_anomalous], ignore_index=True)

FEATURES = ['method_num', 'url_len', 'special_char_count', 'body_len']
X = df[FEATURES]
y = df['class']

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

print("🤖 LightGBM 모델 학습 중...")
model = lgb.LGBMClassifier(n_estimators=300, learning_rate=0.05, random_state=42)
model.fit(X_train, y_train)

# ──────────────────────────────────────────────
# 모델 저장 (FastAPI 서버가 이 파일을 로드)
# ──────────────────────────────────────────────
joblib.dump(model, "lightgbm_model.pkl")
print("✅ 모델 저장 완료: lightgbm_model.pkl")

from sklearn.metrics import accuracy_score, f1_score
y_pred = model.predict(X_test)
print(f"   Accuracy : {accuracy_score(y_test, y_pred):.4f}")
print(f"   F1-Score : {f1_score(y_test, y_pred):.4f}")
