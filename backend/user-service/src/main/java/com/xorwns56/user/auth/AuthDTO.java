package com.xorwns56.user.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

public class AuthDTO {

    // 로그인 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String pw;
    }

    // 회원가입 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$",
                message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
        )
        private String pw;

        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        @Pattern(
                regexp = "^01[0-9]{1}-\\d{3,4}-\\d{4}$",
                message = "유효한 휴대폰 번호 형식이 아닙니다."
        )
        private String hp;
    }

    // 토큰 응답 DTO (refreshToken은 컨트롤러에서 HttpOnly 쿠키로 설정)
    @Getter
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
    }
}
