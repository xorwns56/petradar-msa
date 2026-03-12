package com.xorwns56.report.kafka;

// 실종 신고 등록 이벤트 - search-service에서 이미지 임베딩 처리
public record MissingCreatedEvent(Long missingId, String imageUrl) {
}
