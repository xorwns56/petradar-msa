package com.xorwns56.report.report;

import com.xorwns56.report.missing.Missing;
import com.xorwns56.report.missing.MissingRepository;
import com.xorwns56.report.minio.MinioService;
import com.xorwns56.report.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final MissingRepository missingRepository;
    private final MinioService minioService;
    private final NotificationService notificationService;

    // 목격 제보 상세 조회
    @Transactional(readOnly = true)
    public ReportDTO.Response getDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("목격 제보를 찾을 수 없습니다. id: " + reportId));
        return ReportDTO.Response.from(report);
    }

    // 실종 신고별 목격 제보 목록 조회 (페이지네이션)
    @Transactional(readOnly = true)
    public Page<ReportDTO.Response> getListByMissingId(Long missingId, Pageable pageable) {
        return reportRepository.findByMissingId(missingId, pageable)
                .map(ReportDTO.Response::from);
    }

    // 목격 제보 등록 (이미지는 MinIO에 업로드 후 URL만 저장)
    @Transactional
    public void create(Long userId, Long missingId, ReportDTO.Request request, MultipartFile image) {
        Missing missing = missingRepository.findById(missingId)
                .orElseThrow(() -> new IllegalArgumentException("실종 신고를 찾을 수 없습니다. id: " + missingId));

        // 이미지 파일이 있으면 MinIO에 업로드
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = minioService.upload(image);
        }

        ReportDTO.Point point = request.getPetReportPoint();
        Report report = Report.builder()
                .userId(userId)
                .missing(missing)
                .title(request.getTitle())
                .content(request.getContent())
                .petImage(imageUrl)
                .petReportPlace(request.getPetReportPlace())
                .latitude(point != null ? point.getLat() : null)
                .longitude(point != null ? point.getLng() : null)
                .build();
        reportRepository.save(report);
        log.info("목격 제보 등록 완료: reportId={}, missingId={}, userId={}", report.getId(), missingId, userId);

        // 실종 신고 작성자에게 알림 전송
        notificationService.sendToUser(userId, missing.getUserId(), "report", missingId);
    }
}
