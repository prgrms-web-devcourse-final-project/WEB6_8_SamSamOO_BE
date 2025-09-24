package com.ai.lawyer.domain.precedent.dto;

import lombok.Data;

@Data
public class PrecedentSearchRequestDto {
    private String keyword;  // 검색 키워드
    private int pageNumber;               // 페이지 번호
    private int pageSize;               // 페이지 크기
}
