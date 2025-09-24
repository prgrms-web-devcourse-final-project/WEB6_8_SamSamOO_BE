package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import org.springframework.data.domain.Page;

public interface LawRepositoryCustom {
    Page<LawsDto> searchLaws(LawSearchRequestDto searchRequest);
}
