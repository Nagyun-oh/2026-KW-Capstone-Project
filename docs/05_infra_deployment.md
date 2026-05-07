# 인프라 및 배포 문서

## 로컬 개발 환경
- Backend Server
- MySQL
- AI Module
- Dashboard

## 실행 방법

docker-compose up -d

## Docker 구성

### backend
- 백엔드 서버 실행
- API 제공

### mysql
- 로그 및 이벤트 데이터 저장

### ai-module
- 로그 분석 및 이상 탐지 수행

### dashboard
- 사용자 화면 제공


## 클라우드 배포 구조

사용자

  ↓

Load Balancer

  ↓

Backend Server

  ↓

Kafka

  ↓

AI Analysis Module

  ↓

DataBase

  ↓

Dashboard

## 설계 의도
- Docker를 사용하여 개발 환경과 실행 환경의 차이를 줄였다.
- 서비스별 컨테이너를 분리하여 유지보수성을 높였다.
- 추후 클라우드 환경으로 확장 가능하도록 구조를 설계하였다.