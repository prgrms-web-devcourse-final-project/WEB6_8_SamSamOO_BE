package com.ai.lawyer.domain.law.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LawsDto {
    private Long id;

    private String lawName; // 법령명

    private String lawField; // 법령분야

    private String ministry; // 소관부처

    private String promulgationNumber; // 공포번호

    private LocalDate promulgationDate; // 공포일자

    private LocalDate enforcementDate; // 시행일자

    private String firstJoContent; // 조
}
