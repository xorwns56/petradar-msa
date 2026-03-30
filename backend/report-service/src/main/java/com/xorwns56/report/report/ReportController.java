package com.xorwns56.report.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Report", description = "목격 제보 API")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 목격 제보 상세 조회
    @Operation(summary = "목격 제보 상세 조회")
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDTO.Response> getDetail(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportService.getDetail(reportId));
    }

    // 실종 신고별 목격 제보 목록 조회 (페이지네이션)
    @Operation(summary = "실종 신고별 목격 제보 목록 조회")
    @GetMapping("/missing/{missingId}")
    public ResponseEntity<Page<ReportDTO.Response>> getList(
            @PathVariable Long missingId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(reportService.getListByMissingId(missingId, pageable));
    }

    // 목격 제보 등록 (비회원도 가능 - X-User-Id 없을 수 있음)
    // multipart: request(JSON) + image(File) — MissingController.create와 동일한 패턴
    @Operation(summary = "목격 제보 등록", description = "multipart: request(JSON) + image(File), 비회원도 가능")
    @PostMapping("/missing/{missingId}")
    public ResponseEntity<?> create(
            @PathVariable Long missingId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestPart ReportDTO.Request request,
            @RequestPart(required = false) MultipartFile image) {
        try {
            reportService.create(userId, missingId, request, image);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
