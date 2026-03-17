package com.xorwns56.report.kafka;

import com.xorwns56.report.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MissingCreatedConsumerTest {

    @InjectMocks
    private MissingCreatedConsumer missingCreatedConsumer;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("missing-created 이벤트 수신 시 전체 유저에게 알림 발송")
    void handleMissingCreated_sendsNotificationToAllUsers() {
        // given
        MissingCreatedEvent event = new MissingCreatedEvent(
                1L, 10L, "http://minio:9000/pet-images/test.jpg",
                "멍멍이", "강아지", "수컷", "골든리트리버", "3살",
                "서울시 강남구", "강아지를 찾습니다", "골든리트리버입니다"
        );

        // when
        missingCreatedConsumer.handleMissingCreated(event);

        // then - sendToAllUsers(userId, "missing", missingId) 호출 확인
        then(notificationService).should().sendToAllUsers(10L, "missing", 1L);
    }

    @Test
    @DisplayName("이미지 없는 이벤트도 정상 처리")
    void handleMissingCreated_withoutImage() {
        // given - imageUrl이 null
        MissingCreatedEvent event = new MissingCreatedEvent(
                2L, 20L, null,
                "야옹이", "고양이", null, null, null,
                null, "고양이 찾습니다", null
        );

        // when
        missingCreatedConsumer.handleMissingCreated(event);

        // then
        then(notificationService).should().sendToAllUsers(20L, "missing", 2L);
    }
}
