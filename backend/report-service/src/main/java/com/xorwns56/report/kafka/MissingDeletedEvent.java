package com.xorwns56.report.kafka;

// 실종 신고 삭제 이벤트 - search-service에서 pgvector 임베딩 삭제
public record MissingDeletedEvent(Long missingId) {
}
