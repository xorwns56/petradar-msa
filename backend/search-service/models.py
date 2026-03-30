from sqlalchemy import Column, BigInteger
from pgvector.sqlalchemy import Vector
from database import Base

# Spring의 @Entity와 동일한 개념
# missing_embedding 테이블 정의
class MissingEmbedding(Base):
    __tablename__ = "missing_embedding"

    # Spring의 @Id @GeneratedValue와 달리, missing_id는 report-service의 missing.id를 그대로 사용
    missing_id = Column(BigInteger, primary_key=True)

    # 반려동물 사진으로 생성한 벡터 (이미지 없으면 null)
    image_vector = Column(Vector(512), nullable=True)
