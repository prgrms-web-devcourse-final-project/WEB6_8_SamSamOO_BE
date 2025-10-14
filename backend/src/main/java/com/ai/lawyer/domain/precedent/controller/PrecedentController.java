package com.ai.lawyer.domain.precedent.controller;

import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.dto.PrecedentSummaryListDto;
import com.ai.lawyer.domain.precedent.entity.Precedent;
import com.ai.lawyer.domain.precedent.service.PrecedentService;
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
@RequestMapping("/api/precedent")
@Tag(name = "판례", description = "판례 API")
public class PrecedentController {

    private final PrecedentService precedentService;

    @GetMapping(value = "/list/save")
    @Operation(summary = "키워드 관련 판례 데이터 저장(벡엔드 전용 API)", description = "벡엔드 데이터 저장용 API입니다")
    public ResponseEntity<?> list(
            @RequestParam String query
    ) throws Exception {
        return ResponseEntity.ok().body(precedentService.searchAndSaveAll(query));
    }

    /**
     * POST /api/precedent/search
     * 키워드로 판례 검색 (판시사항, 판결요지, 판례내용, 사건명에서 검색)
     * @param requestDto
     * @return id, 사건명, 사건번호, 선고일자 리스트
     */

    @PostMapping("/search")
    @Operation(summary = "판례 목록 검색 기능", description = "조건에 맞는 판례 목록을 가져옵니다")
    public ResponseEntity<?> searchPrecedents(
            @RequestBody PrecedentSearchRequestDto requestDto) {
        try {
            Page<PrecedentSummaryListDto> results = precedentService.searchByKeywordV2(requestDto);
            return ResponseEntity.ok(PageResponseDto.from(results));
        }catch (Exception e){
            log.error("판례 목록 검색 에러 : " + e.getMessage());
            return ResponseEntity.badRequest().body("판례 목록 검색 에러 : " + e.getMessage());
        }
    }

    /**
     * GET /api/precedent/{id}
     * 주어진 id로 Precedent 조회
     *
     * @param id Precedent PK
     * @return Precedent 엔티티
     */
    @GetMapping("/{id}")
    @Operation(summary = "판례 상세 조회 기능", description = "판례 상세 데이터를 조회합니다 \n" +
            "예시: /api/precedent/1")
    public ResponseEntity<?> getPrecedent(@PathVariable Long id) {
        try {
            Precedent precedent = precedentService.getPrecedentById(id);
            return ResponseEntity.ok(precedent);
        }catch (Exception e){
            log.error("판례 상세 조회 에러 : " + e.getMessage());
            return ResponseEntity.badRequest().body("판례 상세 조회 에러 : " + e.getMessage());
        }
    }
}
