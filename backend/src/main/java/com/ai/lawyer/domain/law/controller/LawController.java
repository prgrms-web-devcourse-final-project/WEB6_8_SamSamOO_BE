package com.ai.lawyer.domain.law.controller;

import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import com.ai.lawyer.domain.law.entity.Law;
import com.ai.lawyer.domain.law.service.LawService;
import com.ai.lawyer.global.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "법령", description = "법령 API")
@RequestMapping("/api/law")
public class LawController {

    private final LawService lawService;


    @GetMapping(value = "/list/save")
    @Operation(summary = "키워드 관련 법령 데이터 저장(벡엔드 전용 API)", description = "벡엔드 데이터 저장용 API입니다")
    public ResponseEntity<?> getStatisticsCard(
            @RequestParam String query
    ) throws Exception {
        long startTime = System.currentTimeMillis();

        lawService.saveLaw(query);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("saveLaw 실행 시간: " + elapsedTime + "ms");

        return ResponseEntity.ok().body("Success");
    }

    @PostMapping("/search")
    @Operation(summary = "볍령 목록 검색 기능", description = "조건에 맞는 법령 목록을 가져옵니다")
    public ResponseEntity<?> searchLaws(@RequestBody LawSearchRequestDto searchRequest) {
        try {
            Page<LawsDto> laws = lawService.searchLaws(searchRequest);
            return ResponseEntity.ok(PageResponseDto.from(laws));
        }catch (Exception e){
            log.error("법령 목록 검색 에러 : " + e.getMessage());
            return ResponseEntity.badRequest().body("법령 목록 검색 에러 : " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "볍령 상세 조회 기능", description = "법령 상세 데이터를 조회합니다 \n" +
            "예시: /api/law/1")
    public ResponseEntity<?> getFullLaw(@PathVariable Long id) {
        try {
            Law law = lawService.getLawWithAllChildren(id);
            return ResponseEntity.ok(law);
        }catch (Exception e){
            log.error("법령 상세 조회 에러 : " + e.getMessage());
            return ResponseEntity.badRequest().body("법령 상세 조회 에러 : " + e.getMessage());
        }



    }
}
