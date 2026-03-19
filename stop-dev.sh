#!/bin/bash
# ===========================================
# PetRadar MSA 로컬 개발 환경 일괄 종료 스크립트
# 애플리케이션 프로세스 + Docker 인프라 전체 정리
# ===========================================

echo "=== PetRadar 서비스 종료 중 ==="

# -----------------------------------------
# 1. 백엔드 Java 서비스 종료 (Gradle bootRun 프로세스)
# -----------------------------------------
echo "백엔드 서비스 종료 중..."
pkill -f "gradlew bootRun" 2>/dev/null
pkill -f "backend/gateway-service" 2>/dev/null
pkill -f "backend/user-service" 2>/dev/null
pkill -f "backend/report-service" 2>/dev/null

# -----------------------------------------
# 2. Search Service 종료 (uvicorn)
# -----------------------------------------
echo "Search Service 종료 중..."
pkill -f "uvicorn main:app" 2>/dev/null

# -----------------------------------------
# 3. Frontend 종료 (Vite)
# -----------------------------------------
echo "Frontend 종료 중..."
pkill -f "vite" 2>/dev/null

# -----------------------------------------
# 4. Docker 인프라 종료
# -----------------------------------------
echo "Docker 인프라 종료 중..."
docker compose down

echo ""
echo "=== 전체 종료 완료 ==="
