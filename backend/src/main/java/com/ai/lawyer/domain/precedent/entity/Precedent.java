package com.ai.lawyer.domain.precedent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "precedent")
public class Precedent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    private String precedentNumber; // 판례일련번호

    @Lob
    @Column(columnDefinition = "TEXT")
    private String caseName; // 사건명

    @Lob
    @Column(columnDefinition = "TEXT")
    private String caseNumber; // 사건번호

    private LocalDate sentencingDate; // 선고일자

    private String sentence; // 선고

    private String courtName; // 법원명

    private String courtTypeCode; // 법원종류코드

    private String caseTypeName; // 사건종류명

    private String caseTypeCode; // 사건종류코드

    private String typeOfJudgment; // 판결유형

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String notice; // 판시사항

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summaryOfTheJudgment; // 판결요지

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String referenceArticle; // 참조조문

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String referencePrecedent; // 참조판례

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String precedentContent; // 판례내용
}
