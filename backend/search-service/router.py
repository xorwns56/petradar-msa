import logging
from fastapi import APIRouter, Depends, File, Form, UploadFile, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import text
from database import get_db
from models import MissingEmbedding
from schemas import SearchResponse, SearchResult
from clip_model import get_image_vector

logger = logging.getLogger(__name__)

# APIRouter: Spring의 @RestController와 유사
# prefix="/api/search"를 붙이면 모든 엔드포인트 앞에 자동으로 붙음
router = APIRouter(prefix="/api/search", tags=["search"])


@router.post("/index")
def index_missing(
    missing_id: int = Form(...),           # multipart form 데이터
    image: UploadFile = File(...),         # 업로드된 이미지 파일
    db: Session = Depends(get_db)          # DB 세션 의존성 주입 (Spring의 @Autowired)
):
    """
    실종 신고 등록 시 report-service에서 호출
    이미지 → CLIP 벡터화 → pgvector 저장
    """
    image_bytes = image.file.read()
    vector = get_image_vector(image_bytes)

    # 이미 존재하면 업데이트, 없으면 새로 저장 (upsert)
    existing = db.query(MissingEmbedding).filter(
        MissingEmbedding.missing_id == missing_id
    ).first()

    if existing:
        existing.image_vector = vector
    else:
        db.add(MissingEmbedding(missing_id=missing_id, image_vector=vector))

    db.commit()
    logger.info("이미지 인덱싱 완료: missingId=%d", missing_id)
    return {"message": "indexed"}


@router.delete("/index/{missing_id}")
def delete_index(missing_id: int, db: Session = Depends(get_db)):
    """
    실종 신고 삭제 시 report-service에서 호출
    pgvector에서 해당 임베딩 제거
    """
    db.query(MissingEmbedding).filter(
        MissingEmbedding.missing_id == missing_id
    ).delete()
    db.commit()
    logger.info("인덱스 삭제 완료: missingId=%d", missing_id)
    return {"message": "deleted"}


@router.post("/image", response_model=SearchResponse)
def search_by_image(
    image: UploadFile = File(...),
    top_k: int = Form(default=5),
    db: Session = Depends(get_db)
):
    """
    이미지로 유사 실종 동물 검색
    목격자가 찍은 사진 → CLIP → pgvector 코사인 유사도 검색
    """
    image_bytes = image.file.read()
    vector = get_image_vector(image_bytes)
    logger.info("이미지 검색 요청: top_k=%d", top_k)
    return _search(vector, top_k, db)


SIMILARITY_THRESHOLD = 0.75  # 최소 유사도 임계값 (CLIP 이미지-이미지 기준, 같은 종 + 유사 외형)

# CLIP 이미지-이미지 코사인 유사도 정규화 범위
# CLIP 벡터는 0.5~1.0 범위에 밀집되어 raw 값을 그대로 퍼센트로 보여주면 왜곡됨
# 예: raw 0.82(고양이↔강아지)가 82%로 표시 → 정규화 후 약 23%로 보정
SIMILARITY_MIN = 0.7   # 이 이하는 0%로 취급
SIMILARITY_MAX = 1.0   # 이 값이 100%


def _normalize_similarity(raw: float) -> float:
    """
    CLIP raw 코사인 유사도를 체감 유사도(0~1)로 정규화
    min-max 스케일링으로 SIMILARITY_MIN~SIMILARITY_MAX → 0.0~1.0 매핑
    """
    normalized = (raw - SIMILARITY_MIN) / (SIMILARITY_MAX - SIMILARITY_MIN)
    return max(0.0, min(1.0, normalized))


def _search(vector: list[float], top_k: int, db: Session) -> SearchResponse:
    """
    pgvector 코사인 유사도 검색 (image_vector 기반)
    CLIP 이미지 인코더로 생성된 벡터 간 코사인 유사도 비교
    threshold 이상인 결과만 반환하고, 체감 유사도로 정규화하여 응답
    """
    results = db.execute(
        text("""
            SELECT missing_id,
                   1 - (image_vector <=> CAST(:vector AS vector)) AS similarity
            FROM missing_embedding
            WHERE image_vector IS NOT NULL
              AND 1 - (image_vector <=> CAST(:vector AS vector)) >= :threshold
            ORDER BY similarity DESC
            LIMIT :top_k
        """),
        {"vector": str(vector), "top_k": top_k, "threshold": SIMILARITY_THRESHOLD}
    ).fetchall()

    logger.info("검색 결과: %d건 (threshold=%.2f)", len(results), SIMILARITY_THRESHOLD)
    return SearchResponse(
        results=[
            SearchResult(
                missing_id=row.missing_id,
                # raw 유사도를 체감 유사도로 정규화하여 반환
                similarity=round(_normalize_similarity(row.similarity), 4)
            )
            for row in results
        ]
    )
