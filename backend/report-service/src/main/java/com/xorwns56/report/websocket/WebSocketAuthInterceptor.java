package com.xorwns56.report.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP CONNECT 시 X-User-Id 헤더에서 userId를 추출하여 Principal로 설정
 * Gateway가 JWT 검증 후 X-User-Id를 전달하므로, WebSocket 세션에서도 이를 활용
 * convertAndSendToUser()가 Principal.getName()으로 유저를 매칭하기 때문에 필수
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Gateway가 전달한 X-User-Id 헤더에서 userId 추출
            String userId = accessor.getFirstNativeHeader("X-User-Id");
            if (userId != null) {
                // STOMP 세션에 Principal 설정 → convertAndSendToUser()에서 매칭
                accessor.setUser(new StompPrincipal(userId));
                log.info("WebSocket 인증 완료: userId={}", userId);
            }
        }

        return message;
    }

    // Principal 구현체 (userId를 name으로 사용)
    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
