package com.xorwns56.search.missing;

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissingSearchService {

    private final MissingSearchRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;

    // Elasticsearch에 실종 신고 문서 인덱싱 (Kafka 이벤트 수신 시 호출)
    public void index(MissingDocument document) {
        repository.save(document);
        log.info("실종 신고 인덱싱 완료: missingId={}", document.getId());
    }

    // Elasticsearch에서 실종 신고 문서 삭제 (Kafka 이벤트 수신 시 호출)
    public void delete(Long missingId) {
        repository.deleteById(missingId);
        log.info("실종 신고 인덱스 삭제: missingId={}", missingId);
    }

    // 전문 검색 (multi_match: 제목, 내용, 이름, 품종, 실종장소에서 검색)
    public SearchDTO.Response search(String query, int page, int size) {
        // multi_match 쿼리: 여러 필드에서 동시에 검색
        // most_fields: 여러 필드에 걸친 조합(품종+실종장소 등) 매칭 시 필드별 점수를 합산해 관련도 상승
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(mm -> mm
                                .query(query)
                                .fields(
                                        "title^2",           // 제목 가중치 2배
                                        "content",
                                        "petName",           // 이름: 목격자는 모르므로 기본 가중치
                                        "petBreed^3",        // 품종 가중치 3배 (목격자 주요 검색어)
                                        "petMissingPlace^2"  // 실종장소 가중치 2배 (목격 위치)
                                )
                                .type(TextQueryType.MostFields)  // 필드별 점수 합산 → 품종+실종장소 조합 검색 대응
                                .fuzziness("AUTO")  // 오타 허용 (자동 거리 계산, 표기 흔들림 대응)
                        )
                )
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<MissingDocument> hits = elasticsearchOperations.search(searchQuery, MissingDocument.class);

        List<SearchDTO.Result> results = hits.getSearchHits().stream()
                .map(hit -> SearchDTO.Result.from(hit.getContent(), hit.getScore()))
                .toList();

        log.info("검색 완료: query='{}', 결과={}건", query, hits.getTotalHits());
        return SearchDTO.Response.builder()
                .results(results)
                .totalHits(hits.getTotalHits())
                .build();
    }

    // 필터 + 검색 (종류, 성별 필터 적용)
    public SearchDTO.Response searchWithFilter(String query, String petType, String petGender, int page, int size) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder();

        // bool 쿼리: must(전문 검색) + filter(필터링)
        queryBuilder.withQuery(q -> q
                .bool(b -> {
                    // 검색어가 있으면 multi_match 적용
                    if (query != null && !query.isBlank()) {
                        b.must(m -> m
                                .multiMatch(mm -> mm
                                        .query(query)
                                        .fields("title^2", "content", "petName", "petBreed^3", "petMissingPlace^2")
                                        .type(TextQueryType.MostFields)  // 필드별 점수 합산 (조합 검색 대응)
                                        .fuzziness("AUTO")
                                )
                        );
                    } else {
                        // 검색어 없으면 전체 조회
                        b.must(m -> m.matchAll(ma -> ma));
                    }

                    // 종류 필터 (dog, cat, etc)
                    if (petType != null && !petType.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("petType").value(petType)));
                    }

                    // 성별 필터 (M, F)
                    if (petGender != null && !petGender.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("petGender").value(petGender)));
                    }

                    return b;
                })
        );

        queryBuilder.withPageable(PageRequest.of(page, size));

        SearchHits<MissingDocument> hits = elasticsearchOperations.search(queryBuilder.build(), MissingDocument.class);

        List<SearchDTO.Result> results = hits.getSearchHits().stream()
                .map(hit -> SearchDTO.Result.from(hit.getContent(), hit.getScore()))
                .toList();

        log.info("필터 검색 완료: query='{}', petType='{}', petGender='{}', 결과={}건",
                query, petType, petGender, hits.getTotalHits());
        return SearchDTO.Response.builder()
                .results(results)
                .totalHits(hits.getTotalHits())
                .build();
    }
}
