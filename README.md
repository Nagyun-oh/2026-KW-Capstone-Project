# 2026-KW-Capstone-Project

## AI 기반 실시간 보안 아키텍처 설계 및 구현

## 1. 프로젝트 소개

본 프로젝트는 실시간으로 발생하는 로그 데이터를 수집하고, AI 기반 분석을 통해 이상 징후 및 보안 이벤트를 탐지하여 대시보드에서 시각화하는 보안 모니터링 시스템이다.

## 2. 주요 기능

- 로그 수집 및 저장
- Kafka 기반 비동기화 구조 설계
- AI 기반 이상 탐지
- 보안 이벤트 분류
- 대시보드 시각화
- Docker 기반 실행 환경
- 클라우드 배포 가능 구조

## 3. 시스템 아키텍처

![alt text](docs\images\02_image.png)

## 4. 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Spring Boot / FastAPI |
| Database | MySQL |
| AI | Python |
| Infra | Docker, Docker Compose |
| Frontend | React |

## 5. 담당 역할

- 오나균(팀장)
    - 프론트엔드 개발
    - 백엔드 API 설계 및 구현
    - 데이터베이스 설계
    - 인프라 및 배포 환경 구성
    - 전체 시스템 아키텍처 설계
- 김진섭
    - 보안 아키텍쳐 설계
    - 보안관련 오픈 데이터셋 탐색 및 정제
    - ai 학습용 데이터셋 작성
    - 테스트 시나리오 작성
- 남현욱
    - AI 설계 및 구현

## 6. 실행 방법

```bash
docker-compose up -d