package com.ai.lawyer.domain.precedent.repository;

import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.dto.PrecedentSummaryListDto;
import com.ai.lawyer.domain.precedent.entity.QPrecedent;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PrecedentRepositoryImpl implements PrecedentRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    private final QPrecedent precedent = QPrecedent.precedent;

    @Override
    public Page<PrecedentSummaryListDto> searchPrecedentsByKeyword(PrecedentSearchRequestDto requestDto) {

        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(requestDto.getKeyword())) {
            String pattern = "%" + requestDto.getKeyword().trim() + "%";
            builder.or(precedent.getNotice().like(pattern))
                    .or(precedent.getSummaryOfTheJudgment().like(pattern))
                    .or(precedent.getPrecedentContent().like(pattern))
                    .or(precedent.getCaseName().like(pattern))
                    .or(precedent.getCaseNumber().like(pattern));
        }

        // 페이징 및 정렬 설정
        Pageable pageable = PageRequest.of(
                requestDto.getPageNumber(),
                requestDto.getPageSize(),
                Sort.by(Sort.Direction.DESC, "sentencingDate")
        );

        // 1) 전체 건수 조회
        long total = queryFactory
                .selectFrom(precedent)
                .where(builder)
                .fetchCount();

        if (total == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // CLOB 필드 조건: null 또는 빈 문자열("") 체크
        StringExpression contents = new CaseBuilder()
                // summaryOfTheJudgment가 null도 아니고, 빈 문자열도 아닐 때
                .when(precedent.getSummaryOfTheJudgment().isNotNull()
                        .and(precedent.getSummaryOfTheJudgment().ne("")))
                .then(precedent.getSummaryOfTheJudgment())
                // notice가 null도 아니고, 빈 문자열도 아닐 때
                .when(precedent.getNotice().isNotNull()
                        .and(precedent.getNotice().ne("")))
                .then(precedent.getNotice())
                // 그 외에는 precedentContent 전체
                .otherwise(precedent.getPrecedentContent());


        // 2) 데이터 조회
        List<PrecedentSummaryListDto> content = queryFactory
                .select(Projections.constructor(PrecedentSummaryListDto.class,
                        precedent.getId(),
                        precedent.getCaseName(),
                        precedent.getCaseNumber(),
                        precedent.getSentencingDate(),
                        contents
                ))
                .from(precedent)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(precedent.getSentencingDate().desc())
                .fetch();

        // Java에서 후처리: 빈 문자열 체크 및 【주문】부분 추출
        content.forEach(dto -> {
            String processedContents = processContents(dto.getContents());
            dto.setContents(processedContents);
        });

        return new PageImpl<>(content, pageable, total);
    }

    private String processContents(String contents) {
        if (contents == null || contents.trim().isEmpty()) {
            return "";
        }

        // 이미 summaryOfTheJudgment나 notice가 있는 경우
        if (!contents.contains("【주    문】")) {
            return contents;
        }

        // precedentContent에서 【주문】부터 【이유】까지 추출
        String startTag = "【주    문】";
        String endTag = "【이    유】";

        int startIndex = contents.indexOf(startTag);
        int endIndex = contents.indexOf(endTag);

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return contents.substring(startIndex, endIndex).trim();
        }

        return contents;
    }
}
