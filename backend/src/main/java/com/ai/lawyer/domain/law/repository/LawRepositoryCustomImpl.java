package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import com.ai.lawyer.domain.law.entity.QLaw;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LawRepositoryCustomImpl implements LawRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private QLaw law = QLaw.law;

    @Override
    public Page<LawsDto> searchLaws(LawSearchRequestDto searchRequest) {
        BooleanBuilder builder = new BooleanBuilder();

        // 법령명 조건 (부분 검색)
        if (StringUtils.hasText(searchRequest.getLawName())) {
            builder.and(law.getLawName().containsIgnoreCase(searchRequest.getLawName()));
        }

        // 소관부처 조건 (완전 일치)
        if (StringUtils.hasText(searchRequest.getMinistry())) {
            builder.and(law.getMinistry().eq(searchRequest.getMinistry()));
        }

        // 법령분야 조건 (완전 일치)
        if (StringUtils.hasText(searchRequest.getLawField())) {
            builder.and(law.getLawField().eq(searchRequest.getLawField()));
        }

        // 시행일자 범위 조건
        if (searchRequest.getEnforcementDateStart() != null &&
                searchRequest.getEnforcementDateEnd() != null) {
            builder.and(law.getEnforcementDate().between(
                    searchRequest.getEnforcementDateStart(),
                    searchRequest.getEnforcementDateEnd()));
        } else if (searchRequest.getEnforcementDateStart() != null) {
            builder.and(law.getEnforcementDate().goe(searchRequest.getEnforcementDateStart()));
        } else if (searchRequest.getEnforcementDateEnd() != null) {
            builder.and(law.getEnforcementDate().loe(searchRequest.getEnforcementDateEnd()));
        }

        // 공포일자 범위 조건
        if (searchRequest.getPromulgationDateStart() != null &&
                searchRequest.getPromulgationDateEnd() != null) {
            builder.and(law.getPromulgationDate().between(
                    searchRequest.getPromulgationDateStart(),
                    searchRequest.getPromulgationDateEnd()));
        } else if (searchRequest.getPromulgationDateStart() != null) {
            builder.and(law.getPromulgationDate().goe(searchRequest.getPromulgationDateStart()));
        } else if (searchRequest.getPromulgationDateEnd() != null) {
            builder.and(law.getPromulgationDate().loe(searchRequest.getPromulgationDateEnd()));
        }

        Pageable pageable = PageRequest.of(searchRequest.getPageNumber(), searchRequest.getPageSize());

        // DTO 프로젝션 조회
        JPAQuery<LawsDto> query = queryFactory
                .select(Projections.constructor(
                        LawsDto.class,
                        law.getId(),
                        law.getLawName(),
                        law.getLawField(),
                        law.getMinistry(),
                        law.getPromulgationNumber(),
                        law.getPromulgationDate(),
                        law.getEnforcementDate()
                ))
                .from(law)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<LawsDto> content = query.fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(law.count())
                .from(law)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
