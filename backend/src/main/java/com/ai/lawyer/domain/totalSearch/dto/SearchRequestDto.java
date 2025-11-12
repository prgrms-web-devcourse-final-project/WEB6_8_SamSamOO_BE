package com.ai.lawyer.domain.totalSearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {

    @Schema(description = "검색 키워드", example = "형사")
    private String keyword;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber = 0;

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize = 10;

    @Schema(description = "법령 검색 포함 여부")
    private boolean includeLaws = true;

    @Schema(description = "판례 검색 포함 여부")
    private boolean includePrecedents = true;
}

