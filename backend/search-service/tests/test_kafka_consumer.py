"""
search-service Kafka consumer 단위 테스트
- CLIP 모델, HTTP 요청, DB를 모킹하여 외부 의존성 없이 실행
"""
from unittest.mock import MagicMock, patch

import pytest

from kafka_consumer import (
    MissingCreatedEvent,
    MissingDeletedEvent,
    _process_sync,
    _delete_embedding,
)


# ========== _process_sync 테스트 (이미지+텍스트 벡터화 후 저장) ==========

class TestProcessSync:

    @patch("kafka_consumer.SessionLocal")
    @patch("kafka_consumer.get_text_vector")
    @patch("kafka_consumer.get_image_vector")
    @patch("kafka_consumer.requests")
    def test_process_with_image_and_text(self, mock_requests, mock_get_image_vector,
                                         mock_get_text_vector, mock_session_local):
        """이미지 + 텍스트 모두 있을 때 두 벡터 모두 저장"""
        # given
        mock_response = MagicMock()
        mock_response.content = b"fake_image_bytes"
        mock_requests.get.return_value = mock_response

        mock_get_image_vector.return_value = [0.1] * 512
        mock_get_text_vector.return_value = [0.2] * 512

        mock_db = MagicMock()
        mock_db.query.return_value.filter.return_value.first.return_value = None
        mock_session_local.return_value = mock_db

        event = MissingCreatedEvent(
            missingId=1, userId=10,
            imageUrl="http://minio:9000/pet-images/test.jpg",
            petName="멍멍이", petType="강아지", petGender="수컷",
            petBreed="골든리트리버", petAge="3살",
            petMissingPlace="서울시 강남구",
            title="강아지를 찾습니다", content="골든리트리버입니다"
        )

        # when
        _process_sync(event)

        # then
        mock_requests.get.assert_called_once_with("http://minio:9000/pet-images/test.jpg", timeout=10)
        mock_get_image_vector.assert_called_once_with(b"fake_image_bytes")
        mock_get_text_vector.assert_called_once()
        mock_db.add.assert_called_once()
        mock_db.commit.assert_called_once()
        mock_db.close.assert_called_once()

    @patch("kafka_consumer.SessionLocal")
    @patch("kafka_consumer.get_text_vector")
    @patch("kafka_consumer.get_image_vector")
    def test_process_without_image(self, mock_get_image_vector, mock_get_text_vector,
                                    mock_session_local):
        """이미지 없을 때 텍스트 벡터만 저장"""
        # given
        mock_get_text_vector.return_value = [0.2] * 512

        mock_db = MagicMock()
        mock_db.query.return_value.filter.return_value.first.return_value = None
        mock_session_local.return_value = mock_db

        event = MissingCreatedEvent(
            missingId=2, userId=20,
            imageUrl=None,  # 이미지 없음
            petName="야옹이", petType="고양이", petGender=None,
            petBreed=None, petAge=None, petMissingPlace=None,
            title="고양이 찾습니다", content=None
        )

        # when
        _process_sync(event)

        # then
        mock_get_image_vector.assert_not_called()  # 이미지 벡터화 호출 안 함
        mock_get_text_vector.assert_called_once()
        mock_db.add.assert_called_once()

    @patch("kafka_consumer.SessionLocal")
    @patch("kafka_consumer.get_text_vector")
    @patch("kafka_consumer.get_image_vector")
    @patch("kafka_consumer.requests")
    def test_process_image_download_failure(self, mock_requests, mock_get_image_vector,
                                             mock_get_text_vector, mock_session_local):
        """이미지 다운로드 실패 시 텍스트 벡터만 저장"""
        # given
        mock_requests.get.side_effect = Exception("Connection refused")
        mock_get_text_vector.return_value = [0.3] * 512

        mock_db = MagicMock()
        mock_db.query.return_value.filter.return_value.first.return_value = None
        mock_session_local.return_value = mock_db

        event = MissingCreatedEvent(
            missingId=3, userId=30,
            imageUrl="http://minio:9000/unreachable.jpg",
            petName="멍멍이", petType="강아지", petGender=None,
            petBreed=None, petAge=None, petMissingPlace=None,
            title="찾아주세요", content=None
        )

        # when
        _process_sync(event)

        # then - 이미지 벡터화 실패해도 텍스트 벡터로 저장
        mock_get_image_vector.assert_not_called()
        mock_db.add.assert_called_once()

    @patch("kafka_consumer.SessionLocal")
    @patch("kafka_consumer.get_text_vector")
    @patch("kafka_consumer.get_image_vector")
    def test_process_no_vectors(self, mock_get_image_vector, mock_get_text_vector,
                                 mock_session_local):
        """이미지도 텍스트도 없으면 저장 안 함"""
        # given - 모든 필드가 None
        event = MissingCreatedEvent(
            missingId=4, userId=40,
            imageUrl=None,
            petName=None, petType=None, petGender=None,
            petBreed=None, petAge=None, petMissingPlace=None,
            title=None, content=None
        )

        # when
        _process_sync(event)

        # then - DB 저장 호출 안 함
        mock_session_local.assert_not_called()

    @patch("kafka_consumer.SessionLocal")
    @patch("kafka_consumer.get_text_vector")
    @patch("kafka_consumer.get_image_vector")
    @patch("kafka_consumer.requests")
    def test_process_updates_existing_embedding(self, mock_requests, mock_get_image_vector,
                                                  mock_get_text_vector, mock_session_local):
        """기존 임베딩이 있으면 업데이트 (upsert)"""
        # given
        mock_response = MagicMock()
        mock_response.content = b"image"
        mock_requests.get.return_value = mock_response

        mock_get_image_vector.return_value = [0.5] * 512
        mock_get_text_vector.return_value = [0.6] * 512

        mock_existing = MagicMock()
        mock_db = MagicMock()
        mock_db.query.return_value.filter.return_value.first.return_value = mock_existing
        mock_session_local.return_value = mock_db

        event = MissingCreatedEvent(
            missingId=1, userId=10,
            imageUrl="http://minio:9000/pet-images/test.jpg",
            petName="멍멍이", petType="강아지", petGender=None,
            petBreed=None, petAge=None, petMissingPlace=None,
            title="찾아주세요", content=None
        )

        # when
        _process_sync(event)

        # then - 기존 레코드 업데이트, add 호출 안 함
        assert mock_existing.image_vector == [0.5] * 512
        assert mock_existing.text_vector == [0.6] * 512
        mock_db.add.assert_not_called()


# ========== _delete_embedding 테스트 ==========

class TestDeleteEmbedding:

    @patch("kafka_consumer.SessionLocal")
    def test_delete_embedding_success(self, mock_session_local):
        """임베딩 삭제 성공"""
        # given
        mock_db = MagicMock()
        mock_session_local.return_value = mock_db

        # when
        _delete_embedding(1)

        # then
        mock_db.query.return_value.filter.return_value.delete.assert_called_once()
        mock_db.commit.assert_called_once()
        mock_db.close.assert_called_once()

    @patch("kafka_consumer.SessionLocal")
    def test_delete_embedding_failure_closes_db(self, mock_session_local):
        """삭제 실패해도 DB 세션 정상 종료"""
        # given
        mock_db = MagicMock()
        mock_db.query.return_value.filter.return_value.delete.side_effect = Exception("DB error")
        mock_session_local.return_value = mock_db

        # when
        _delete_embedding(999)

        # then - 예외 발생해도 close 호출
        mock_db.close.assert_called_once()
