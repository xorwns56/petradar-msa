package com.xorwns56.user.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xorwns56.user.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // ========== 내 정보 조회 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/user/me")
    class GetMe {

        @Test
        @DisplayName("정상 조회 - X-User-Id 헤더로 사용자 정보 반환")
        void getMe_success() throws Exception {
            // given
            UserDTO.Response response = UserDTO.Response.builder()
                    .id(1L).loginId("testuser").hp("010-1234-5678").build();
            given(userService.findById(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/user/me")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.loginId").value("testuser"))
                    .andExpect(jsonPath("$.hp").value("010-1234-5678"));
        }

        @Test
        @DisplayName("X-User-Id 헤더 없으면 400 반환")
        void getMe_noHeader_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/user/me"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== 내 정보 수정 API 테스트 ==========

    @Nested
    @DisplayName("PATCH /api/user/me")
    class UpdateMe {

        @Test
        @DisplayName("정상 수정")
        void updateMe_success() throws Exception {
            // given
            UserDTO.UpdateRequest request = new UserDTO.UpdateRequest("NewPass1!", "010-9999-8888");
            willDoNothing().given(userService).update(eq(1L), any(UserDTO.UpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/user/me")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 수정 시 400 반환")
        void updateMe_notFound_returns400() throws Exception {
            // given
            UserDTO.UpdateRequest request = new UserDTO.UpdateRequest("NewPass1!", "010-9999-8888");
            willThrow(new IllegalArgumentException("사용자를 찾을 수 없습니다."))
                    .given(userService).update(eq(999L), any(UserDTO.UpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/user/me")
                            .header("X-User-Id", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
        }
    }

    // ========== 회원 탈퇴 API 테스트 ==========

    @Nested
    @DisplayName("DELETE /api/user/me")
    class DeleteMe {

        @Test
        @DisplayName("정상 탈퇴")
        void deleteMe_success() throws Exception {
            // given
            willDoNothing().given(userService).delete(1L);

            // when & then
            mockMvc.perform(delete("/api/user/me")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk());

            then(userService).should().delete(1L);
        }
    }

    // ========== 단건 사용자 조회 API (internal) 테스트 ==========

    @Nested
    @DisplayName("GET /api/user/{id}")
    class GetUser {

        @Test
        @DisplayName("정상 조회")
        void getUser_success() throws Exception {
            // given
            UserDTO.Response response = UserDTO.Response.builder()
                    .id(1L).loginId("testuser").hp("010-1234-5678").build();
            given(userService.findById(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.loginId").value("testuser"));
        }
    }

    // ========== 전체 사용자 조회 API (internal) 테스트 ==========

    @Nested
    @DisplayName("GET /api/user/all")
    class GetAllUsers {

        @Test
        @DisplayName("전체 사용자 목록 반환")
        void getAllUsers_success() throws Exception {
            // given
            List<UserDTO.Response> users = List.of(
                    UserDTO.Response.builder().id(1L).loginId("user1").hp("010-1111-1111").build(),
                    UserDTO.Response.builder().id(2L).loginId("user2").hp("010-2222-2222").build()
            );
            given(userService.findAll()).willReturn(users);

            // when & then
            mockMvc.perform(get("/api/user/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].loginId").value("user1"))
                    .andExpect(jsonPath("$[1].loginId").value("user2"));
        }
    }
}
