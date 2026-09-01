package com.xorwns56.search.kafka;

// 실종 신고 삭제 이벤트 (report-service에서 발행)
// Elasticsearch에서 인덱스 삭제
public record MissingDeletedEvent(Long missingId) {
}
