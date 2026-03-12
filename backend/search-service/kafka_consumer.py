import asyncio
import logging
import os

import requests
from faststream.kafka import KafkaBroker
from pydantic import BaseModel

from clip_model import get_image_vector
from database import SessionLocal
from models import MissingEmbedding

logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")

# KafkaBroker - Spring의 KafkaListenerContainerFactory와 유사
broker = KafkaBroker(KAFKA_BOOTSTRAP_SERVERS)


# Pydantic으로 메시지 스키마 정의 - 역직렬화 + 유효성 검사 자동
class MissingCreatedEvent(BaseModel):
    missingId: int
    imageUrl: str


class MissingDeletedEvent(BaseModel):
    missingId: int


# @broker.subscriber - Spring의 @KafkaListener와 동일한 개념
@broker.subscriber("missing-created", group_id="search-service")
async def handle_missing_created(msg: MissingCreatedEvent):
    """
    missing-created 이벤트 처리
    MinIO에서 이미지 다운로드 → CLIP 벡터화 → pgvector 저장
    블로킹 작업은 asyncio.to_thread()로 스레드풀에서 실행
    """
    logger.info("missing-created 이벤트 수신 - missingId: %d", msg.missingId)
    await asyncio.to_thread(_process_sync, msg.missingId, msg.imageUrl)


@broker.subscriber("missing-deleted", group_id="search-service")
async def handle_missing_deleted(msg: MissingDeletedEvent):
    """missing-deleted 이벤트 처리 - pgvector 임베딩 삭제"""
    logger.info("missing-deleted 이벤트 수신 - missingId: %d", msg.missingId)
    await asyncio.to_thread(_delete_embedding, msg.missingId)


def _delete_embedding(missing_id: int):
    """pgvector에서 임베딩 삭제"""
    db = SessionLocal()
    try:
        db.query(MissingEmbedding).filter(
            MissingEmbedding.missing_id == missing_id
        ).delete()
        db.commit()
        logger.info("임베딩 삭제 완료 - missingId: %d", missing_id)
    except Exception as e:
        logger.error("임베딩 삭제 실패 - missingId: %d, error: %s", missing_id, str(e))
    finally:
        db.close()


def _process_sync(missing_id: int, image_url: str):
    """동기 처리 로직 - asyncio.to_thread()로 이벤트 루프 블로킹 방지"""
    try:
        # MinIO에서 이미지 다운로드
        response = requests.get(image_url, timeout=10)
        response.raise_for_status()

        # CLIP 벡터화
        vector = get_image_vector(response.content)

        # pgvector 저장
        db = SessionLocal()
        try:
            existing = db.query(MissingEmbedding).filter(
                MissingEmbedding.missing_id == missing_id
            ).first()

            if existing:
                existing.image_vector = vector
            else:
                db.add(MissingEmbedding(missing_id=missing_id, image_vector=vector))

            db.commit()
            logger.info("임베딩 저장 완료 - missingId: %d", missing_id)
        finally:
            db.close()

    except Exception as e:
        logger.error("임베딩 처리 실패 - missingId: %d, error: %s", missing_id, str(e))
