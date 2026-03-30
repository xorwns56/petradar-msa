package com.xorwns56.report.report;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class ReportDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long missingId;
        private Long userId;
        private String title;
        private String content;
        private String petImage;
        private String petReportPlace;
        private Point petReportPoint;

        public static Response from(Report report) {
            Point point = null;
            if (report.getLatitude() != null && report.getLongitude() != null) {
                point = new Point(report.getLatitude(), report.getLongitude());
            }
            return Response.builder()
                    .id(report.getId())
                    .missingId(report.getMissing().getId())
                    .userId(report.getUserId())
                    .title(report.getTitle())
                    .content(report.getContent())
                    .petImage(report.getPetImage())
                    .petReportPlace(report.getPetReportPlace())
                    .petReportPoint(point)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "제목을 입력해주세요.")
        private String title;
        @NotBlank(message = "내용을 입력해주세요.")
        private String content;
        // petImage는 MultipartFile로 별도 수신 → MinIO 업로드 후 URL 저장
        private String petReportPlace;
        private Point petReportPoint;
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
