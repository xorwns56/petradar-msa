package com.xorwns56.report.notification;

import com.xorwns56.report.client.UserServiceClient;
import com.xorwns56.report.websocket.RedisPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;
    private final RedisPublisher redisPublisher;

    // 특정 유저에게 알림 전송 (목격 제보 시 실종 신고 작성자에게)
    @Transactional
    public void sendToUser(Long senderId, Long receiverId, String postType, Long postId) {
        Notification notification = Notification.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .postType(postType)
                .postId(postId)
                .build();
        notificationRepository.save(notification);

        // Redis Pub/Sub으로 실시간 WebSocket 알림 발송
        redisPublisher.publish(NotificationDTO.WebSocketMessage.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .postType(postType)
                .postId(postId)
                .build());
    }

    // 전체 유저에게 알림 전송 (실종 신고 등록 시) - 비동기 처리
    @Async
    @Transactional
    public void sendToAllUsers(Long senderId, String postType, Long postId) {
        // user-service에서 전체 유저 목록 조회
        List<UserServiceClient.UserResponse> allUsers = userServiceClient.getAllUsers();

        allUsers.stream()
                .filter(user -> !user.id().equals(senderId)) // 작성자 본인 제외
                .forEach(user -> sendToUser(senderId, user.id(), postType, postId));
    }

    // 내 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationDTO.Response> getMyNotifications(Long userId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationDTO.Response::from)
                .collect(Collectors.toList());
    }

    // 알림 삭제
    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        if (!notification.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        notificationRepository.delete(notification);
    }
}
