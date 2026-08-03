# WAF (Web Application Firewall)

OWASP ModSecurity CRS + Nginx 기반 WAF 설정입니다.

## 구조

```text
waf/
  docker-compose.yml              # WAF 컨테이너 및 CRS 환경변수 설정
  rules/
    CUSTOM-001-ai-align.conf      # AI 모델 특성과 맞춘 커스텀 룰
    CUSTOM-002-blacklist.conf     # IP 블랙리스트 룰
  logs/                           # Nginx access/error 로그
  fluent-bit.conf                 # access.log/error.log -> Kafka 전송 설정
  parsers.conf                    # Nginx access.log/error.log 파서
```

## 실행 방법

```bash
docker compose -f ../backend/docker-compose.yml up -d zookeeper kafka db
docker compose -f ./waf/docker-compose.yml -f ../targets/juiceshop/docker-compose.yml up -d
```

## 트래픽 흐름

기본 백엔드 연동:

```text
Client -> WAF (port 80) -> Spring Boot (port 8080)
```

Juice Shop 테스트 타깃 사용 시:

```text
Client -> WAF (port 80) -> Juice Shop (port 3000)
```

## 로그 파이프라인

WAF는 `/var/log/nginx/access.log`에 요청 로그를 남기고, `/var/log/nginx/error.log`에 Nginx 오류와 ModSecurity 탐지 상세 로그를 남깁니다. 두 경로는 호스트의 `waf/logs/` 폴더와 연결되어 있고, Fluent Bit가 각 파일을 읽어서 Kafka로 전송합니다.

```text
WAF/Nginx access.log -> Fluent Bit -> Kafka topic(log-topic)
WAF/Nginx error.log  -> Fluent Bit -> Kafka topic(waf-error-topic)
```

확인 명령:

```bash
curl "http://localhost/rest/products/search?q=test"
docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic log-topic --from-beginning --timeout-ms 8000 --max-messages 5
docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic waf-error-topic --from-beginning --timeout-ms 8000 --max-messages 5
```

## 현재 설정

CRS 기본 룰셋은 `owasp/modsecurity-crs:nginx` Docker 이미지에 포함되어 있습니다. 프로젝트에서는 `docker-compose.yml`의 환경변수로 CRS 민감도와 차단 임계값을 설정합니다.

```yaml
MODSEC_RULE_ENGINE: DetectionOnly
PARANOIA: 2
BLOCKING_PARANOIA: 1
ANOMALY_INBOUND: 5
ANOMALY_OUTBOUND: 4
```

- `MODSEC_RULE_ENGINE=DetectionOnly`: ModSecurity 탐지 및 차단 활성화
- `PARANOIA=2`: CRS 탐지 범위를 PL2까지 확장
- `BLOCKING_PARANOIA=1`: 실제 차단 판단은 PL1 기준으로 적용
- `ANOMALY_INBOUND=5`: 요청 이상 점수가 5 이상이면 차단
- `ANOMALY_OUTBOUND=4`: 응답 이상 점수가 3 이상이면 차단

## 커스텀 룰

### CUSTOM-001-ai-align.conf

AI 모델이 사용하는 HTTP 특성과 맞춰 WAF 1차 차단 기준을 추가합니다.

| Rule ID | 대상 | 기준 |
| --- | --- | --- |
| 10001 | HTTP Method | 허용되지 않은 Method 차단 |
| 10002 | URL 길이 | 500자 초과 차단 |
| 10003 | 요청 Body 크기 | 10KB 초과 차단 |
| 10004 | Juice Shop 호환성 | 허용 Method 확장 |
| 10005 | Socket.IO 호환성 | polling 요청의 CRS 오탐 완화 |

SQL Injection, XSS, Path Traversal 같은 일반 웹 공격 패턴은 기본 CRS 룰셋이 담당합니다.

### CUSTOM-002-blacklist.conf

차단할 IP를 수동 또는 백엔드 연동으로 추가할 수 있는 블랙리스트 룰 파일입니다.

```apache
SecRule REMOTE_ADDR "@ipMatch 1.2.3.4" \
    "id:20001,phase:1,deny,status:403,log,msg:'Blacklisted IP'"
```

## 미구현 또는 TODO

- [ ] 백엔드 블랙리스트 자동 동기화 (`BlacklistService` -> `CUSTOM-002`)
- [ ] AI 모델 분석 결과와 WAF 정책 자동 연계
