package com.xorwns56.report.notification;

import com.xorwns56.report.client.UserServiceClient;
import com.xorwns56.report.websocket.RedisPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RedisPublisher redisPublisher;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .senderId(20L)
                .receiverId(10L)
                .postType("report")
                .postId(100L)
                .build();
    }

    // ========== 알림 전송 테스트 ==========

    @Nested
    @DisplayName("특정 유저에게 알림 전송")
    class SendToUser {

        @Test
        @DisplayName("정상 전송 - DB 저장 + Redis Pub/Sub 발행")
        void sendToUser_success() {
            // when
            notificationService.sendToUser(20L, 10L, "report", 100L);

            // then
            then(notificationRepository).should().save(any(Notification.class));
            then(redisPublisher).should().publish(any(NotificationDTO.WebSocketMessage.class));
        }
    }

    // ========== 전체 유저 알림 전송 테스트 ==========

    @Nested
    @DisplayName("전체 유저에게 알림 전송")
    class SendToAllUsers {

        @Test
        @DisplayName("작성자 본인 제외하고 전체 유저에게 알림 전송")
        void sendToAllUsers_excludesSender() {
            // given
            List<UserServiceClient.UserResponse> allUsers = List.of(
                    new UserServiceClient.UserResponse(10L, "user10", "010-1111-1111"),
                    new UserServiceClient.UserResponse(20L, "user20", "010-2222-2222"),  // 작성자(senderId=20L)
                    new UserServiceClient.UserResponse(30L, "user30", "010-3333-3333")
            );
            given(userServiceClient.getAllUsers()).willReturn(allUsers);

            // when
            notificationService.sendToAllUsers(20L, "missing", 1L);

            // then - 작성자(20L) 제외, userId 10, 30에게만 알림
            then(notificationRepository).should(times(2)).save(any(Notification.class));
            then(redisPublisher).should(times(2)).publish(any(NotificationDTO.WebSocketMessage.class));
        }
    }

    // ========== 내 알림 목록 조회 테스트 ==========

    @Nested
    @DisplayName("내 알림 목록 조회")
    class GetMyNotifications {

        @Test
        @DisplayName("정상 조회")
        void getMyNotifications_success() {
            // given
            Notification notification2 = Notification.builder()
                    .id(2L).senderId(30L).receiverId(10L).postType("missing").postId(5L).build();
            given(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(10L))
                    .willReturn(List.of(testNotification, notification2));

            // when
            List<NotificationDTO.Response> result = notificationService.getMyNotifications(10L);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getPostType()).isEqualTo("report");
        }
    }

    // ========== 알림 삭제 테스트 ==========

    @Nested
    @DisplayName("알림 삭제")
    class Delete {

        @Test
        @DisplayName("정상 삭제 - 수신자 본인")
        void delete_success() {
            // given
            given(notificationRepository.findById(1L)).willReturn(Optional.of(testNotification));

            // when
            notificationService.delete(10L, 1L);

            // then
            then(notificationRepository).should().delete(testNotification);
        }

        @Test
        @DisplayName("다른 사용자가 삭제 시 예외 발생")
        void delete_notReceiver_throwsException() {
            // given
            given(notificationRepository.findById(1L)).willReturn(Optional.of(testNotification));

            // when & then - receiverId=10L인 알림을 userId=99L이 삭제 시도
            assertThatThrownBy(() -> notificationService.delete(99L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("삭제 권한이 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 알림 삭제 시 예외 발생")
        void delete_notFound() {
            // given
            given(notificationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.delete(10L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("알림을 찾을 수 없습니다.");
        }
    }
}
