package com.xorwns56.search.kafka;

import com.xorwns56.search.missing.MissingDocument;
import com.xorwns56.search.missing.MissingSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Kafka 이벤트 수신 → Elasticsearch 인덱싱/삭제
@Slf4j
@Component
@RequiredArgsConstructor
public class MissingEventConsumer {

    private final MissingSearchService searchService;

    // 실종 신고 등록 이벤트 → Elasticsearch 인덱싱
    @KafkaListener(topics = "missing-created", groupId = "search-service")
    public void handleMissingCreated(MissingCreatedEvent event) {
        log.info("missing-created 이벤트 수신: missingId={}", event.missingId());

        MissingDocument document = MissingDocument.builder()
                .id(event.missingId())
                .userId(event.userId())
                .title(event.title())
                .content(event.content())
                .petName(event.petName())
                .petType(event.petType())
                .petGender(event.petGender())
                .petBreed(event.petBreed())
                .petAge(event.petAge())
                .petMissingPlace(event.petMissingPlace())
                .petMissingDate(event.petMissingDate())
                .imageUrl(event.imageUrl())
                .build();

        searchService.index(document);
    }

    // 실종 신고 삭제 이벤트 → Elasticsearch 인덱스 삭제
    @KafkaListener(topics = "missing-deleted", groupId = "search-service")
    public void handleMissingDeleted(MissingDeletedEvent event) {
        log.info("missing-deleted 이벤트 수신: missingId={}", event.missingId());
        searchService.delete(event.missingId());
    }
}
