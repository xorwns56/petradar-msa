import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI

# 로그 파일 출력 설정 (Promtail이 수집)
os.makedirs("logs", exist_ok=True)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    handlers=[
        logging.StreamHandler(),                              # 콘솔 출력
        logging.FileHandler("logs/search-service.log"),       # 파일 출력
    ],
)
from faststream.kafka import KafkaBroker
from prometheus_fastapi_instrumentator import Instrumentator

from database import init_db
from kafka_consumer import broker
from router import router


# lifespan: 앱 시작/종료 시 실행할 로직 정의
@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()          # 앱 시작 시 테이블 생성
    await broker.start()  # Kafka broker 시작 (subscriber 등록)
    yield
    await broker.close()  # 앱 종료 시 Kafka broker 정상 종료


app = FastAPI(
    title="PetRadar Search Service",
    lifespan=lifespan
)

# 라우터 등록
app.include_router(router)

# Prometheus 메트릭 수집 엔드포인트 등록 (/metrics)
Instrumentator().instrument(app).expose(app)


@app.get("/health")
def health():
    return {"status": "UP"}
