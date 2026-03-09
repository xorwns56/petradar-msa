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
        private String postType;
        private Long postId;

        public static Response from(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .receiverId(notification.getReceiverId())
                    .senderId(notification.getSenderId())
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
