# PetRadar MSA

MSA 기반 위치기반 반려동물 실종신고 서비스 (Spring Boot + React)

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
             │ Spring:8081│  │ Spring:8082│   │ Spring:8083 │
             └──┬───┬─────┘  └──┬──┬──┬───┘   └──────┬──────┘
                │   │           │  │  │              │
                │   │    Feign  │  │  │              │
                │   │◄──────────┘  │  │              │
                │   │              │  │              │
  ┌─────────────▼───▼──────────────▼──│──┐    ┌──────▼────────────┐
  │         PostgreSQL :5432           │  │    │ Elasticsearch:9200│
  │  ┌──────────┐ ┌──────────┐        │  │    │  missing 인덱스    │
  │  │ user_db  │ │report_db │        │  │    │  (전문 검색)       │
  │  └──────────┘ └──────────┘        │  │    └───────────────────┘
  └────────────────────────────────────┘  │
                                          │
                ┌────────────────────┐    │
                │   MinIO :9000      │◄───┘
                │  이미지 스토리지    │
                └────────────────────┘

  ┌──────────────────── Kafka (KRaft) :9092 ────────────────────┐
  │                                                              │
  │  user-deleted:    User → Report  (탈퇴 시 연관 데이터 정리)   │
  │  missing-created: Report → Search (Elasticsearch 인덱싱)      │
  │                   Report → Report (전체 유저 알림 발송)       │
  │  missing-deleted: Report → Search (인덱스 제거)               │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────── Redis :6379 ────────────┐
  │  Refresh Token 저장 (TTL 24h)       │
  │  WebSocket Pub/Sub (스케일 아웃)     │
  └─────────────────────────────────────┘
```

---

## 기술 스택

| 영역 | 기술 | 선택 이유 |
|---|---|---|
| **API Gateway** | Spring Cloud Gateway | WebFlux 기반 논블로킹, JWT 필터 체인과 Spring 생태계 통합 |
| **백엔드** | Spring Boot 3.4.1 | 서비스 간 일관된 구조, JPA/Security/Actuator 등 성숙한 에코시스템 |
| **프론트엔드** | React + Vite | 빠른 HMR, FSD 아키텍처로 feature 단위 모듈화 |
| **검색 엔진** | Elasticsearch 8.15 | 역색인 기반 전문 검색, 필드별 가중치와 오타 보정(fuzziness) 지원 |
| **메시지 브로커** | Kafka (KRaft) | Zookeeper 없는 경량 구성, 서비스 간 비동기 이벤트 처리 |
| **캐시/Pub-Sub** | Redis | Refresh Token TTL 관리 + WebSocket 스케일 아웃 브로드캐스트 |
| **파일 스토리지** | MinIO | S3 호환 API, 로컬/k8s 동일 인터페이스 |
| **인증** | JWT | Access Token 10분 + Refresh Token 24시간, Gateway에서 검증 후 X-User-Id 전달 |
| **동기 통신** | FeignClient | 선언적 REST 클라이언트, report → user 서비스 호출 |
| **실시간 통신** | WebSocket (STOMP) | 알림 실시간 전달, Redis Pub/Sub으로 다중 인스턴스 대응 |
| **CI/CD** | GitHub Actions + ArgoCD | 이미지 빌드/푸시 자동화, GitOps 기반 k8s 배포 |
| **컨테이너 오케스트레이션** | Kubernetes (Helm) | 서비스별 독립 스케일링, Helm으로 환경별 설정 분리 |

---

## 서비스 구조

| 서비스 | 포트 | 역할 |
|---|---|---|
| `gateway-service` | 8080 | JWT 검증 + 라우팅 (Spring Cloud Gateway) |
| `user-service` | 8081 | 인증(로그인/회원가입) + 사용자 관리 + JWT 발급 |
| `report-service` | 8082 | 실종 신고 + 목격 제보 + 알림 + WebSocket |
| `search-service` | 8083 | Elasticsearch 기반 실종동물 전문 검색 |
| `frontend` | 3000 | React SPA (FSD 아키텍처, Zustand + React Query) |

---

## 프로젝트 구조

```
petradar-msa/
├── backend/
│   ├── gateway-service/      # API Gateway (JWT 필터, 라우팅)
│   ├── user-service/         # 인증 + 사용자 관리
│   ├── report-service/       # 실종/제보/알림/WebSocket
│   └── search-service/       # Elasticsearch 전문 검색
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
│   └── elasticsearch/        # Elasticsearch 이미지 빌드
├── argocd/                   # ArgoCD Application 매니페스트
├── .github/workflows/        # GitHub Actions CI
├── docker-compose.yml        # 로컬 인프라 (PostgreSQL, Redis, Kafka, MinIO, Elasticsearch)
├── start-dev.sh              # 로컬 전체 일괄 실행
└── stop-dev.sh               # 로컬 전체 일괄 종료
```

---

## 실행 가이드

### 사전 요구사항

- Java 17+
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
# 1. 인프라 실행 (PostgreSQL, Redis, Kafka, MinIO, Elasticsearch)
docker compose up -d

# 2. 백엔드 서비스 (각각 별도 터미널)
./gradlew bootRun -p backend/gateway-service
./gradlew bootRun -p backend/user-service
./gradlew bootRun -p backend/report-service
./gradlew bootRun -p backend/search-service

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
| Swagger (Search) | http://localhost:8083/swagger-ui.html | - |
| Elasticsearch | http://localhost:9200 | - |
| MinIO 콘솔 | http://localhost:9001 | minioadmin / minioadmin |

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

파드 헬스 프로브는 Spring Actuator의 `/actuator/health`를 사용합니다.

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
cd backend/{service-name}
./gradlew test
```

