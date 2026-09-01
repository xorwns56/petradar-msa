package com.xorwns56.search.missing;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// Spring Data Elasticsearch 레포지토리 (Spring Data JPA의 JpaRepository와 동일한 패턴)
public interface MissingSearchRepository extends ElasticsearchRepository<MissingDocument, Long> {
}
