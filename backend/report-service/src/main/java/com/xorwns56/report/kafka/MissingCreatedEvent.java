package com.xorwns56.report.kafka;

// 실종 신고 등록 이벤트
// search-service: 이미지/텍스트 벡터화 후 pgvector 저장
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
        String content
) {}
