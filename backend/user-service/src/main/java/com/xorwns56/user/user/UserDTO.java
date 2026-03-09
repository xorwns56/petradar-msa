package com.xorwns56.user.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

public class UserDTO {

    // 사용자 정보 응답 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String loginId;
        private String hp;

        public static Response from(User user) {
            return Response.builder()
                    .id(user.getId())
                    .loginId(user.getLoginId())
                    .hp(user.getHp())
                    .build();
        }
    }

    // 사용자 정보 수정 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

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
}
