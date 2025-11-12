package com.ai.lawyer.domain.precedent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrecedentSummaryListDto {
    private Long id;
    private String caseName;        // 사건명
    private String caseNumber;      // 사건번호
    private LocalDate sentencingDate; // 선고일자
    private String contents;
}
