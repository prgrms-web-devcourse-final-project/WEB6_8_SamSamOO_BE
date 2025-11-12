package com.ai.lawyer.domain.totalSearch.service;

import com.ai.lawyer.domain.totalSearch.dto.SearchRequestDto;
import com.ai.lawyer.domain.totalSearch.dto.SearchResponseDto;

public interface SearchService {
    SearchResponseDto combinedSearch(SearchRequestDto request);
}

