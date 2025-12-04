package com.ai.lawyer.domain.totalSearch.service;

import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.service.LawService;
import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.service.PrecedentService;
import com.ai.lawyer.domain.totalSearch.dto.SearchRequestDto;
import com.ai.lawyer.domain.totalSearch.dto.SearchResponseDto;
import com.ai.lawyer.global.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final LawService lawService;
    private final PrecedentService precedentService;

    @Override
    public SearchResponseDto combinedSearch(SearchRequestDto request) {
        SearchResponseDto response = new SearchResponseDto();

        try {
            CompletableFuture<?> lawFuture = null;
            CompletableFuture<?> precFuture = null;

            if (request.isIncludeLaws()) {
                // lawName에 명시값이 없으면 공통 키워드를 fallback으로 사용
                // 예: keyword="형사", lawName="" -> lawName="형사"
                if (StringUtils.hasText(request.getKeyword()) && !StringUtils.hasText(request.getLawName())) {
                    request.setLawName(request.getKeyword());
                }

                String lawNameForSearch = StringUtils.hasText(request.getLawName()) ? request.getLawName() : request.getKeyword();

                LawSearchRequestDto lawReq = LawSearchRequestDto.builder()
                        .lawName(lawNameForSearch)
                        .lawField(request.getLawField())
                        .ministry(request.getMinistry())
                        .promulgationDateStart(request.getPromulgationDateStart())
                        .promulgationDateEnd(request.getPromulgationDateEnd())
                        .enforcementDateStart(request.getEnforcementDateStart())
                        .enforcementDateEnd(request.getEnforcementDateEnd())
                        .pageNumber(request.getPageNumber())
                        .pageSize(request.getPageSize())
                        .build();

                lawFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return PageResponseDto.from(lawService.searchLaws(lawReq));
                    } catch (Exception e) {
                        log.warn("법령 검색 실패 in combinedSearch: {}", e.getMessage());
                        return null;
                    }
                });
            }

            if (request.isIncludePrecedents()) {
                PrecedentSearchRequestDto precReq = new PrecedentSearchRequestDto();
                precReq.setKeyword(request.getKeyword());
                precReq.setSentencingDateStart(request.getSentencingDateStart());
                precReq.setSentencingDateEnd(request.getSentencingDateEnd());
                precReq.setPageNumber(request.getPageNumber());
                precReq.setPageSize(request.getPageSize());

                precFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return PageResponseDto.from(precedentService.searchByKeywordV2(precReq));
                    } catch (Exception e) {
                        log.warn("판례 검색 실패 in combinedSearch: {}", e.getMessage());
                        return null;
                    }
                });
            }

            if (lawFuture != null) {
                Object lawResult = lawFuture.join();
                response.setLaws(lawResult == null ? null : (PageResponseDto) lawResult);
            }

            if (precFuture != null) {
                Object precResult = precFuture.join();
                response.setPrecedents(precResult == null ? null : (PageResponseDto) precResult);
            }

            // 통합 total 계산: laws.totalElements + precedents.totalElements
            long lawsTotal = response.getLaws() != null ? response.getLaws().getTotalElements() : 0L;
            long precTotal = response.getPrecedents() != null ? response.getPrecedents().getTotalElements() : 0L;
            response.setLawPrecTotalElements(lawsTotal + precTotal);

            // 통합 total pages 계산: laws.totalPages + precedents.totalPages
            int lawsPages = response.getLaws() != null ? response.getLaws().getTotalPages() : 0;
            int precPages = response.getPrecedents() != null ? response.getPrecedents().getTotalPages() : 0;
            response.setLawPrecTotalPages(lawsPages + precPages);

            if (request.isIncludeLaws() && request.isIncludePrecedents()
                    && response.getLaws() == null && response.getPrecedents() == null) {
                throw new RuntimeException("법령 및 판례 검색 모두 실패");
            }

            return response;
        } catch (Exception e) {
            log.error("통합 검색 에러 in service : {}", e.getMessage(), e);
            throw e;
        }
    }
}
