package com.xorwns56.report.minio;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    // 이미지 업로드 → MinIO URL 반환
    public String upload(MultipartFile file) {
        String objectName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 업로드 실패: " + e.getMessage());
        }
        return endpoint + "/" + bucket + "/" + objectName;
    }

    // 이미지 삭제
    public void delete(String imageUrl) {
        String objectName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO 삭제 실패: {}", e.getMessage());
        }
    }
}
