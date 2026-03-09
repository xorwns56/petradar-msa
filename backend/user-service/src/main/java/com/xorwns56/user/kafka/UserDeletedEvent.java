package com.xorwns56.user.kafka;

// 회원 탈퇴 이벤트 - report-service에서 관련 데이터 삭제 처리
public record UserDeletedEvent(Long userId) {
}
