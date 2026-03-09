package com.xorwns56.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service", url = "${client.user-service.url}")
public interface UserServiceClient {

    // 단건 사용자 조회 (NotificationDTO 변환 시 loginId 필요할 경우 사용)
    @GetMapping("/api/user/{id}")
    UserResponse getUser(@PathVariable Long id);

    // 전체 사용자 조회 (실종 신고 등록 시 전체 알림 발송용)
    @GetMapping("/api/user/all")
    List<UserResponse> getAllUsers();

    // user-service의 UserDTO.Response와 동일한 구조
    record UserResponse(Long id, String loginId, String hp) {}
}