전체 107개 테스트 (서비스 레이어 단위 테스트 + API 통합 테스트)

| 서비스 | 테스트 수 |
|---|---|
| gateway-service | 20 |
| user-service | 37 |
| report-service | 50 |

---

## 서비스 간 통신

### REST (동기) - FeignClient

```
report-service → user-service
  GET /api/user/{id}    # 알림 변환 시 loginId 조회
  GET /api/user/all     # 실종 신고 전체 알림 발송
```

### Kafka (비동기)

| 토픽 | Producer | Consumer (group-id) | 용도 |
|---|---|---|---|
| `user-deleted` | user-service | report-service | 회원 탈퇴 시 관련 데이터 정리 |
| `missing-created` | report-service | search-service | 실종 신고 → Elasticsearch 인덱싱 |
| `missing-created` | report-service | report-service-notification | 실종 신고 → 전체 유저 알림 발송 |
| `missing-deleted` | report-service | search-service | 실종 신고 삭제 → 인덱스 제거 |

> `missing-created`는 컨슈머 그룹을 분리해 인덱싱과 알림 발송이 서로 독립적으로 처리됩니다.

### WebSocket (실시간)

- STOMP 프로토콜, 알림 경로: `/user/{userId}/queue/notification`
- Redis Pub/Sub으로 다중 인스턴스 간 브로드캐스트

---

## 검색 (Elasticsearch)

`missing` 인덱스에 실종 신고를 색인하고, Kafka 이벤트로 동기화합니다.

```
GET /api/search?q=검색어&petType=dog&petGender=M&page=0&size=10
```

- **multi_match**: 제목, 내용, 이름, 품종, 실종장소를 한 번에 검색
- **필드 가중치**: `petBreed^3`, `title^2`, `petMissingPlace^2` — 목격자가 주로 품종과 장소로 검색하는 패턴 반영
- **most_fields**: 여러 필드에 걸친 조합 검색 시 점수를 합산 (예: "말티즈 강남")
- **fuzziness AUTO**: 표기 흔들림 보정 (말티즈 ↔ 몰티즈)
- **필터**: `petType`, `petGender`는 `keyword` 필드로 매핑해 `filter` 절에서 처리 (점수 계산 생략 + 캐싱)

검색어가 없는 목록 브라우징은 정렬(최신순/오래된순)이 필요하므로 report-service의 `/api/missing`을 사용합니다.
