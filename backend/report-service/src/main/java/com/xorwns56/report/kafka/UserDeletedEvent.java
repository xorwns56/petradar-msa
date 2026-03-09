package com.xorwns56.report.kafka;

// user-service에서 발행하는 회원 탈퇴 이벤트
public record UserDeletedEvent(Long userId) {
}
