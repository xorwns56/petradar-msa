# PetRadar MSA

MSA 기반 위치기반 반려동물 실종신고 서비스 (Spring Boot + FastAPI + React)

> 원본 모놀리식 레포: [PetRadar](https://github.com/xorwns56/PetRadar)

---

## 아키텍처

```
                              ┌──────────────┐
                              │   Frontend   │
                              │  React:3000  │
                              └──────┬───────┘
                                     │
                              ┌──────▼───────┐
                              │   Gateway    │
                              │ Spring:8080  │
                              │  JWT 검증    │
                              └──┬───┬───┬───┘
                    ┌────────────┘   │   └────────────┐
                    ▼                ▼                 ▼
             ┌────────────┐  ┌────────────┐   ┌─────────────┐
             │   User     │  │  Report    │   │   Search    │
             │  Service   │  │  Service   │   │   Service   │
             │ Spring:8081│  │ Spring:8082│   │ FastAPI:8000│
             └──┬───┬─────┘  └──┬──┬──┬───┘   └──┬──┬──────┘
                │   │           │  │  │           │  │
                │   │    Feign  │  │  │           │  │
                │   │◄──────────┘  │  │           │  │
                │   │              │  │           │  │
  ┌─────────────▼───▼──────────────▼──│───────────▼──│──────────┐
  │         PostgreSQL :5432          │              │          │
  │  ┌──────────┐ ┌──────────┐ ┌─────┴────┐        │          │
  │  │ user_db  │ │report_db │ │search_db │        │          │
  │  │          │ │          │ │(pgvector)│        │          │
  │  └──────────┘ └──────────┘ └──────────┘        │          │
  └────────────────────────────────────────────────│──────────┘
                                                    │
                ┌────────────────────┐              │
                │   MinIO :9000     │◄─────────────┘
                │  이미지 스토리지    │   이미지 다운로드 → CLIP 벡터화
                └────────────────────┘

  ┌──────────────────── Kafka (KRaft) :9092 ────────────────────┐
  │                                                              │
  │  user-deleted:  User → Report  (탈퇴 시 연관 데이터 정리)     │
  │  missing-created: Report → Search (이미지 임베딩 생성)        │
  │  missing-deleted: Report → Search (임베딩 제거)               │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────── Redis :6379 ────────────┐
  │  Refresh Token 저장 (TTL 24h)       │
  │  WebSocket Pub/Sub (스케일 아웃)     │
  └─────────────────────────────────────┘

  ┌──────────── 모니터링 ───────────────┐
  │  Prometheus :9090 → 메트릭 수집     │
  │  Grafana    :3000 → 시각화          │
  │  Loki       :3100 → 로그 수집       │
  │  Promtail          → 로그 전송      │
  │  Zipkin     :9411 → 분산 트레이싱    │
  └─────────────────────────────────────┘
```

---

## 기술 스택

| 영역 | 기술 | 선택 이유 |
|---|---|---|
| **API Gateway** | Spring Cloud Gateway | WebFlux 기반 논블로킹, JWT 필터 체인과 Spring 생태계 통합 |
| **백엔드 (Java)** | Spring Boot 3.4.1 | 서비스 간 일관된 구조, JPA/Security/Actuator 등 성숙한 에코시스템 |
| **백엔드 (Python)** | FastAPI | CLIP 모델 서빙에 적합, 비동기 처리, 자동 API 문서 생성 |
| **프론트엔드** | React + Vite | 빠른 HMR, FSD 아키텍처로 feature 단위 모듈화 |
| **이미지 검색** | CLIP (clip-vit-base-patch32) | 이미지/텍스트 크로스 모달 검색을 단일 모델로 처리 |
| **벡터 DB** | pgvector | PostgreSQL 확장으로 별도 DB 없이 벡터 유사도 검색 |
| **메시지 브로커** | Kafka (KRaft) | Zookeeper 없는 경량 구성, 서비스 간 비동기 이벤트 처리 |
| **캐시/Pub-Sub** | Redis | Refresh Token TTL 관리 + WebSocket 스케일 아웃 브로드캐스트 |
| **파일 스토리지** | MinIO | S3 호환 API, 로컬/k8s 동일 인터페이스 |
| **인증** | JWT | Access Token 10분 + Refresh Token 24시간, Gateway에서 검증 후 X-User-Id 전달 |
| **동기 통신** | FeignClient | 선언적 REST 클라이언트, report → user 서비스 호출 |
| **실시간 통신** | WebSocket (STOMP) | 알림 실시간 전달, Redis Pub/Sub으로 다중 인스턴스 대응 |
| **모니터링** | Prometheus + Grafana | 메트릭 수집/시각화, Spring Actuator/FastAPI Instrumentator 연동 |
| **로그** | Loki + Promtail | ELK 대비 경량, Grafana에서 메트릭과 로그를 함께 조회 |
| **CI/CD** | GitHub Actions + ArgoCD | 이미지 빌드/푸시 자동화, GitOps 기반 k8s 배포 |
| **컨테이너 오케스트레이션** | Kubernetes (Helm) | 서비스별 독립 스케일링, Helm으로 환경별 설정 분리 |

---

## 서비스 구조

| 서비스 | 포트 | 역할 |
|---|---|---|
| `gateway-service` | 8080 | JWT 검증 + 라우팅 (Spring Cloud Gateway) |
| `user-service` | 8081 | 인증(로그인/회원가입) + 사용자 관리 + JWT 발급 |
| `report-service` | 8082 | 실종 신고 + 목격 제보 + 알림 + WebSocket |
| `search-service` | 8000 | CLIP 이미지/텍스트 유사도 검색 (FastAPI + pgvector) |
| `frontend` | 3000 | React SPA (FSD 아키텍처, Zustand + React Query) |

---

## 프로젝트 구조

```
petradar-msa/
├── backend/
│   ├── gateway-service/      # API Gateway (JWT 필터, 라우팅)
│   ├── user-service/         # 인증 + 사용자 관리
│   ├── report-service/       # 실종/제보/알림/WebSocket
│   └── search-service/       # Python CLIP 유사도 검색
├── frontend/                 # React (FSD 구조)
│   └── src/
│       ├── app/              # 라우팅, 프로바이더
│       ├── pages/            # 페이지 컴포넌트
│       ├── widgets/          # 조합 컴포넌트
│       ├── features/         # 기능 단위 모듈
│       ├── entities/         # 도메인 모델/API
│       └── shared/           # 공통 유틸/UI
├── k8s/                      # Helm Chart
│   ├── Chart.yaml
│   ├── values.yaml           # 전체 설정값
│   └── templates/            # 서비스별 k8s 매니페스트
├── docker/                   # Docker 설정 파일
│   ├── postgres/init.sql     # DB 초기화 (스키마 분리)
│   ├── prometheus/           # Prometheus 설정
│   ├── grafana/              # Grafana 대시보드/데이터소스
│   ├── loki/                 # Loki 설정
│   └── promtail/             # Promtail 설정
├── argocd/                   # ArgoCD Application 매니페스트
├── .github/workflows/        # GitHub Actions CI
├── docker-compose.yml        # 로컬 인프라 (PostgreSQL, Redis, Kafka, MinIO, 모니터링)
├── start-dev.sh              # 로컬 전체 일괄 실행
└── stop-dev.sh               # 로컬 전체 일괄 종료
```

---

## 실행 가이드

### 사전 요구사항

- Java 17+
- Python 3.10+
- Node.js 18+
- Docker & Docker Compose

### 일괄 실행 (권장)

```bash
# 인프라 + 백엔드 + 프론트엔드 전체 실행
./start-dev.sh

# 전체 종료
./stop-dev.sh
```

인프라 헬스체크 통과 후 애플리케이션을 실행하며, 하나라도 실패 시 전체 종료됩니다.

### 개별 실행

```bash
# 1. 인프라 실행 (PostgreSQL, Redis, Kafka, MinIO, 모니터링)
docker compose up -d

# 2. 백엔드 서비스 (각각 별도 터미널)
./gradlew bootRun -p backend/gateway-service
./gradlew bootRun -p backend/user-service
./gradlew bootRun -p backend/report-service
cd backend/search-service && uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# 3. 프론트엔드
cd frontend && npm install && npm run dev
```

### 접속 정보

| 서비스 | URL | 계정 |
|---|---|---|
| Frontend | http://localhost:5173 | - |
| Gateway | http://localhost:8080 | - |
| Swagger (User) | http://localhost:8081/swagger-ui.html | - |
| Swagger (Report) | http://localhost:8082/swagger-ui.html | - |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| MinIO 콘솔 | http://localhost:9001 | minioadmin / minioadmin |
| Zipkin | http://localhost:9411 | - |

---

## Kubernetes 배포

```bash
# 네임스페이스 생성 + Helm 설치
helm install petradar ./k8s -n petradar --create-namespace \
  --set jwtSecret="시크릿값" \
  --set minio.accessKey="minioadmin" \
  --set minio.secretKey="minioadmin"

# 배포 상태 확인
kubectl get pods -n petradar

# 업그레이드
helm upgrade petradar ./k8s -n petradar

# 삭제
helm uninstall petradar -n petradar
```

---

## CI/CD 파이프라인

```
코드 Push (main) → GitHub Actions → 테스트 → Docker 빌드 → GHCR 푸시 → ArgoCD 감지 → k8s 배포
```

- **GitHub Actions**: 서비스별 병렬 빌드 (matrix strategy), 테스트 실패 시 빌드 차단
- **ArgoCD**: GHCR 이미지 변경 감지 → 자동 k8s 롤링 업데이트
- **멀티 아키텍처**: `linux/amd64` + `linux/arm64` 동시 빌드

---

## 테스트

```bash
# Java 서비스 (gateway, user, report)
cd backend/{service-name}
./gradlew test

# Python 서비스 (search)
cd backend/search-service
python -m pytest tests/ -v
```

전체 122개 테스트 (서비스 레이어 단위 테스트 + API 통합 테스트)

---

## 서비스 간 통신

### REST (동기) - FeignClient

```
report-service → user-service
  GET /api/user/{id}    # 알림 변환 시 loginId 조회
  GET /api/user/all     # 실종 신고 전체 알림 발송
```

### Kafka (비동기)

| 토픽 | Producer | Consumer | 용도 |
|---|---|---|---|
| `user-deleted` | user-service | report-service | 회원 탈퇴 시 관련 데이터 정리 |
| `missing-created` | report-service | search-service | 실종 신고 → 이미지 임베딩 생성 |
| `missing-deleted` | report-service | search-service | 실종 신고 삭제 → 임베딩 제거 |

### WebSocket (실시간)

- STOMP 프로토콜, 알림 경로: `/user/{userId}/queue/notification`
- Redis Pub/Sub으로 다중 인스턴스 간 브로드캐스트

---

## 모니터링

| 도구 | 역할 | 접속 |
|---|---|---|
| **Prometheus** | Spring Actuator / FastAPI 메트릭 수집 (15초 주기) | http://localhost:9090 |
| **Grafana** | 대시보드 시각화 (서비스 상태, HTTP 요청/에러율, JVM 힙, 로그) | http://localhost:3000 |
| **Loki** | 컨테이너 로그 수집/검색 (7일 보존) | Grafana에서 조회 |
| **Promtail** | Docker/k8s 로그 → Loki 전송 | - |
| **Zipkin** | 분산 트레이싱 | http://localhost:9411 |

Grafana 대시보드는 시작 시 자동 프로비저닝됩니다 (데이터소스 + 대시보드 JSON).
