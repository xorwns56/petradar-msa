package com.xorwns56.search.missing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

// Elasticsearch 인덱스 문서 (Spring의 @Entity와 유사)
// missing 테이블의 검색 대상 필드를 Elasticsearch에 인덱싱
@Document(indexName = "missing")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingDocument {

    // report-service의 missing.id를 그대로 사용
    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId;

    // text 타입: standard 분석기로 토큰화 후 인덱싱
    // (검색 시 fuzziness로 오타/표기 흔들림 보정 — 예: "말티즈" ↔ "몰티즈")
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String petName;

    // keyword 타입: 정확히 일치하는 검색 (필터링 용도)
    @Field(type = FieldType.Keyword)
    private String petType;

    @Field(type = FieldType.Keyword)
    private String petGender;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String petBreed;

    @Field(type = FieldType.Keyword)
    private String petAge;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String petMissingPlace;

    // 실종일자: 검색 대상이 아닌 표시용이므로 keyword로 저장
    @Field(type = FieldType.Keyword, index = false)
    private String petMissingDate;

    // 이미지 URL은 검색 대상이 아니므로 keyword로 저장
    @Field(type = FieldType.Keyword, index = false)
    private String imageUrl;
}
