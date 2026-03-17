package com.xorwns56.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(jwtTokenProvider);
    }

    // chain.filter stub을 필요한 테스트에서만 설정하는 헬퍼
    private void stubChainFilter() {
        given(chain.filter(any())).willReturn(Mono.empty());
    }

    // ========== public 경로 테스트 ==========

    @Nested
    @DisplayName("public 경로 (JWT 검증 없이 통과)")
    class PublicPaths {

        @Test
        @DisplayName("/api/auth/login 은 토큰 없이 통과")
        void login_passesWithoutToken() {
            // given
            stubChainFilter();
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/auth/login").build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            verify(chain).filter(any());
        }

        @Test
        @DisplayName("/api/auth/register 는 토큰 없이 통과")
        void register_passesWithoutToken() {
            // given
            stubChainFilter();
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/auth/register").build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            verify(chain).filter(any());
        }

        @Test
        @DisplayName("/api/auth/reissue 는 토큰 없이 통과")
        void reissue_passesWithoutToken() {
            // given
            stubChainFilter();
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/auth/reissue").build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            verify(chain).filter(any());
        }
    }

    // ========== 비회원 허용 경로 테스트 ==========

    @Nested
    @DisplayName("비회원 허용 경로 (토큰 없어도 통과)")
    class OptionalAuthPaths {

        @Test
        @DisplayName("/api/missing 은 토큰 없이 통과")
        void missing_passesWithoutToken() {
            // given
            stubChainFilter();
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/missing").build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            verify(chain).filter(any());
        }

        @Test
        @DisplayName("/api/report 은 토큰 없이 통과")
        void report_passesWithoutToken() {
            // given
            stubChainFilter();
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/report/missing/1").build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            verify(chain).filter(any());
        }

        @Test
        @DisplayName("/api/search 는 토큰 없이 통과")
        void search_passesWithoutToken() {
            // given
            stubChainFilter();
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/search/image").build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            verify(chain).filter(any());
        }
    }

    // ========== 인증 필수 경로 테스트 ==========

    @Nested
    @DisplayName("인증 필수 경로")
    class ProtectedPaths {

        @Test
        @DisplayName("토큰 없으면 401 반환")
        void noToken_returns401() {
            // given - /api/user/me 는 인증 필수
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/user/me").build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Bearer 형식이 아닌 토큰이면 401 반환")
        void invalidBearerFormat_returns401() {
            // given
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/user/me")
                            .header(HttpHeaders.AUTHORIZATION, "Basic some-token")
                            .build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 401 반환")
        void invalidToken_returns401() {
            // given
            given(jwtTokenProvider.validateToken("invalid_token")).willReturn(false);
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/user/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid_token")
                            .build());

            // when
            jwtAuthFilter.filter(exchange, chain).block();

            // then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("유효한 토큰이면 X-User-Id 헤더 설정 후 통과")
        void validToken_setsXUserIdHeader() {
            // given
            stubChainFilter();
            given(jwtTokenProvider.validateToken("valid_token")).willReturn(true);
            given(jwtTokenProvider.getUserId("valid_token")).willReturn("42");
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/user/me")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
                            .build());

            // when
            ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            jwtAuthFilter.filter(exchange, chain).block();

            // then - chain.filter에 전달된 exchange에서 X-User-Id 확인
            verify(chain).filter(captor.capture());
            String xUserId = captor.getValue().getRequest().getHeaders().getFirst("X-User-Id");
            assertThat(xUserId).isEqualTo("42");
        }
    }

    // ========== X-User-Id 스푸핑 방지 테스트 ==========

    @Nested
    @DisplayName("X-User-Id 스푸핑 방지")
    class SpoofingPrevention {

        @Test
        @DisplayName("외부에서 주입한 X-User-Id 헤더를 제거")
        void removesExternalXUserId_onPublicPath() {
            // given - 외부에서 X-User-Id를 직접 설정한 요청 (public 경로)
            stubChainFilter();
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/auth/login")
                            .header("X-User-Id", "999")
                            .build());

            // when
            ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            jwtAuthFilter.filter(exchange, chain).block();

            // then - X-User-Id가 제거되었는지 확인
            verify(chain).filter(captor.capture());
            String xUserId = captor.getValue().getRequest().getHeaders().getFirst("X-User-Id");
            assertThat(xUserId).isNull();
        }

        @Test
        @DisplayName("유효한 토큰이 있으면 토큰의 userId로 X-User-Id 교체")
        void replacesExternalXUserId_withTokenUserId() {
            // given - 외부에서 X-User-Id=999를 설정했지만, 토큰의 userId=42
            stubChainFilter();
            given(jwtTokenProvider.validateToken("valid_token")).willReturn(true);
            given(jwtTokenProvider.getUserId("valid_token")).willReturn("42");
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/user/me")
                            .header("X-User-Id", "999")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
                            .build());

            // when
            ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            jwtAuthFilter.filter(exchange, chain).block();

            // then - 토큰의 userId(42)가 설정됨, 외부 주입값(999) 아님
            verify(chain).filter(captor.capture());
            String xUserId = captor.getValue().getRequest().getHeaders().getFirst("X-User-Id");
            assertThat(xUserId).isEqualTo("42");
        }
    }

    // ========== 필터 순서 테스트 ==========

    @Test
    @DisplayName("필터 우선순위가 -1 (가장 먼저 실행)")
    void getOrder_returnsNegativeOne() {
        assertThat(jwtAuthFilter.getOrder()).isEqualTo(-1);
    }
}
