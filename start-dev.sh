#!/bin/bash
# ===========================================
# PetRadar MSA 로컬 개발 환경 일괄 실행 스크립트
# 인프라(Docker) → 백엔드 4개 → 프론트엔드 순서로 실행
# Ctrl+C로 전체 종료
# ===========================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 정리 함수: 스크립트 종료 시 모든 프로세스 정리
cleanup() {
  echo ""
  echo "전체 서비스 정리 중..."
  kill $(jobs -p) 2>/dev/null
  wait 2>/dev/null
  docker compose down
  echo "정리 완료"
}
trap cleanup EXIT  # 스크립트 종료 시 자동 실행 (Ctrl+C 포함)

# -----------------------------------------
# 1. 인프라 실행 (PostgreSQL, Redis, Kafka, MinIO, 모니터링)
# -----------------------------------------
echo "=== 인프라 시작 ==="
docker compose up -d

echo "PostgreSQL 준비 대기 중..."
until docker compose exec postgres pg_isready -U postgres > /dev/null 2>&1; do
  sleep 1
done
echo "PostgreSQL 준비 완료"

echo "Redis 준비 대기 중..."
until docker compose exec redis redis-cli ping > /dev/null 2>&1; do
  sleep 1
done
echo "Redis 준비 완료"

echo "Kafka 준비 대기 중..."
until docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
  sleep 1
done
echo "Kafka 준비 완료"

echo "=== 인프라 준비 완료 ==="
echo ""

# -----------------------------------------
# 2. 백엔드 서비스 실행
# -----------------------------------------
echo "=== 백엔드 서비스 시작 ==="

"$SCRIPT_DIR"/gradlew bootRun -p "$SCRIPT_DIR/backend/gateway-service" &
"$SCRIPT_DIR"/gradlew bootRun -p "$SCRIPT_DIR/backend/user-service" &
"$SCRIPT_DIR"/gradlew bootRun -p "$SCRIPT_DIR/backend/report-service" &

# Python 가상환경의 uvicorn 직접 실행
(cd "$SCRIPT_DIR/backend/search-service" && ./venv/bin/uvicorn main:app --host 0.0.0.0 --port 8000 --reload) &

# -----------------------------------------
# 3. 프론트엔드 실행
# -----------------------------------------
echo "=== 프론트엔드 시작 ==="

(cd "$SCRIPT_DIR/frontend" && npm run dev) &

echo ""
echo "=== 전체 서비스 실행 완료 ==="
echo "  Frontend:       http://localhost:5173"
echo "  Gateway:        http://localhost:8080"
echo "  User Service:   http://localhost:8081"
echo "  Report Service: http://localhost:8082"
echo "  Search Service: http://localhost:8000"
echo "  Grafana:        http://localhost:3000"
echo "  MinIO Console:  http://localhost:9001"
echo ""
echo "종료하려면 Ctrl+C"
echo ""

# -----------------------------------------
# 4. 모든 백그라운드 프로세스 대기
# -----------------------------------------
wait
