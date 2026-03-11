from sqlalchemy import Column, BigInteger
from pgvector.sqlalchemy import Vector
from database import Base

# Spring의 @Entity와 동일한 개념
# missing_embedding 테이블 정의
class MissingEmbedding(Base):
    __tablename__ = "missing_embedding"

    # Spring의 @Id @GeneratedValue와 달리, missing_id는 report-service의 missing.id를 그대로 사용
    missing_id = Column(BigInteger, primary_key=True)

    # CLIP(clip-vit-base-patch32) 벡터 차원: 512
    # 이미지와 텍스트 모두 이 컬럼 하나로 검색 가능 (크로스 모달)
    image_vector = Column(Vector(512), nullable=False)
