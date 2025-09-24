package com.ai.lawyer.domain.law.controller;

import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import com.ai.lawyer.domain.law.entity.Law;
import com.ai.lawyer.domain.law.service.LawService;
import com.ai.lawyer.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/law")
public class LawController {

    private final LawService lawService;

    // 법령 리스트 출력
    @GetMapping(value = "/list")
    public ResponseEntity<?> list(
            @RequestParam String query,
            @RequestParam int page
    ) throws Exception {
        String lawList = lawService.getLawList(query, page);
        return ResponseEntity.ok().body(lawList);
    }


    @GetMapping(value = "/list/save")
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

    @GetMapping("/{id}")
    public ResponseEntity<Law> getFullLaw(@PathVariable Long id) {
        Law law = lawService.getLawWithAllChildren(id);

        return ResponseEntity.ok(law);
    }


    @PostMapping("/search")
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
}
