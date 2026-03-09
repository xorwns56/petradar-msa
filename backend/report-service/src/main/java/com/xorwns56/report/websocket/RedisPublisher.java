package com.xorwns56.report.websocket;

import com.xorwns56.report.notification.NotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis notification 채널로 메시지 발행
    // 모든 인스턴스의 RedisSubscriber가 수신하여 해당 유저에게 WebSocket 전송
    public void publish(NotificationDTO.WebSocketMessage message) {
        redisTemplate.convertAndSend(RedisConfig.NOTIFICATION_CHANNEL, message);
    }
}
