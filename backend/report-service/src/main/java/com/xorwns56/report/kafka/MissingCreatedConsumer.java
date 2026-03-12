package com.xorwns56.report.kafka;

import com.xorwns56.report.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissingCreatedConsumer {

    private final NotificationService notificationService;

    // missing-created 이벤트 수신 → 전체 유저에게 알림 발송
    // search-service와 다른 group_id를 써서 동일 메시지를 독립적으로 수신
    // Kafka가 메시지 보관 → user-service 장애 시에도 eventually 처리 보장
    @KafkaListener(topics = "missing-created", groupId = "report-service-notification")
    public void handleMissingCreated(MissingCreatedEvent event) {
        log.info("missing-created 이벤트 수신 - missingId: {}", event.missingId());
        notificationService.sendToAllUsers(event.userId(), "missing", event.missingId());
    }
}
