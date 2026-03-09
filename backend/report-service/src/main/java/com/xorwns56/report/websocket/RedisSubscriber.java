package com.xorwns56.report.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xorwns56.report.notification.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Redis notification 채널에서 메시지 수신 → WebSocket으로 해당 유저에게 전송
    public void onMessage(String message) {
        try {
            NotificationDTO.WebSocketMessage wsMessage =
                    objectMapper.readValue(message, NotificationDTO.WebSocketMessage.class);

            // /user/{receiverId}/queue/notification 경로로 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(wsMessage.getReceiverId()),
                    "/queue/notification",
                    wsMessage
            );
        } catch (Exception e) {
            log.error("WebSocket 알림 전송 실패: {}", e.getMessage());
        }
    }
}
