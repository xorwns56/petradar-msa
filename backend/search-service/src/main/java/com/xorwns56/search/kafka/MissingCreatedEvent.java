package com.xorwns56.search.kafka;

// 실종 신고 등록 이벤트 (report-service에서 발행)
// Elasticsearch에 인덱싱할 데이터 수신
public record MissingCreatedEvent(
        Long missingId,
        Long userId,
        String imageUrl,
        String petName,
        String petType,
        String petGender,
        String petBreed,
        String petAge,
        String petMissingPlace,
        String title,
        String content,
        String petMissingDate   // 실종일자 (검색 결과 카드 표시용)
) {}
