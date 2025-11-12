package com.ai.lawyer.domain.precedent.repository;

import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.dto.PrecedentSummaryListDto;
import org.springframework.data.domain.Page;


public interface PrecedentRepositoryCustom {
    Page<PrecedentSummaryListDto> searchPrecedentsByKeyword(PrecedentSearchRequestDto requestDto);
}
