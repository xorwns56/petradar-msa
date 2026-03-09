package com.xorwns56.user.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회 - Gateway가 X-User-Id 헤더로 userId 전달
    @GetMapping("/me")
    public ResponseEntity<UserDTO.Response> getMe(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.findById(userId));
    }

    // 내 정보 수정
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
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(@RequestHeader("X-User-Id") Long userId) {
        userService.delete(userId);
        return ResponseEntity.ok().build();
    }

    // 단건 사용자 조회 (internal - report-service에서 호출)
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO.Response> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // 전체 사용자 조회 (internal - 실종 신고 전체 알림 발송 시 report-service에서 호출)
    @GetMapping("/all")
    public ResponseEntity<List<UserDTO.Response>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
}
