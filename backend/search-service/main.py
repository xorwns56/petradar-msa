from fastapi import FastAPI
from contextlib import asynccontextmanager
from prometheus_fastapi_instrumentator import Instrumentator
from database import init_db
from router import router

# lifespan: 앱 시작/종료 시 실행할 로직 정의
# Spring의 @PostConstruct / ApplicationRunner와 유사한 개념
@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()   # 앱 시작 시 테이블 생성
    yield       # 앱 실행 중
                # 앱 종료 시 여기 아래 코드 실행 (현재는 없음)

app = FastAPI(
    title="PetRadar Search Service",
    lifespan=lifespan
)

# 라우터 등록 (Spring의 @ComponentScan과 유사)
app.include_router(router)

# Prometheus 메트릭 수집 엔드포인트 등록 (/metrics)
Instrumentator().instrument(app).expose(app)

@app.get("/health")
def health():
    return {"status": "UP"}
