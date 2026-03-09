package com.xorwns56.report.missing;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class MissingDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String petName;
        private String petType;
        private String petGender;
        private String petBreed;
        private String petAge;
        private String petMissingDate;
        private String petMissingPlace;
        private Point petMissingPoint;
        private String petImage;
        private String title;
        private String content;

        public static Response from(Missing missing) {
            Point point = null;
            if (missing.getLatitude() != null && missing.getLongitude() != null) {
                point = new Point(missing.getLatitude(), missing.getLongitude());
            }
            return Response.builder()
                    .id(missing.getId())
                    .userId(missing.getUserId())
                    .petName(missing.getPetName())
                    .petType(missing.getPetType())
                    .petGender(missing.getPetGender())
                    .petBreed(missing.getPetBreed())
                    .petAge(missing.getPetAge())
                    .petMissingDate(missing.getPetMissingDate())
                    .petMissingPlace(missing.getPetMissingPlace())
                    .petMissingPoint(point)
                    .petImage(missing.getPetImage())
                    .title(missing.getTitle())
                    .content(missing.getContent())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "반려동물 이름을 입력해주세요.")
        private String petName;
        private String petType;
        private String petGender;
        private String petBreed;
        private String petAge;
        private String petMissingDate;
        private String petMissingPlace;
        private Point petMissingPoint;
        private String petImage;
        @NotBlank(message = "제목을 입력해주세요.")
        private String title;
        private String content;
    }

    // 좌표 (위도/경도)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private Double lat;
        private Double lng;
    }
}
