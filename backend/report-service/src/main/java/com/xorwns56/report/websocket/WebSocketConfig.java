package com.xorwns56.report.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 prefix - /queue/notification
        config.enableSimpleBroker("/queue");
        // 클라이언트가 서버로 메시지 보낼 때 prefix
        config.setApplicationDestinationPrefixes("/app");
        // convertAndSendToUser() 사용 시 유저 구분 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 (/api/ws - Gateway 라우팅과 일치)
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS fallback (WebSocket 미지원 브라우저 대응)
    }

    // STOMP CONNECT 시 X-User-Id → Principal 설정 (인바운드 채널 인터셉터)
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
