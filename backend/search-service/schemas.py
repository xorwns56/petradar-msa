from pydantic import BaseModel
from typing import List

# Pydantic은 Spring의 DTO와 동일한 역할
# 요청/응답 데이터 구조를 정의하고 자동으로 유효성 검사

# 검색 결과 단건
class SearchResult(BaseModel):
    missing_id: int
    similarity: float  # 유사도 점수 (0~1, 높을수록 유사)

# 검색 응답
class SearchResponse(BaseModel):
    results: List[SearchResult]
