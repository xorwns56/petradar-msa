package com.xorwns56.user.auth;

import com.xorwns56.user.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@Tag(name = "Auth", description = "인증 API (로그인/회원가입/토큰 재발급)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    // 로그인 - Access Token 응답 body, Refresh Token HttpOnly 쿠키 설정
    @Operation(summary = "로그인", description = "Access Token을 응답 body로, Refresh Token을 HttpOnly 쿠키로 설정합니다.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDTO.LoginRequest request,
                                   HttpServletResponse response) {
        try {
            AuthDTO.TokenResponse tokenResponse = userService.login(request);
            setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
            return ResponseEntity.ok(Map.of("accessToken", tokenResponse.getAccessToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    // 회원가입
    @Operation(summary = "회원가입")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        try {
            userService.register(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 아이디 중복 확인
    @Operation(summary = "아이디 중복 확인")
    @GetMapping("/check-exist")
    public ResponseEntity<Boolean> checkExist(@RequestParam String loginId) {
        return ResponseEntity.ok(userService.existsByLoginId(loginId));
    }

    // Access Token 재발급 - 쿠키의 Refresh Token 검증 후 새 Access Token 발급
    @Operation(summary = "Access Token 재발급", description = "쿠키의 Refresh Token을 검증하고 새 Access Token을 발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Refresh Token이 없습니다."));
        }
        try {
            String newAccessToken = userService.reissue(refreshToken);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    // 로그아웃 - Redis Refresh Token 삭제 + 쿠키 만료
    @Operation(summary = "로그아웃", description = "Redis에서 Refresh Token을 삭제하고 쿠키를 만료시킵니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            userService.logout(refreshToken);
        }
        expireRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    // Refresh Token을 HttpOnly 쿠키로 설정
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);   // XSS 방지
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24); // 24시간
        // cookie.setSecure(true);  // HTTPS 환경에서만 전송 (프로덕션 시 활성화)
        response.addCookie(cookie);
    }

    // 쿠키에서 Refresh Token 추출
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // Refresh Token 쿠키 만료 처리
    private void expireRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);
    }
}
