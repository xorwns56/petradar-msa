package com.xorwns56.report.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    // ========== 내 알림 목록 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/notification/me")
    class GetMyNotifications {

        @Test
        @DisplayName("정상 조회")
        void getMyNotifications_success() throws Exception {
            // given
            List<NotificationDTO.Response> notifications = List.of(
                    NotificationDTO.Response.builder()
                            .id(1L).receiverId(10L).senderId(20L)
                            .postType("report").postId(100L).build(),
                    NotificationDTO.Response.builder()
                            .id(2L).receiverId(10L).senderId(30L)
                            .postType("missing").postId(5L).build()
            );
            given(notificationService.getMyNotifications(10L)).willReturn(notifications);

            // when & then
            mockMvc.perform(get("/api/notification/me")
                            .header("X-User-Id", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].postType").value("report"))
                    .andExpect(jsonPath("$[1].postType").value("missing"));
        }
    }

    // ========== 알림 삭제 API 테스트 ==========

    @Nested
    @DisplayName("DELETE /api/notification/{notificationId}")
    class Delete {

        @Test
        @DisplayName("정상 삭제")
        void delete_success() throws Exception {
            // given
            willDoNothing().given(notificationService).delete(10L, 1L);

            // when & then
            mockMvc.perform(delete("/api/notification/1")
                            .header("X-User-Id", 10L))
                    .andExpect(status().isOk());

            then(notificationService).should().delete(10L, 1L);
        }

        @Test
        @DisplayName("권한 없는 삭제 시 400 반환")
        void delete_forbidden_returns400() throws Exception {
            // given
            willThrow(new IllegalArgumentException("삭제 권한이 없습니다."))
                    .given(notificationService).delete(99L, 1L);

            // when & then
            mockMvc.perform(delete("/api/notification/1")
                            .header("X-User-Id", 99L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("삭제 권한이 없습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 알림 삭제 시 400 반환")
        void delete_notFound_returns400() throws Exception {
            // given
            willThrow(new IllegalArgumentException("알림을 찾을 수 없습니다."))
                    .given(notificationService).delete(10L, 999L);

            // when & then
            mockMvc.perform(delete("/api/notification/999")
                            .header("X-User-Id", 10L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."));
        }
    }
}
