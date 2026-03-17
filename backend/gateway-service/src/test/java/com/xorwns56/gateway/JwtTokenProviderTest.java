package com.xorwns56.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // 테스트용 시크릿 (HMAC-SHA256 최소 32바이트)
    private static final String TEST_SECRET = "test-secret-key-must-be-at-least-32-bytes-long!!";

    private JwtTokenProvider jwtTokenProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET);
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // 테스트용 Access Token 생성 헬퍼
    private String createToken(String userId, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    // ========== 토큰 유효성 검증 테스트 ==========

    @Nested
    @DisplayName("토큰 유효성 검증")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validateToken_validToken_returnsTrue() {
            // given - 10분 유효 토큰
            String token = createToken("1", 600_000);

            // when & then
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰 검증 실패")
        void validateToken_expiredToken_returnsFalse() {
            // given - 이미 만료된 토큰
            String token = createToken("1", -1000);

            // when & then
            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰 검증 실패")
        void validateToken_malformedToken_returnsFalse() {
            // when & then
            assertThat(jwtTokenProvider.validateToken("invalid.token.format")).isFalse();
        }

        @Test
        @DisplayName("다른 시크릿으로 서명된 토큰 검증 실패")
        void validateToken_wrongSecret_returnsFalse() {
            // given - 다른 시크릿 키로 서명된 토큰
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    "wrong-secret-key-must-be-at-least-32-bytes-long!".getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject("1")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 600_000))
                    .signWith(wrongKey)
                    .compact();

            // when & then
            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }
    }

    // ========== userId 추출 테스트 ==========

    @Nested
    @DisplayName("userId 추출")
    class GetUserId {

        @Test
        @DisplayName("토큰에서 userId 정상 추출")
        void getUserId_success() {
            // given
            String token = createToken("42", 600_000);

            // when
            String userId = jwtTokenProvider.getUserId(token);

            // then
            assertThat(userId).isEqualTo("42");
        }

        @Test
        @DisplayName("다른 userId로 생성된 토큰에서 정확한 값 추출")
        void getUserId_differentUsers() {
            // given
            String token1 = createToken("1", 600_000);
            String token2 = createToken("999", 600_000);

            // when & then
            assertThat(jwtTokenProvider.getUserId(token1)).isEqualTo("1");
            assertThat(jwtTokenProvider.getUserId(token2)).isEqualTo("999");
        }
    }
}
