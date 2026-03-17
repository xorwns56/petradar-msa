package com.xorwns56.report.report;

import com.xorwns56.report.missing.Missing;
import com.xorwns56.report.missing.MissingRepository;
import com.xorwns56.report.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private MissingRepository missingRepository;

    @Mock
    private NotificationService notificationService;

    private Missing testMissing;
    private Report testReport;

    @BeforeEach
    void setUp() {
        testMissing = Missing.builder()
                .id(1L)
                .userId(10L)
                .petName("멍멍이")
                .title("강아지를 찾습니다")
                .build();

        testReport = Report.builder()
                .id(100L)
                .userId(20L)
                .missing(testMissing)
                .title("여기서 봤어요")
                .content("공원에서 비슷한 강아지를 봤습니다")
                .petReportPlace("서울시 강남구 역삼동")
                .latitude(37.5000)
                .longitude(127.0400)
                .build();
    }

    // ========== 상세 조회 테스트 ==========

    @Nested
    @DisplayName("목격 제보 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("정상 조회")
        void getDetail_success() {
            // given
            given(reportRepository.findById(100L)).willReturn(Optional.of(testReport));

            // when
            ReportDTO.Response result = reportService.getDetail(100L);

            // then
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getMissingId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("여기서 봤어요");
            assertThat(result.getPetReportPoint().getLat()).isEqualTo(37.5000);
        }

        @Test
        @DisplayName("존재하지 않는 제보 조회 시 예외 발생")
        void getDetail_notFound() {
            // given
            given(reportRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.getDetail(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("목격 제보를 찾을 수 없습니다.");
        }
    }

    // ========== 목록 조회 테스트 ==========

    @Nested
    @DisplayName("실종 신고별 목격 제보 목록 조회")
    class GetListByMissingId {

        @Test
        @DisplayName("페이지네이션으로 조회")
        void getListByMissingId_success() {
            // given
            Page<Report> page = new PageImpl<>(List.of(testReport), PageRequest.of(0, 10), 1);
            given(reportRepository.findByMissingId(eq(1L), any(Pageable.class))).willReturn(page);

            // when
            Page<ReportDTO.Response> result = reportService.getListByMissingId(1L, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("여기서 봤어요");
        }
    }

    // ========== 등록 테스트 ==========

    @Nested
    @DisplayName("목격 제보 등록")
    class Create {

        @Test
        @DisplayName("정상 등록 - DB 저장 + 실종 신고 작성자에게 알림 전송")
        void create_success() {
            // given
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));
            ReportDTO.Request request = new ReportDTO.Request(
                    "여기서 봤어요", "공원에서 봤습니다", null, "서울시 강남구",
                    new ReportDTO.Point(37.5, 127.04)
            );

            // when
            reportService.create(20L, 1L, request);

            // then
            then(reportRepository).should().save(any(Report.class));
            // 실종 신고 작성자(userId=10L)에게 알림 전송 확인
            then(notificationService).should().sendToUser(20L, 10L, "report", 1L);
        }

        @Test
        @DisplayName("비회원 제보 - userId null로 등록")
        void create_anonymousUser() {
            // given
            given(missingRepository.findById(1L)).willReturn(Optional.of(testMissing));
            ReportDTO.Request request = new ReportDTO.Request(
                    "제보합니다", "비슷한 동물을 봤어요", null, "서울시 서초구", null
            );

            // when
            reportService.create(null, 1L, request);

            // then
            then(reportRepository).should().save(any(Report.class));
            then(notificationService).should().sendToUser(null, 10L, "report", 1L);
        }

        @Test
        @DisplayName("존재하지 않는 실종 신고에 제보 시 예외 발생")
        void create_missingNotFound() {
            // given
            given(missingRepository.findById(999L)).willReturn(Optional.empty());
            ReportDTO.Request request = new ReportDTO.Request(
                    "제보", "내용", null, "장소", null
            );

            // when & then
            assertThatThrownBy(() -> reportService.create(20L, 999L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("실종 신고를 찾을 수 없습니다.");
        }
    }
}
