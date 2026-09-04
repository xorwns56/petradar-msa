package com.xorwns56.report.kafka;

// 실종 신고 등록 이벤트
// search-service: Elasticsearch 인덱싱
// report-service(MissingCreatedConsumer): 전체 유저 알림 발송
public record MissingCreatedEvent(
        Long missingId,
        Long userId,
        String imageUrl,     // MinIO URL (없으면 null)
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
