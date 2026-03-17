package com.xorwns56.report.missing;

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

import java.util.List;
import java.util.Map;

@Tag(name = "Missing", description = "실종 신고 API")
@RestController
@RequestMapping("/api/missing")
@RequiredArgsConstructor
public class MissingController {

    private final MissingService missingService;

    // 실종 신고 목록 조회 (검색, 정렬, 페이지네이션)
    @Operation(summary = "실종 신고 목록 조회", description = "검색어, 정렬, 페이지네이션을 지원합니다.")
    @GetMapping
    public ResponseEntity<Page<MissingDTO.Response>> getList(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "latest") String sort,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(missingService.getList(search, sort, pageable));
    }

    // 실종 신고 전체 목록 조회 (지도 마커 표시용, 페이징 없음)
    @Operation(summary = "실종 신고 전체 목록 조회", description = "지도 마커 표시용, 페이징 없음")
    @GetMapping("/all")
    public ResponseEntity<List<MissingDTO.Response>> getAll() {
        return ResponseEntity.ok(missingService.getList("", "latest"));
    }

    // ID 목록으로 실종 신고 조회 (search-service 유사도 검색 결과 연동용)
    @Operation(summary = "ID 목록으로 실종 신고 조회", description = "search-service 유사도 검색 결과 연동용")
    @GetMapping("/batch")
    public ResponseEntity<List<MissingDTO.Response>> getByIds(
            @RequestParam List<Long> ids) {
        return ResponseEntity.ok(missingService.getByIds(ids));
    }

    // 내 실종 신고 목록 조회
    @Operation(summary = "내 실종 신고 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<List<MissingDTO.Response>> getMyList(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(missingService.getMyList(userId));
    }

    // 실종 신고 상세 조회
    @Operation(summary = "실종 신고 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<MissingDTO.Response> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(missingService.getDetail(id));
    }

    // 실종 신고 등록
    @Operation(summary = "실종 신고 등록", description = "multipart: request(JSON) + image(File)")
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestPart MissingDTO.Request request,
            @RequestPart(required = false) MultipartFile image) {
        try {
            missingService.create(userId, request, image);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 실종 신고 수정
    @Operation(summary = "실종 신고 수정")
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MissingDTO.Request request) {
        try {
            missingService.update(id, userId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 실종 신고 삭제
    @Operation(summary = "실종 신고 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            missingService.delete(id, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
