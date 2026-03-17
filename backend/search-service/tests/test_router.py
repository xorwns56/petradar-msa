"""
search-service router 단위 테스트
- FastAPI TestClient로 HTTP 요청/응답 검증
- CLIP 모델과 DB는 모킹하여 외부 의존성 없이 실행
"""
import io
from unittest.mock import MagicMock, patch

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from database import get_db
from router import router


@pytest.fixture
def mock_db():
    """가짜 DB 세션"""
    return MagicMock()


@pytest.fixture
def app(mock_db):
    """테스트용 FastAPI 앱 - DB 의존성을 Mock으로 교체"""
    test_app = FastAPI()
    test_app.include_router(router)
    # FastAPI dependency_overrides로 DB 세션 교체
    test_app.dependency_overrides[get_db] = lambda: mock_db
    return test_app


@pytest.fixture
def client(app):
    """테스트 클라이언트"""
    return TestClient(app)


# ========== /api/search/index 테스트 ==========

class TestIndexMissing:

    @patch("router.get_image_vector")
    def test_index_missing_new(self, mock_get_image_vector, client, mock_db):
        """새 임베딩 저장 (기존 데이터 없을 때)"""
        # given
        mock_vector = [0.1] * 512
        mock_get_image_vector.return_value = mock_vector
        mock_db.query.return_value.filter.return_value.first.return_value = None

        image_bytes = b"\x89PNG\r\n\x1a\n" + b"\x00" * 100

        # when
        response = client.post(
            "/api/search/index",
            data={"missing_id": 1},
            files={"image": ("test.png", io.BytesIO(image_bytes), "image/png")}
        )

        # then
        assert response.status_code == 200
        assert response.json() == {"message": "indexed"}
        mock_db.add.assert_called_once()
        mock_db.commit.assert_called_once()

    @patch("router.get_image_vector")
    def test_index_missing_update(self, mock_get_image_vector, client, mock_db):
        """기존 임베딩 업데이트 (이미 존재할 때)"""
        # given
        mock_vector = [0.2] * 512
        mock_get_image_vector.return_value = mock_vector
        mock_existing = MagicMock()
        mock_db.query.return_value.filter.return_value.first.return_value = mock_existing

        image_bytes = b"\x89PNG\r\n\x1a\n" + b"\x00" * 100

        # when
        response = client.post(
            "/api/search/index",
            data={"missing_id": 1},
            files={"image": ("test.png", io.BytesIO(image_bytes), "image/png")}
        )

        # then
        assert response.status_code == 200
        assert mock_existing.image_vector == mock_vector
        mock_db.add.assert_not_called()  # 업데이트이므로 add 호출 안 함


# ========== /api/search/index/{missing_id} 삭제 테스트 ==========

class TestDeleteIndex:

    def test_delete_index(self, client, mock_db):
        """임베딩 삭제"""
        # when
        response = client.delete("/api/search/index/1")

        # then
        assert response.status_code == 200
        assert response.json() == {"message": "deleted"}
        mock_db.query.return_value.filter.return_value.delete.assert_called_once()
        mock_db.commit.assert_called_once()


# ========== /api/search/image 테스트 ==========

class TestSearchByImage:

    @patch("router.get_image_vector")
    def test_search_by_image(self, mock_get_image_vector, client, mock_db):
        """이미지 검색 - 유사 실종동물 반환"""
        # given
        mock_get_image_vector.return_value = [0.1] * 512
        # pgvector 쿼리 결과 모킹
        mock_row1 = MagicMock()
        mock_row1.missing_id = 1
        mock_row1.similarity = 0.9512
        mock_row2 = MagicMock()
        mock_row2.missing_id = 3
        mock_row2.similarity = 0.8234
        mock_db.execute.return_value.fetchall.return_value = [mock_row1, mock_row2]

        image_bytes = b"\x89PNG\r\n\x1a\n" + b"\x00" * 100

        # when
        response = client.post(
            "/api/search/image",
            data={"top_k": 5},
            files={"image": ("test.png", io.BytesIO(image_bytes), "image/png")}
        )

        # then
        assert response.status_code == 200
        data = response.json()
        assert len(data["results"]) == 2
        assert data["results"][0]["missing_id"] == 1
        assert data["results"][0]["similarity"] == 0.9512
        assert data["results"][1]["missing_id"] == 3


# ========== /api/search/text 테스트 ==========

class TestSearchByText:

    @patch("router.get_text_vector")
    def test_search_by_text(self, mock_get_text_vector, client, mock_db):
        """텍스트 검색 - 크로스 모달 유사도 검색"""
        # given
        mock_get_text_vector.return_value = [0.05] * 512
        mock_row = MagicMock()
        mock_row.missing_id = 5
        mock_row.similarity = 0.7890
        mock_db.execute.return_value.fetchall.return_value = [mock_row]

        # when
        response = client.post(
            "/api/search/text",
            json={"query": "갈색 강아지", "top_k": 3}
        )

        # then
        assert response.status_code == 200
        data = response.json()
        assert len(data["results"]) == 1
        assert data["results"][0]["missing_id"] == 5
        assert data["results"][0]["similarity"] == 0.789

    @patch("router.get_text_vector")
    def test_search_by_text_empty_results(self, mock_get_text_vector, client, mock_db):
        """검색 결과 없을 때 빈 배열 반환"""
        # given
        mock_get_text_vector.return_value = [0.0] * 512
        mock_db.execute.return_value.fetchall.return_value = []

        # when
        response = client.post(
            "/api/search/text",
            json={"query": "존재하지않는동물", "top_k": 5}
        )

        # then
        assert response.status_code == 200
        assert response.json()["results"] == []

    @patch("router.get_text_vector")
    def test_search_by_text_default_top_k(self, mock_get_text_vector, client, mock_db):
        """top_k 미지정 시 기본값 5 사용"""
        # given
        mock_get_text_vector.return_value = [0.1] * 512
        mock_db.execute.return_value.fetchall.return_value = []

        # when
        response = client.post(
            "/api/search/text",
            json={"query": "강아지"}
        )

        # then
        assert response.status_code == 200
