package com.xorwns56.report.notification;

import lombok.*;

public class NotificationDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long receiverId;
        private Long senderId;
        private String senderLoginId;   // user-service에서 조회한 sender의 loginId
        private String receiverLoginId; // user-service에서 조회한 receiver의 loginId
        private String postType;
        private Long postId;

        public static Response from(Notification notification, String senderLoginId, String receiverLoginId) {
            return Response.builder()
                    .id(notification.getId())
                    .receiverId(notification.getReceiverId())
                    .senderId(notification.getSenderId())
                    .senderLoginId(senderLoginId)
                    .receiverLoginId(receiverLoginId)
                    .postType(notification.getPostType())
                    .postId(notification.getPostId())
                    .build();
        }
    }

    // WebSocket으로 전송되는 실시간 알림 메시지
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebSocketMessage {
        private Long receiverId;
        private Long senderId;
        private String postType;
        private Long postId;
    }
}
