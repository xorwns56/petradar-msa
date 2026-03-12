package com.xorwns56.report.missing;

import com.xorwns56.report.kafka.MissingCreatedEvent;
import com.xorwns56.report.kafka.MissingDeletedEvent;
import com.xorwns56.report.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissingService {

    private final MissingRepository missingRepository;
    private final MinioService minioService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String KAFKA_TOPIC_MISSING_CREATED = "missing-created";
    private static final String KAFKA_TOPIC_MISSING_DELETED = "missing-deleted";

    // 실종 신고 목록 조회 (검색 + 정렬)
    @Transactional(readOnly = true)
    public List<MissingDTO.Response> getList(String search, String sort) {
        Sort sortBy = "oldest".equals(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        return missingRepository.findByTitleContainingIgnoreCase(search, sortBy).stream()
                .map(MissingDTO.Response::from)
                .collect(Collectors.toList());
    }

    // 내 실종 신고 목록 조회
    @Transactional(readOnly = true)
    public List<MissingDTO.Response> getMyList(Long userId) {
        return missingRepository.findByUserId(userId, Sort.by("createdAt").descending()).stream()
                .map(MissingDTO.Response::from)
                .collect(Collectors.toList());
    }

    // 실종 신고 상세 조회
    @Transactional(readOnly = true)
    public MissingDTO.Response getDetail(Long id) {
        Missing missing = missingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("실종 신고를 찾을 수 없습니다. id: " + id));
        return MissingDTO.Response.from(missing);
    }

    // 실종 신고 등록
    @Transactional
    public void create(Long userId, MissingDTO.Request request, MultipartFile image) {
        // 1. 이미지 MinIO 업로드
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = minioService.upload(image);
        }

        // 2. DB 저장
        MissingDTO.Point point = request.getPetMissingPoint();
        Missing missing = Missing.builder()
                .userId(userId)
                .petName(request.getPetName())
                .petType(request.getPetType())
                .petGender(request.getPetGender())
                .petBreed(request.getPetBreed())
                .petAge(request.getPetAge())
                .petMissingDate(request.getPetMissingDate())
                .petMissingPlace(request.getPetMissingPlace())
                .latitude(point != null ? point.getLat() : null)
                .longitude(point != null ? point.getLng() : null)
                .title(request.getTitle())
                .content(request.getContent())
                .petImage(imageUrl)
                .build();
        missingRepository.save(missing);

        // 3. Kafka로 missing-created 이벤트 발행
        // search-service: CLIP 벡터화 후 pgvector 저장
        // report-service(MissingCreatedConsumer): 전체 유저 알림 발송
        // Kafka가 메시지 보관 → 각 consumer 장애 시에도 eventually 처리 보장
        kafkaTemplate.send(KAFKA_TOPIC_MISSING_CREATED,
                new MissingCreatedEvent(
                        missing.getId(),
                        userId,
                        imageUrl,
                        missing.getPetName(),
                        missing.getPetType(),
                        missing.getPetGender(),
                        missing.getPetBreed(),
                        missing.getPetAge(),
                        missing.getPetMissingPlace(),
                        missing.getTitle(),
                        missing.getContent()
                ));
    }

    // 실종 신고 수정
    @Transactional
    public void update(Long id, Long userId, MissingDTO.Request request) {
        Missing missing = missingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("실종 신고를 찾을 수 없습니다."));
        if (!missing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        MissingDTO.Point point = request.getPetMissingPoint();
        missing.setPetName(request.getPetName());
        missing.setPetType(request.getPetType());
        missing.setPetGender(request.getPetGender());
        missing.setPetBreed(request.getPetBreed());
        missing.setPetAge(request.getPetAge());
        missing.setPetMissingDate(request.getPetMissingDate());
        missing.setPetMissingPlace(request.getPetMissingPlace());
        missing.setLatitude(point != null ? point.getLat() : null);
        missing.setLongitude(point != null ? point.getLng() : null);
        missing.setTitle(request.getTitle());
        missing.setContent(request.getContent());
    }

    // 실종 신고 삭제
    @Transactional
    public void delete(Long id, Long userId) {
        Missing missing = missingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("실종 신고를 찾을 수 없습니다."));
        if (!missing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        // MinIO 이미지 삭제
        if (missing.getPetImage() != null) {
            minioService.delete(missing.getPetImage());
        }

        missingRepository.delete(missing);

        // search-service에서 pgvector 임베딩 삭제
        kafkaTemplate.send(KAFKA_TOPIC_MISSING_DELETED, new MissingDeletedEvent(id));
    }
}
