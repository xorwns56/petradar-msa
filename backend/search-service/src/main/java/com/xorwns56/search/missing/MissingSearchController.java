package com.xorwns56.search.missing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Search", description = "실종동물 검색 API")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class MissingSearchController {

    private final MissingSearchService searchService;

    // 전문 검색 (제목, 내용, 이름, 품종, 실종장소에서 통합 검색)
    @Operation(summary = "실종동물 검색", description = "Elasticsearch 전문 검색 (제목, 내용, 이름, 품종, 장소)")
    @GetMapping
    public ResponseEntity<SearchDTO.Response> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String petType,
            @RequestParam(required = false) String petGender,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        SearchDTO.Response response;
        // 필터가 있으면 필터 검색, 없으면 단순 검색
        if ((petType != null && !petType.isBlank()) || (petGender != null && !petGender.isBlank())) {
            response = searchService.searchWithFilter(q, petType, petGender, page, size);
        } else {
            response = searchService.search(q, page, size);
        }
        return ResponseEntity.ok(response);
    }
}
