package com.ai.lawyer.domain.precedent.repository;

import com.ai.lawyer.domain.precedent.entity.Precedent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrecedentRepository extends JpaRepository<Precedent, Long>, PrecedentRepositoryCustom {

    @Query(value = """
        SELECT 
            id, 
            case_name, 
            case_number, 
            sentencing_date,
            COALESCE(
                NULLIF(summary_of_the_judgment, ''),
                NULLIF(notice, ''),
                NULLIF(precedent_content, ''),
                ''
            ) AS contents,
            MATCH(notice, summary_of_the_judgment, precedent_content, case_name, case_number)
                AGAINST (:keyword IN BOOLEAN MODE) AS relevance
        FROM precedent
        WHERE 
            (:keyword IS NULL OR :keyword = '' OR
             MATCH(notice, summary_of_the_judgment, precedent_content, case_name, case_number)
             AGAINST (:keyword IN BOOLEAN MODE) > 0)
          AND (:startDate IS NULL OR sentencing_date >= :startDate)
          AND (:endDate IS NULL OR sentencing_date <= :endDate)
        ORDER BY relevance DESC, sentencing_date DESC
        LIMIT :offset, :pageSize
        """, nativeQuery = true)
    List<Object[]> searchByKeywordNative(
            @Param("keyword") String keyword, // "절도*" 형식
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM precedent
        WHERE 
            (:keyword IS NULL OR :keyword = '' OR
             MATCH(notice, summary_of_the_judgment, precedent_content, case_name, case_number)
             AGAINST (:keyword IN BOOLEAN MODE) > 0)
          AND (:startDate IS NULL OR sentencing_date >= :startDate)
          AND (:endDate IS NULL OR sentencing_date <= :endDate)
        """, nativeQuery = true)
    Long countByKeywordNative(
            @Param("keyword") String keyword,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
