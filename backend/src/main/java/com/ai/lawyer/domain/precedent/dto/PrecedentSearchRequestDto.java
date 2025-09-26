package com.ai.lawyer.domain.precedent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PrecedentSearchRequestDto {

    @Schema(description = "검색 키워드", example = "노동")
    private String keyword;  // 검색 키워드

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;               // 페이지 번호

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;               // 페이지 크기
}
