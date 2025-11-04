package com.ai.lawyer.domain.search.service;

import com.ai.lawyer.domain.search.dto.SearchRequestDto;
import com.ai.lawyer.domain.search.dto.SearchResponseDto;

public interface SearchService {
    SearchResponseDto combinedSearch(SearchRequestDto request);
}

