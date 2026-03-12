import asyncio
import logging
import os
from typing import Optional

import requests
from faststream.kafka import KafkaBroker
from pydantic import BaseModel

from clip_model import get_image_vector, get_text_vector
from database import SessionLocal
from models import MissingEmbedding

logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")

# KafkaBroker - Spring의 KafkaListenerContainerFactory와 유사
broker = KafkaBroker(KAFKA_BOOTSTRAP_SERVERS)


class MissingCreatedEvent(BaseModel):
    missingId: int
    userId: int
    imageUrl: Optional[str] = None
    petName: Optional[str] = None
    petType: Optional[str] = None
    petGender: Optional[str] = None
    petBreed: Optional[str] = None
    petAge: Optional[str] = None
    petMissingPlace: Optional[str] = None
    title: Optional[str] = None
    content: Optional[str] = None


class MissingDeletedEvent(BaseModel):
    missingId: int


@broker.subscriber("missing-created", group_id="search-service")
async def handle_missing_created(msg: MissingCreatedEvent):
    """
    missing-created 이벤트 처리
    - 이미지 있으면 → CLIP 이미지 인코더 → image_vector 저장
    - 텍스트 설명 → CLIP 텍스트 인코더 → text_vector 저장
    검색 시 두 벡터 모두 비교해서 더 유사한 결과 반환
    """
    logger.info("missing-created 이벤트 수신 - missingId: %d", msg.missingId)
    await asyncio.to_thread(_process_sync, msg)


def _process_sync(msg: MissingCreatedEvent):
    """동기 처리 로직 - asyncio.to_thread()로 이벤트 루프 블로킹 방지"""
    image_vector = None
    text_vector = None

    # 1. 이미지 벡터화 (이미지 있을 때만)
    if msg.imageUrl:
        try:
            response = requests.get(msg.imageUrl, timeout=10)
            response.raise_for_status()
            image_vector = get_image_vector(response.content)
        except Exception as e:
            logger.error("이미지 벡터화 실패 - missingId: %d, error: %s", msg.missingId, str(e))

    # 2. 텍스트 벡터화 (pet 설명 조합)
    # 검색 품질을 높이기 위해 주요 필드를 공백으로 연결
    text_parts = [msg.petType, msg.petBreed, msg.petGender, msg.petName,
                  msg.petMissingPlace, msg.title, msg.content]
    text = " ".join(part for part in text_parts if part)

    if text.strip():
        try:
            text_vector = get_text_vector(text)
        except Exception as e:
            logger.error("텍스트 벡터화 실패 - missingId: %d, error: %s", msg.missingId, str(e))

    # 3. pgvector 저장
    if image_vector is None and text_vector is None:
        logger.warning("벡터화 결과 없음 - missingId: %d", msg.missingId)
        return

    db = SessionLocal()
    try:
        existing = db.query(MissingEmbedding).filter(
            MissingEmbedding.missing_id == msg.missingId
        ).first()

        if existing:
            existing.image_vector = image_vector
            existing.text_vector = text_vector
        else:
            db.add(MissingEmbedding(
                missing_id=msg.missingId,
                image_vector=image_vector,
                text_vector=text_vector
            ))

        db.commit()
        logger.info("임베딩 저장 완료 - missingId: %d", msg.missingId)
    finally:
        db.close()


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
