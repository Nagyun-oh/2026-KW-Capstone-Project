# 백엔드 설계 문서

## 백엔드 역할
- 로그 수집 API
- 로그 조회 API
- 보안 이벤트 조회 API
- AI 분석 결과 저장/조회
- 대시보드 데이터 제공

## API 설계 예시

| Method | URL | 설명 |
|---|---|---|
| POST | /api/logs | 로그 저장 |
| GET | /api/logs | 로그 목록 조회 |
| GET | /api/logs/{id} | 특정 로그 조회 |
| GET | /api/events | 보안 이벤트 조회 |
| GET | /api/dashboard/summary | 대시보드 요약 정보 조회 |

## 예시 요청

POST /api/logs

{ "message": "Failed login attempt",
"source": "auth-server",
"level": "WARN",
"createdAt": "2026-05-04T10:00:00"
}

## 예시 응답

{
  "id": 1,
  "status": "saved"
}

## 예외 처리
- 잘못된 로그 형식 입력 시 400 Bad Request 반환
- DB 저장 실패 시 500 Internal Server Error 반환

## 설계 의도
- 로그 수집과 조회 기능을 분리하여 유지보수성을 높였다.
- 대시보드에서 필요한 데이터를 별도 API로 제공하여 프론트엔드와의 결합도를 낮췄다.