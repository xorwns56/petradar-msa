import os
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, DeclarativeBase
from dotenv import load_dotenv

load_dotenv()

# PostgreSQL 연결 URL (Spring의 datasource.url과 동일한 개념)
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://postgres:1234@localhost:5432/search_db"  # 로컬 기본값
)

# SQLAlchemy 엔진 생성 (Spring의 DataSource)
engine = create_engine(DATABASE_URL)

# 세션 팩토리 생성 (Spring의 EntityManager와 유사)
SessionLocal = sessionmaker(bind=engine)

# 모든 모델이 상속받을 Base 클래스 (Spring의 @Entity 기반 클래스)
class Base(DeclarativeBase):
    pass

# pgvector 확장 활성화 및 테이블 생성
def init_db():
    with engine.connect() as conn:
        conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
        conn.commit()
    Base.metadata.create_all(bind=engine)

# FastAPI 의존성 주입용 DB 세션 (Spring의 @Autowired와 유사한 개념)
# 요청마다 세션을 열고 끝나면 자동으로 닫음
def get_db():
    db = SessionLocal()
    try:
        yield db  # yield: 이 시점에 컨트롤을 라우터에게 넘김
    finally:
        db.close()
