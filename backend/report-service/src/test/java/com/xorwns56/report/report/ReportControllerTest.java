package com.xorwns56.report.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;

    private final ReportDTO.Response sampleResponse = ReportDTO.Response.builder()
            .id(100L)
            .missingId(1L)
            .userId(20L)
            .title("여기서 봤어요")
            .content("공원에서 비슷한 강아지를 봤습니다")
            .petReportPlace("서울시 강남구")
            .petReportPoint(new ReportDTO.Point(37.5, 127.04))
            .build();

    // ========== 상세 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/report/{reportId}")
    class GetDetail {

        @Test
        @DisplayName("정상 조회")
        void getDetail_success() throws Exception {
            // given
            given(reportService.getDetail(100L)).willReturn(sampleResponse);

            // when & then
            mockMvc.perform(get("/api/report/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.title").value("여기서 봤어요"))
                    .andExpect(jsonPath("$.missingId").value(1));
        }
    }

    // ========== 목록 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/report/missing/{missingId}")
    class GetList {

        @Test
        @DisplayName("실종 신고별 목격 제보 목록 조회")
        void getList_success() throws Exception {
            // given
            Page<ReportDTO.Response> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 10), 1);
            given(reportService.getListByMissingId(eq(1L), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/report/missing/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(100))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ========== 등록 API 테스트 ==========

    @Nested
    @DisplayName("POST /api/report/missing/{missingId}")
    class Create {

        @Test
        @DisplayName("회원 제보 등록")
        void create_withUser() throws Exception {
            // given
            ReportDTO.Request request = new ReportDTO.Request(
                    "여기서 봤어요", "공원에서 봤습니다", null, "서울시 강남구",
                    new ReportDTO.Point(37.5, 127.04)
            );
            willDoNothing().given(reportService).create(eq(20L), eq(1L), any());

            // when & then
            mockMvc.perform(post("/api/report/missing/1")
                            .header("X-User-Id", 20L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("비회원 제보 등록 - X-User-Id 헤더 없음")
        void create_anonymous() throws Exception {
            // given
            ReportDTO.Request request = new ReportDTO.Request(
                    "제보합니다", "내용", null, "장소", null
            );
            willDoNothing().given(reportService).create(isNull(), eq(1L), any());

            // when & then
            mockMvc.perform(post("/api/report/missing/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("존재하지 않는 실종 신고에 제보 시 400 반환")
        void create_missingNotFound() throws Exception {
            // given
            ReportDTO.Request request = new ReportDTO.Request(
                    "제보", "내용", null, "장소", null
            );
            willThrow(new IllegalArgumentException("실종 신고를 찾을 수 없습니다. id: 999"))
                    .given(reportService).create(eq(20L), eq(999L), any());

            // when & then
            mockMvc.perform(post("/api/report/missing/999")
                            .header("X-User-Id", 20L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("실종 신고를 찾을 수 없습니다. id: 999"));
        }
    }
}
