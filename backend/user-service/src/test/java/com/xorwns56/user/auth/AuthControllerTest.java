package com.xorwns56.user.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xorwns56.user.security.SecurityConfig;
import com.xorwns56.user.user.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // ========== 로그인 API 테스트 ==========

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginApi {

        @Test
        @DisplayName("정상 로그인 - Access Token 응답 + Refresh Token 쿠키 설정")
        void login_success() throws Exception {
            // given
            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest("testuser", "Password1!");
            AuthDTO.TokenResponse tokenResponse = new AuthDTO.TokenResponse("access_token", "refresh_token");
            given(userService.login(any(AuthDTO.LoginRequest.class))).willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access_token"))
                    .andExpect(cookie().value("refreshToken", "refresh_token"))
                    .andExpect(cookie().httpOnly("refreshToken", true));
        }

        @Test
        @DisplayName("로그인 실패 - 401 반환")
        void login_failure_returns401() throws Exception {
            // given
            AuthDTO.LoginRequest request = new AuthDTO.LoginRequest("testuser", "wrongpw");
            given(userService.login(any(AuthDTO.LoginRequest.class)))
                    .willThrow(new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("Validation 실패 - loginId 누락 시 400")
        void login_validation_missingLoginId() throws Exception {
            // given - loginId 빈 값
            String body = "{\"loginId\":\"\",\"pw\":\"password\"}";

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== 회원가입 API 테스트 ==========

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterApi {

        @Test
        @DisplayName("정상 회원가입")
        void register_success() throws Exception {
            // given
            AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest("newuser", "Password1!", "010-1111-2222");
            willDoNothing().given(userService).register(any(AuthDTO.RegisterRequest.class));

            // when & then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("중복 아이디 회원가입 시 400 반환")
        void register_duplicate_returns400() throws Exception {
            // given
            AuthDTO.RegisterRequest request = new AuthDTO.RegisterRequest("existing", "Password1!", "010-1111-2222");
            willThrow(new IllegalArgumentException("이미 사용 중인 아이디입니다."))
                    .given(userService).register(any(AuthDTO.RegisterRequest.class));

            // when & then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
        }
    }

    // ========== 아이디 중복 확인 API 테스트 ==========

    @Nested
    @DisplayName("GET /api/auth/check-exist")
    class CheckExistApi {

        @Test
        @DisplayName("존재하는 아이디 - true 반환")
        void checkExist_exists() throws Exception {
            // given
            given(userService.existsByLoginId("testuser")).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/auth/check-exist")
                            .param("loginId", "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("존재하지 않는 아이디 - false 반환")
        void checkExist_notExists() throws Exception {
            // given
            given(userService.existsByLoginId("newuser")).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/auth/check-exist")
                            .param("loginId", "newuser"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));
        }
    }

    // ========== 토큰 재발급 API 테스트 ==========

    @Nested
    @DisplayName("POST /api/auth/reissue")
    class ReissueApi {

        @Test
        @DisplayName("정상 재발급 - 쿠키의 Refresh Token으로 새 Access Token 발급")
        void reissue_success() throws Exception {
            // given
            given(userService.reissue("valid_refresh_token")).willReturn("new_access_token");

            // when & then
            mockMvc.perform(post("/api/auth/reissue")
                            .cookie(new Cookie("refreshToken", "valid_refresh_token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new_access_token"));
        }

        @Test
        @DisplayName("Refresh Token 쿠키 없을 시 401 반환")
        void reissue_noCookie_returns401() throws Exception {
            // when & then
            mockMvc.perform(post("/api/auth/reissue"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Refresh Token이 없습니다."));
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token 시 401 반환")
        void reissue_invalidToken_returns401() throws Exception {
            // given
            given(userService.reissue("invalid"))
                    .willThrow(new IllegalArgumentException("유효하지 않은 Refresh Token입니다."));

            // when & then
            mockMvc.perform(post("/api/auth/reissue")
                            .cookie(new Cookie("refreshToken", "invalid")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("유효하지 않은 Refresh Token입니다."));
        }
    }

    // ========== 로그아웃 API 테스트 ==========

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutApi {

        @Test
        @DisplayName("정상 로그아웃 - Refresh Token 쿠키 만료")
        void logout_success() throws Exception {
            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .cookie(new Cookie("refreshToken", "refresh_token")))
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("refreshToken", 0));

            then(userService).should().logout("refresh_token");
        }

        @Test
        @DisplayName("쿠키 없이 로그아웃해도 200 반환")
        void logout_noCookie_stillOk() throws Exception {
            // when & then
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk());

            then(userService).should(never()).logout(anyString());
        }
    }
}
