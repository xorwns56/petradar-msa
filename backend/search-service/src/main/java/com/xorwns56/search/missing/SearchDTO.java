package com.xorwns56.search.missing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 검색 API 응답 DTO
public class SearchDTO {

    // 검색 결과 단건
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Result {
        private Long missingId;        // 실종 신고 ID
        private Long userId;           // 작성자 ID (프론트 본인글 판별용)
        private String title;          // 제목
        private String petName;        // 반려동물 이름
        private String petType;        // 종류 (dog, cat, etc)
        private String petGender;      // 성별 (M, F)
        private String petBreed;       // 품종
        private String petAge;         // 나이(년생)
        private String petMissingDate; // 실종일자
        private String content;        // 본문 (상세 모달 표시용)
        private String imageUrl;       // 대표 이미지 URL
        private float score;           // Elasticsearch 검색 점수

        public static Result from(MissingDocument doc, float score) {
            return Result.builder()
                    .missingId(doc.getId())
                    .userId(doc.getUserId())
                    .title(doc.getTitle())
                    .petName(doc.getPetName())
                    .petType(doc.getPetType())
                    .petGender(doc.getPetGender())
                    .petBreed(doc.getPetBreed())
                    .petAge(doc.getPetAge())
                    .petMissingDate(doc.getPetMissingDate())
                    .content(doc.getContent())
                    .imageUrl(doc.getImageUrl())
                    .score(score)
                    .build();
        }
    }

    // 검색 응답 (결과 목록 + 전체 건수)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private List<Result> results;
        private long totalHits;
    }
}
