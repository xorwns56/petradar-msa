from fastapi import APIRouter, Depends, File, Form, UploadFile, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import text
from database import get_db
from models import MissingEmbedding
from schemas import SearchResponse, SearchResult, TextSearchRequest
from clip_model import get_image_vector, get_text_vector
from typing import Optional

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
    return _search(vector, top_k, db)


@router.post("/text", response_model=SearchResponse)
def search_by_text(request: TextSearchRequest, db: Session = Depends(get_db)):
    """
    텍스트로 유사 실종 동물 검색 (크로스 모달)
    "갈색 강아지" → CLIP 텍스트 인코더 → image_vector와 유사도 비교
    """
    vector = get_text_vector(request.query)
    return _search(vector, request.top_k, db)


def _search(vector: list[float], top_k: int, db: Session) -> SearchResponse:
    """
    pgvector 코사인 유사도 검색
    image_vector와 text_vector 모두 비교 후 더 유사한 값을 similarity로 사용
    이미지/텍스트 어느 쪽으로 검색해도 두 벡터를 모두 활용해 더 좋은 결과 반환
    """
    results = db.execute(
        text("""
            SELECT missing_id,
                   GREATEST(
                       CASE WHEN image_vector IS NOT NULL
                            THEN 1 - (image_vector <=> CAST(:vector AS vector))
                            ELSE 0 END,
                       CASE WHEN text_vector IS NOT NULL
                            THEN 1 - (text_vector <=> CAST(:vector AS vector))
                            ELSE 0 END
                   ) AS similarity
            FROM missing_embedding
            ORDER BY similarity DESC
            LIMIT :top_k
        """),
        {"vector": str(vector), "top_k": top_k}
    ).fetchall()

    return SearchResponse(
        results=[
            SearchResult(missing_id=row.missing_id, similarity=round(row.similarity, 4))
            for row in results
        ]
    )
