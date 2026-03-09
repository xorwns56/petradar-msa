package com.xorwns56.report.notification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MSA: User 엔티티 직접 참조 불가 → userId만 저장
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "sender_id") // 비회원 제보 시 null 허용
    private Long senderId;

    @Column(name = "post_type", nullable = false)
    private String postType; // "missing" or "report"

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
