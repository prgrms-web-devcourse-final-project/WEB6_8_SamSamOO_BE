package com.ai.lawyer.domain.totalSearch.controller;

import com.ai.lawyer.domain.totalSearch.dto.SearchRequestDto;
import com.ai.lawyer.domain.totalSearch.dto.SearchResponseDto;
import com.ai.lawyer.domain.totalSearch.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/totalSearch")
@Tag(name = "통합 검색", description = "법령 + 판례 통합 검색 API")
public class TotalSearchController {

    private final SearchService searchService;

    @PostMapping("/search")
    @Operation(summary = "법령 + 판례 통합 검색", description = "법령과 판례를 함께 검색합니다.")
    public ResponseEntity<?> combinedSearch(@RequestBody SearchRequestDto request) {
        try {
            SearchResponseDto response = searchService.combinedSearch(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("통합 검색 에러 : {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("통합 검색 에러 : " + e.getMessage());
        }
    }
}
