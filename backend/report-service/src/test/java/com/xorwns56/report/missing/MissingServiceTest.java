package com.xorwns56.report.missing;

import com.xorwns56.report.minio.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MissingServiceTest {

    @InjectMocks
    private MissingService missingService;

    @Mock
    private MissingRepository missingRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Missing testMissing;

    @BeforeEach
    void setUp() {
        testMissing = Missing.builder()
                .id(1L)
                .userId(10L)
                .petName("멍멍이")
                .petType("강아지")
                .petGender("수컷")
                .petBreed("골든리트리버")
                .petAge("3살")
                .petMissingDate("2025-01-01")
                .petMissingPlace("서울시 강남구")
                .latitude(37.5172)
                .longitude(127.0473)
                .title("강아지를 찾습니다")
                .content("골든리트리버, 갈색, 3살")
                .petImage("http://minio:9000/pet-images/test.jpg")
                .build();
    }

    // ========== 목록 조회 테스트 ==========

    @Nested
    @DisplayName("실종 신고 목록 조회")
    class GetList {

        @Test
        @DisplayName("검색어 + 최신순 정렬 조회")
        void getList_latestSort() {
            // given
            given(missingRepository.findByTitleContainingIgnoreCase(eq("강아지"), any(Sort.class)))
                    .willReturn(List.of(testMissing));

            // when
            List<MissingDTO.Response> result = missingService.getList("강아지", "latest");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("강아지를 찾습니다");
        }

        @Test
        @DisplayName("페이지네이션 조회")
        void getList_withPagination() {
            // given
            Page<Missing> page = new PageImpl<>(List.of(testMissing), PageRequest.of(0, 10), 1);
            given(missingRepository.findByTitleContainingIgnoreCase(eq(""), any(Pageable.class)))
                    .willReturn(page);

            // when
            Page<MissingDTO.Response> result = missingService.getList("", "latest", PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("ID 목록으로 조회 - 요청 순서 유지")
        void getByIds_preservesOrder() {
            // given
            Missing missing2 = Missing.builder().id(2L).userId(10L).petName("야옹이").title("고양이 찾아요").build();
            // DB에서 id=2가 먼저 반환되어도 요청 순서(1, 2) 유지 확인
            given(missingRepository.findByIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(missing2, testMissing));

            // when
            List<MissingDTO.Response> result = missingService.getByIds(List.of(1L, 2L));

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(1).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("내 실종 신고 목록 조회")
        void getMyList() {
            // given
            given(missingRepository.findByUserId(eq(10L), any(Sort.class)))
                    .willReturn(List.of(testMissing));

            // when
            List<MissingDTO.Response> result = missingService.getMyList(10L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(10L);
        }
    }

    // ========== 상세 조회 테스트 ==========

    @Nested
    @DisplayName("실종 신고 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("정상 조회")
        void getDetail_success() {
            // given
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));

            // when
            MissingDTO.Response result = missingService.getDetail(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPetName()).isEqualTo("멍멍이");
            assertThat(result.getPetMissingPoint().getLat()).isEqualTo(37.5172);
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 예외 발생")
        void getDetail_notFound() {
            // given
            given(missingRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> missingService.getDetail(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("실종 신고를 찾을 수 없습니다.");
        }
    }

    // ========== 등록 테스트 ==========

    @Nested
    @DisplayName("실종 신고 등록")
    class Create {

        @Test
        @DisplayName("이미지 포함 등록 - MinIO 업로드 + DB 저장 + Kafka 이벤트 발행")
        void create_withImage() {
            // given
            MissingDTO.Request request = new MissingDTO.Request(
                    "멍멍이", "강아지", "수컷", "골든리트리버", "3살",
                    "2025-01-01", "서울시 강남구",
                    new MissingDTO.Point(37.5172, 127.0473),
                    null, "강아지를 찾습니다", "골든리트리버입니다"
            );
            MultipartFile mockImage = mock(MultipartFile.class);
            given(mockImage.isEmpty()).willReturn(false);
            given(minioService.upload(mockImage)).willReturn("http://minio:9000/pet-images/uuid-test.jpg");
            given(missingRepository.save(any(Missing.class))).willAnswer(invocation -> {
                Missing saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            missingService.create(10L, request, mockImage);

            // then
            then(minioService).should().upload(mockImage);
            then(missingRepository).should().save(any(Missing.class));
            then(kafkaTemplate).should().send(eq("missing-created"), any());
        }

        @Test
        @DisplayName("이미지 없이 등록")
        void create_withoutImage() {
            // given
            MissingDTO.Request request = new MissingDTO.Request(
                    "야옹이", "고양이", "암컷", "러시안블루", "2살",
                    "2025-01-15", "서울시 서초구",
                    null, null, "고양이를 찾습니다", "러시안블루입니다"
            );
            given(missingRepository.save(any(Missing.class))).willAnswer(invocation -> {
                Missing saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            // when
            missingService.create(10L, request, null);

            // then
            then(minioService).should(never()).upload(any());
            then(missingRepository).should().save(any(Missing.class));
            // Kafka 이벤트에 imageUrl이 null로 발행
            then(kafkaTemplate).should().send(eq("missing-created"), any());
        }
    }

    // ========== 수정 테스트 ==========

    @Nested
    @DisplayName("실종 신고 수정")
    class Update {

        @Test
        @DisplayName("정상 수정 - 작성자 본인")
        void update_success() {
            // given
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));
            MissingDTO.Request request = new MissingDTO.Request(
                    "멍멍이(수정)", "강아지", "수컷", "골든리트리버", "4살",
                    "2025-01-01", "서울시 송파구",
                    new MissingDTO.Point(37.5145, 127.1060),
                    null, "강아지를 찾습니다 (수정)", "수정된 내용"
            );

            // when
            missingService.update(1L, 10L, request);

            // then
            assertThat(testMissing.getPetName()).isEqualTo("멍멍이(수정)");
            assertThat(testMissing.getTitle()).isEqualTo("강아지를 찾습니다 (수정)");
            assertThat(testMissing.getPetMissingPlace()).isEqualTo("서울시 송파구");
        }

        @Test
        @DisplayName("다른 사용자가 수정 시 예외 발생")
        void update_notOwner_throwsException() {
            // given
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));
            MissingDTO.Request request = new MissingDTO.Request(
                    "수정시도", null, null, null, null,
                    null, null, null, null, "제목", "내용"
            );

            // when & then - userId 99L은 작성자(10L)가 아님
            assertThatThrownBy(() -> missingService.update(1L, 99L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수정 권한이 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 게시물 수정 시 예외 발생")
        void update_notFound() {
            // given
            given(missingRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> missingService.update(999L, 10L, any()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ========== 삭제 테스트 ==========

    @Nested
    @DisplayName("실종 신고 삭제")
    class Delete {

        @Test
        @DisplayName("정상 삭제 - MinIO 이미지 삭제 + DB 삭제 + Kafka 이벤트 발행")
        void delete_success() {
            // given
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));

            // when
            missingService.delete(1L, 10L);

            // then
            then(minioService).should().delete("http://minio:9000/pet-images/test.jpg");
            then(missingRepository).should().delete(testMissing);
            then(kafkaTemplate).should().send(eq("missing-deleted"), any());
        }

        @Test
        @DisplayName("이미지 없는 게시물 삭제 시 MinIO 삭제 호출 안 함")
        void delete_noImage() {
            // given
            testMissing.setPetImage(null);
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));

            // when
            missingService.delete(1L, 10L);

            // then
            then(minioService).should(never()).delete(anyString());
            then(missingRepository).should().delete(testMissing);
        }

        @Test
        @DisplayName("다른 사용자가 삭제 시 예외 발생")
        void delete_notOwner_throwsException() {
            // given
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));

            // when & then
            assertThatThrownBy(() -> missingService.delete(1L, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("삭제 권한이 없습니다.");
        }
    }
}
