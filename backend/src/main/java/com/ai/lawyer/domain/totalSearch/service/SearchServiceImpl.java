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
                LawSearchRequestDto lawReq = LawSearchRequestDto.builder()
                        .lawName(request.getKeyword())
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
