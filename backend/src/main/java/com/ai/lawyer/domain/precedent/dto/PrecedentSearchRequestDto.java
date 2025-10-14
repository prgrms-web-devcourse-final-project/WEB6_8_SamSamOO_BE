package com.ai.lawyer.domain.precedent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@Data
public class PrecedentSearchRequestDto {

    @Schema(description = "검색 키워드", example = "절도")
    private String keyword;  // 검색 키워드

    @Schema(description = "선고일자 시작", example = "2000-01-01")
    private LocalDate sentencingDateStart; // 선고일자 시작

    @Schema(description = "선고일자 종료", example = "2024-12-31")
    private LocalDate sentencingDateEnd;   // 선고일자 종료

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;               // 페이지 번호

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;               // 페이지 크기

    public Pageable toPageable() {
        return PageRequest.of(pageNumber, pageSize);
    }
}
