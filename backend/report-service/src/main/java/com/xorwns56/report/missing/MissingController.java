package com.xorwns56.report.missing;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missing")
@RequiredArgsConstructor
public class MissingController {

    private final MissingService missingService;

    // 실종 신고 목록 조회 (검색, 정렬)
    @GetMapping
    public ResponseEntity<List<MissingDTO.Response>> getList(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "latest") String sort) {
        return ResponseEntity.ok(missingService.getList(search, sort));
    }

    // 내 실종 신고 목록 조회
    @GetMapping("/me")
    public ResponseEntity<List<MissingDTO.Response>> getMyList(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(missingService.getMyList(userId));
    }

    // 실종 신고 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<MissingDTO.Response> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(missingService.getDetail(id));
    }

    // 실종 신고 등록
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
