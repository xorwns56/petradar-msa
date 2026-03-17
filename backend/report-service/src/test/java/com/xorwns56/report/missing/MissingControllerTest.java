package com.xorwns56.report.missing;

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

@WebMvcTest(MissingController.class)
class MissingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MissingService missingService;

    private final MissingDTO.Response sampleResponse = MissingDTO.Response.builder()
            .id(1L)
            .userId(10L)
            .petName("멍멍이")
            .petType("강아지")
            .title("강아지를 찾습니다")
            .content("골든리트리버입니다")
            .petMissingPoint(new MissingDTO.Point(37.5172, 127.0473))
            .build();

    // ========== 목록 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/missing")
    class GetList {

        @Test
        @DisplayName("페이지네이션 목록 조회")
        void getList_success() throws Exception {
            // given
            Page<MissingDTO.Response> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 10), 1);
            given(missingService.getList(eq(""), eq("latest"), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/missing"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].petName").value("멍멍이"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("검색어로 조회")
        void getList_withSearch() throws Exception {
            // given
            Page<MissingDTO.Response> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 10), 1);
            given(missingService.getList(eq("강아지"), eq("latest"), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/missing")
                            .param("search", "강아지"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("강아지를 찾습니다"));
        }
    }

    // ========== 전체 목록 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/missing/all")
    class GetAll {

        @Test
        @DisplayName("지도 마커용 전체 목록 조회")
        void getAll_success() throws Exception {
            // given
            given(missingService.getList("", "latest")).willReturn(List.of(sampleResponse));

            // when & then
            mockMvc.perform(get("/api/missing/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    // ========== ID 목록 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/missing/batch")
    class GetByIds {

        @Test
        @DisplayName("ID 목록으로 조회")
        void getByIds_success() throws Exception {
            // given
            given(missingService.getByIds(List.of(1L, 2L)))
                    .willReturn(List.of(sampleResponse));

            // when & then
            mockMvc.perform(get("/api/missing/batch")
                            .param("ids", "1", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    // ========== 내 목록 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/missing/me")
    class GetMyList {

        @Test
        @DisplayName("X-User-Id 헤더로 내 목록 조회")
        void getMyList_success() throws Exception {
            // given
            given(missingService.getMyList(10L)).willReturn(List.of(sampleResponse));

            // when & then
            mockMvc.perform(get("/api/missing/me")
                            .header("X-User-Id", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(10));
        }
    }

    // ========== 상세 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/missing/{id}")
    class GetDetail {

        @Test
        @DisplayName("정상 상세 조회")
        void getDetail_success() throws Exception {
            // given
            given(missingService.getDetail(1L)).willReturn(sampleResponse);

            // when & then
            mockMvc.perform(get("/api/missing/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.petName").value("멍멍이"));
        }
    }

    // ========== 수정 API 테스트 ==========

    @Nested
    @DisplayName("PATCH /api/missing/{id}")
    class Update {

        @Test
        @DisplayName("정상 수정")
        void update_success() throws Exception {
            // given
            MissingDTO.Request request = new MissingDTO.Request(
                    "멍멍이", "강아지", "수컷", "골든리트리버", "3살",
                    "2025-01-01", "서울시 강남구", null, null,
                    "수정된 제목", "수정된 내용"
            );
            willDoNothing().given(missingService).update(eq(1L), eq(10L), any());

            // when & then
            mockMvc.perform(patch("/api/missing/1")
                            .header("X-User-Id", 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("권한 없는 수정 시 400 반환")
        void update_forbidden_returns400() throws Exception {
            // given
            MissingDTO.Request request = new MissingDTO.Request(
                    "멍멍이", null, null, null, null,
                    null, null, null, null, "제목", null
            );
            willThrow(new IllegalArgumentException("수정 권한이 없습니다."))
                    .given(missingService).update(eq(1L), eq(99L), any());

            // when & then
            mockMvc.perform(patch("/api/missing/1")
                            .header("X-User-Id", 99L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("수정 권한이 없습니다."));
        }
    }

    // ========== 삭제 API 테스트 ==========

    @Nested
    @DisplayName("DELETE /api/missing/{id}")
    class Delete {

        @Test
        @DisplayName("정상 삭제")
        void delete_success() throws Exception {
            // given
            willDoNothing().given(missingService).delete(1L, 10L);

            // when & then
            mockMvc.perform(delete("/api/missing/1")
                            .header("X-User-Id", 10L))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("권한 없는 삭제 시 400 반환")
        void delete_forbidden_returns400() throws Exception {
            // given
            willThrow(new IllegalArgumentException("삭제 권한이 없습니다."))
                    .given(missingService).delete(1L, 99L);

            // when & then
            mockMvc.perform(delete("/api/missing/1")
                            .header("X-User-Id", 99L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("삭제 권한이 없습니다."));
        }
    }
}
