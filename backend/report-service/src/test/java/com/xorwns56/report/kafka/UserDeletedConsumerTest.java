package com.xorwns56.report.kafka;

import com.xorwns56.report.missing.MissingRepository;
import com.xorwns56.report.notification.NotificationRepository;
import com.xorwns56.report.report.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UserDeletedConsumerTest {

    @InjectMocks
    private UserDeletedConsumer userDeletedConsumer;

    @Mock
    private MissingRepository missingRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("user-deleted 이벤트 수신 시 notification → report → missing 순서로 삭제")
    void handleUserDeleted_deletesInCorrectOrder() {
        // given
        UserDeletedEvent event = new UserDeletedEvent(10L);

        // when
        userDeletedConsumer.handleUserDeleted(event);

        // then - 외래키 제약 때문에 삭제 순서가 중요 (notification → report → missing)
        InOrder inOrder = inOrder(notificationRepository, reportRepository, missingRepository);
        inOrder.verify(notificationRepository).deleteAllByReceiverIdOrSenderId(10L, 10L);
        inOrder.verify(reportRepository).deleteAllByUserId(10L);
        inOrder.verify(missingRepository).deleteAllByUserId(10L);
    }

    @Test
    @DisplayName("다른 userId로도 정상 처리")
    void handleUserDeleted_differentUserId() {
        // given
        UserDeletedEvent event = new UserDeletedEvent(999L);

        // when
        userDeletedConsumer.handleUserDeleted(event);

        // then
        then(notificationRepository).should().deleteAllByReceiverIdOrSenderId(999L, 999L);
        then(reportRepository).should().deleteAllByUserId(999L);
        then(missingRepository).should().deleteAllByUserId(999L);
    }
}
