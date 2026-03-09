package com.xorwns56.report.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    // 회원 탈퇴 시 해당 유저의 알림 전체 삭제
    void deleteAllByReceiverIdOrSenderId(Long receiverId, Long senderId);
}
