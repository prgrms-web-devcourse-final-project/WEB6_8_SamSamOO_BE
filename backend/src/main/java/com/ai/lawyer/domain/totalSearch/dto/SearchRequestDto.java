package com.ai.lawyer.domain.totalSearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {

    @Schema(description = "공통 검색 키워드", example = "형사")
    private String keyword;

    // Pagination
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber = 0;

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize = 10;

    // Include flags
    @Schema(description = "법령 검색 포함 여부")
    private boolean includeLaws = true;

    @Schema(description = "판례 검색 포함 여부")
    private boolean includePrecedents = true;

    // --- Precedent specific filters ---
    @Schema(description = "판례: 선고일자 시작", example = "2000-01-01")
    private LocalDate sentencingDateStart;

    @Schema(description = "판례: 선고일자 종료", example = "2024-12-31")
    private LocalDate sentencingDateEnd;

    // --- Law specific filters ---
    @Schema(description = "법령: 법령명", example = "형사")
    private String lawName;

    @Schema(description = "법령: 법령분야", example = "법률")
    private String lawField;

    @Schema(description = "법령: 소관부처", example = "법무부")
    private String ministry;

    @Schema(description = "법령: 공포일자 시작", example = "2000-03-25")
    private LocalDate promulgationDateStart;

    @Schema(description = "법령: 공포일자 종료", example = "2025-03-25")
    private LocalDate promulgationDateEnd;

    @Schema(description = "법령: 시행일자 시작", example = "2000-03-25")
    private LocalDate enforcementDateStart;

    @Schema(description = "법령: 시행일자 종료", example = "2025-03-25")
    private LocalDate enforcementDateEnd;
}
