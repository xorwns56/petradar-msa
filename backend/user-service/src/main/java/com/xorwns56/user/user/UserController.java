package com.xorwns56.user.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회 - Gateway가 X-User-Id 헤더로 userId 전달
    @Operation(summary = "내 정보 조회", description = "Gateway가 JWT에서 추출한 X-User-Id 헤더로 사용자 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserDTO.Response> getMe(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.findById(userId));
    }

    // 내 정보 수정
    @Operation(summary = "내 정보 수정")
    @PatchMapping("/me")
    public ResponseEntity<?> updateMe(@RequestHeader("X-User-Id") Long userId,
                                      @Valid @RequestBody UserDTO.UpdateRequest request) {
        try {
            userService.update(userId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 회원 탈퇴
    @Operation(summary = "회원 탈퇴", description = "DB 삭제 + Redis 토큰 삭제 + Kafka로 user-deleted 이벤트 발행")
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(@RequestHeader("X-User-Id") Long userId) {
        userService.delete(userId);
        return ResponseEntity.ok().build();
    }

    // 단건 사용자 조회 (internal - report-service에서 호출)
    @Operation(summary = "단건 사용자 조회 (internal)", description = "report-service에서 FeignClient로 호출합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO.Response> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // 전체 사용자 조회 (internal - 실종 신고 전체 알림 발송 시 report-service에서 호출)
    @Operation(summary = "전체 사용자 조회 (internal)", description = "실종 신고 등록 시 전체 알림 발송을 위해 report-service에서 호출합니다.")
    @GetMapping("/all")
    public ResponseEntity<List<UserDTO.Response>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
}
