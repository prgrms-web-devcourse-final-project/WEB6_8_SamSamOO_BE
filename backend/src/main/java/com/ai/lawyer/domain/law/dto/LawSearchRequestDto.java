package com.ai.lawyer.domain.law.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LawSearchRequestDto {
    private String lawName;        // 법령명
    private String lawField;       // 법령분야
    private String ministry;       // 소관부처
    private LocalDate promulgationDateStart;      // 공포일자 시작
    private LocalDate promulgationDateEnd;        // 공포일자 종료
    private LocalDate enforcementDateStart;      // 시행일자 시작
    private LocalDate enforcementDateEnd;        // 시행일자 종료
    private int pageNumber;               // 페이지 번호
    private int pageSize;               // 페이지 크기
}
