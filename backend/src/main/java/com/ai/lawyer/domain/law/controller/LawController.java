package com.ai.lawyer.domain.law.controller;

import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import com.ai.lawyer.domain.law.entity.Law;
import com.ai.lawyer.domain.law.service.LawService;
import com.ai.lawyer.global.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/law")
public class LawController {

    private final LawService lawService;


    @GetMapping(value = "/list/save")
    @Operation(summary = "키워드 관련 법령 데이터 저장(벡엔드 전용 API)", description = "벡엔드 데이터 저장용 API입니다")
    public ResponseEntity<?> getStatisticsCard(
            @RequestParam String query,
            @RequestParam int page
    ) throws Exception {
        long startTime = System.currentTimeMillis();

        lawService.saveLaw(query, page);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("saveLaw 실행 시간: " + elapsedTime + "ms");

        return ResponseEntity.ok().body("Success");
    }

    @PostMapping("/search")
    @Operation(summary = "볍령 목록 검색 기능", description = "조건에 맞는 법령 목록을 가져옵니다")
    public ResponseEntity<PageResponseDto> searchLaws(@RequestBody LawSearchRequestDto searchRequest) {
        Page<LawsDto> laws = lawService.searchLaws(searchRequest);
        PageResponseDto response = PageResponseDto.builder()
                .content(laws.getContent())
                .totalElements(laws.getTotalElements())
                .totalPages(laws.getTotalPages())
                .pageNumber(laws.getNumber())
                .pageSize(laws.getSize())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "볍령 상세 조회 기능", description = "법령 상세 데이터를 조회합니다 \n" +
            "예시: /api/law/1")
    public ResponseEntity<Law> getFullLaw(@PathVariable Long id) {
        Law law = lawService.getLawWithAllChildren(id);

        return ResponseEntity.ok(law);
    }
}