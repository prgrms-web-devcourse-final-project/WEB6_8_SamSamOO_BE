package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.dto.LawSearchRequestDto;
import com.ai.lawyer.domain.law.dto.LawsDto;
import com.ai.lawyer.domain.law.entity.QJang;
import com.ai.lawyer.domain.law.entity.QJo;
import com.ai.lawyer.domain.law.entity.QLaw;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import com.querydsl.core.types.dsl.StringTemplate;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LawRepositoryCustomImpl implements LawRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private QLaw law = QLaw.law;
    private QJang jang = QJang.jang;
    private QJo jo = QJo.jo;

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
                        law.getEnforcementDate(),
                        Expressions.nullExpression(String.class)
                ))
                .from(law)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<LawsDto> content = query.fetch();

        if (content.isEmpty()) {
            return new PageImpl<>(content, pageable, 0);
        }

        // 조회한 법령 ID 목록 추출
        List<Long> lawIds = content.stream()
                .map(LawsDto::getId)
                .toList();

        // 3. 법령별 첫 번째 조 내용 조회 - 반복문으로 각각 조회
        Map<Long, String> firstJoContentMap = new HashMap<>();
        for (Long lawId : lawIds) {
            String firstJoContent = queryFactory
                    .select(jo.getContent())
                    .from(jo)
                    .join(jo.getJang(), jang)
                    .where(jang.getLaw().getId().eq(lawId))
                    .orderBy(jang.getId().asc(), jo.getId().asc())
                    .limit(1)
                    .fetchOne();
            firstJoContentMap.put(lawId, firstJoContent);
        }

        // 4. 조회 결과에 firstJoContent 세팅
        content.forEach(dto -> {
            String contentVal = firstJoContentMap.get(dto.getId());
            dto.setFirstJoContent(contentVal != null ? contentVal : "");
        });

        // 전체 개수 조회
        Long total = queryFactory
                .select(law.count())
                .from(law)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
