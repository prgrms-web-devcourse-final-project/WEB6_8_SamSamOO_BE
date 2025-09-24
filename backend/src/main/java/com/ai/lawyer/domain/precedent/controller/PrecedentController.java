package com.ai.lawyer.domain.precedent.controller;

import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.dto.PrecedentSummaryListDto;
import com.ai.lawyer.domain.precedent.entity.Precedent;
import com.ai.lawyer.domain.precedent.service.PrecedentService;
import com.ai.lawyer.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/precedent")
public class PrecedentController {

    private final PrecedentService precedentService;

    @GetMapping(value = "/list/save")
    public ResponseEntity<?> list(
            @RequestParam String query
    ) throws Exception {
        return ResponseEntity.ok().body(precedentService.searchAndSaveAll(query));
    }

    @PostMapping("/search")
    public ResponseEntity<PageResponseDto> searchPrecedents(
            @RequestBody PrecedentSearchRequestDto requestDto) {

        Page<PrecedentSummaryListDto> results = precedentService.searchByKeyword(requestDto);
        PageResponseDto response = PageResponseDto.builder()
                .content(results.getContent())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .pageNumber(results.getNumber())
                .pageSize(results.getSize())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/precedent/{id}
     * 주어진 id로 Precedent 조회
     *
     * @param id Precedent PK
     */
    @GetMapping("/{id}")
    public ResponseEntity<Precedent> getPrecedent(@PathVariable Long id) {
        Precedent precedent = precedentService.getPrecedentById(id);
        return ResponseEntity.ok(precedent);
    }
}
