package com.xorwns56.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    // JWT 검증 없이 통과시킬 경로 (public 엔드포인트)
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/check-exist",
            "/api/auth/reissue"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 외부에서 X-User-Id 헤더를 직접 설정하는 스푸핑 방지
        // Gateway에서만 X-User-Id를 설정하도록 외부 요청의 헤더를 제거
        exchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(headers -> headers.remove("X-User-Id"))
                        .build())
                .build();

        // public 경로는 JWT 검증 없이 통과
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // WebSocket 연결: 쿼리 파라미터에서 토큰 추출 (SockJS는 Authorization 헤더 사용 불가)
        if (path.startsWith("/api/ws")) {
            String tokenParam = exchange.getRequest().getQueryParams().getFirst("token");
            if (tokenParam != null && tokenParam.startsWith("Bearer ")) {
                String wsToken = tokenParam.substring(7);
                if (jwtTokenProvider.validateToken(wsToken)) {
                    String userId = jwtTokenProvider.getUserId(wsToken);
                    ServerWebExchange wsExchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Id", userId)
                                    .build())
                            .build();
                    return chain.filter(wsExchange);
                }
            }
            return unauthorized(exchange);
        }

        // Authorization 헤더에서 토큰 추출
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 토큰 없거나 Bearer 형식이 아니면 일부 경로는 통과 (비회원 허용)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (isOptionalAuthPath(path)) {
                return chain.filter(exchange); // 비회원도 가능한 경로
            }
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        // 토큰 유효성 검증 실패 시, optional 경로는 비회원으로 통과
        if (!jwtTokenProvider.validateToken(token)) {
            if (isOptionalAuthPath(path)) {
                return chain.filter(exchange);
            }
            return unauthorized(exchange);
        }

        // userId 추출 후 X-User-Id 헤더로 downstream 서비스에 전달
        String userId = jwtTokenProvider.getUserId(token);
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .build())
                .build();

        return chain.filter(modifiedExchange);
    }

    // 비회원도 접근 가능한 경로 (목격 제보, 실종 신고 조회 등)
    // /me 등 로그인 필수 경로는 제외해야 토큰 만료 시 401 → 자동 재발급 흐름이 정상 동작
    private boolean isOptionalAuthPath(String path) {
        // 로그인 필수 경로는 optional에서 제외
        if (path.equals("/api/missing/me") || path.equals("/api/notification/me")) {
            return false;
        }
        return path.startsWith("/api/missing") ||
               path.startsWith("/api/report") ||
               path.startsWith("/api/search");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // 필터 우선순위 (낮을수록 먼저 실행)
    @Override
    public int getOrder() {
        return -1;
    }
}
