# WAF (Web Application Firewall)

OWASP ModSecurity CRS + Nginx 기반 WAF 초기 설정

## 구조

```
waf/
  docker-compose.yml              # WAF 컨테이너 설정
  config/
    crs-setup.conf                # CRS 파라노이아 레벨, 임계값 설정
  rules/
    CUSTOM-001-ai-align.conf      # AI 모델 기반 커스텀 룰
    CUSTOM-002-blacklist.conf     # IP 블랙리스트
  logs/                           # Nginx access/error 로그
  fluent-bit.conf                 # access.log -> Kafka 전송 설정
  parsers.conf                    # Nginx access.log 파서
```

## 실행 방법

```bash
docker compose -f ../backend/docker-compose.yml up -d zookeeper kafka db
docker compose -f ./waf/docker-compose.yml -f ../targets/juiceshop/docker-compose.yml up -d
```

## 트래픽 흐름

```
클라이언트 → WAF (포트 80) → Spring Boot (포트 8080)
```

Juice Shop 테스트 대상 사용 시:

```
클라이언트 → WAF (포트 80) → Juice Shop (포트 3000)
```

## 로그 파이프라인

WAF는 `/var/log/nginx/access.log`에 요청 로그를 남긴다. 이 경로는 호스트의 `waf/logs/access.log`와 연결되어 있고, Fluent Bit가 같은 파일을 읽어서 Kafka로 전송한다.

```
WAF/Nginx access.log → Fluent Bit → Kafka topic(raw-waf-logs)
```

확인 명령:

```bash
curl "http://localhost/rest/products/search?q=test"
docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic log-topic --from-beginning --timeout-ms 8000 --max-messages 5
```

## 현재 설정

### CRS 설정 (crs-setup.conf)
- 파라노이아 레벨: 2 (SQLi, XSS 강화 탐지)
- 인바운드 임계값: 10
- 아웃바운드 임계값: 5

### 커스텀 룰 (CUSTOM-001-ai-align.conf)
AI 모델(LightGBM)이 사용하는 특성과 동일한 기준으로 1차 차단:

| 룰 ID | 대상 | 기준 |
|-------|------|------|
| 10001 | HTTP 메서드 | GET, POST, HEAD, OPTIONS 외 차단 |
| 10002 | URL 길이 | 500자 초과 차단 |
| 10003 | 요청 바디 크기 | 10KB 초과 차단 |

> SQLi, XSS, Path Traversal은 CRS PARANOIA=2가 처리하므로 중복 제외

### IP 블랙리스트 (CUSTOM-002-blacklist.conf)
차단할 IP 추가 방법:
```apache
SecRule REMOTE_ADDR "@ipMatch 1.2.3.4" \
    "id:20001,phase:1,deny,status:403,log,msg:'Blacklisted IP'"
```
id는 20001부터 순서대로 증가

## 미구현 (추후 연동 예정)

- [x] Fluent Bit 연동 (logs/ → Kafka)
- [ ] 백엔드 블랙리스트 자동 동기화 (BlacklistService → CUSTOM-002)
- [ ] AI 모델 연동
