package com.xorwns56.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "search-service", url = "${client.search-service.url}")
public interface SearchServiceClient {

    // 실종 신고 등록 시 이미지 임베딩 저장
    @PostMapping(value = "/api/search/index", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void indexMissing(@RequestPart Long missingId, @RequestPart MultipartFile image);

    // 실종 신고 삭제 시 임베딩 제거
    @DeleteMapping("/api/search/index/{missingId}")
    void deleteIndex(@PathVariable Long missingId);
}
