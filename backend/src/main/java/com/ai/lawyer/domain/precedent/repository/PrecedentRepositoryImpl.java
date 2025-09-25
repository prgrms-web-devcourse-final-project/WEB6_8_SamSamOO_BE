package com.ai.lawyer.domain.precedent.repository;

import com.ai.lawyer.domain.precedent.dto.PrecedentSearchRequestDto;
import com.ai.lawyer.domain.precedent.dto.PrecedentSummaryListDto;
import com.ai.lawyer.domain.precedent.entity.QPrecedent;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
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

        // 2) 데이터 조회
        List<PrecedentSummaryListDto> content = queryFactory
                .select(Projections.constructor(PrecedentSummaryListDto.class,
                        precedent.getId(),
                        precedent.getCaseName(),
                        precedent.getCaseNumber(),
                        precedent.getSentencingDate()))
                .from(precedent)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(precedent.getSentencingDate().desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }
}