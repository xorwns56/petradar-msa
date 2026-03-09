package com.xorwns56.report.kafka;

import com.xorwns56.report.missing.MissingRepository;
import com.xorwns56.report.notification.NotificationRepository;
import com.xorwns56.report.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedConsumer {

    private final MissingRepository missingRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;

    // user-service에서 회원 탈퇴 이벤트 수신 → 관련 데이터 삭제
    @KafkaListener(topics = "user-deleted", groupId = "report-service")
    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        Long userId = event.userId();
        log.info("회원 탈퇴 이벤트 수신 - userId: {}", userId);

        notificationRepository.deleteAllByReceiverIdOrSenderId(userId, userId);
        reportRepository.deleteAllByUserId(userId);
        missingRepository.deleteAllByUserId(userId);
    }
}
