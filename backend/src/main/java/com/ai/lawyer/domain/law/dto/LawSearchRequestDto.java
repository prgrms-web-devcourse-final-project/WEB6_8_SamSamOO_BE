package com.ai.lawyer.domain.law.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LawSearchRequestDto {

    @Schema(description = "법령명", example = "노동")
    private String lawName;        // 법령명

    @Schema(description = "법령분야", example = "법률")
    private String lawField;       // 법령분야

    @Schema(description = "소관부처", example = "고용노동부")
    private String ministry;       // 소관부처

    @Schema(description = "공포일자 시작", example =  "2000-03-25")
    private LocalDate promulgationDateStart;      // 공포일자 시작

    @Schema(description = "공포일자 종료", example =  "2025-03-25")
    private LocalDate promulgationDateEnd;        // 공포일자 종료

    @Schema(description = "시행일자 시작", example =  "2000-03-25")
    private LocalDate enforcementDateStart;      // 시행일자 시작

    @Schema(description = "시행일자 종료", example =  "2025-03-25")
    private LocalDate enforcementDateEnd;        // 시행일자 종료

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;               // 페이지 번호

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;               // 페이지 크기
}
