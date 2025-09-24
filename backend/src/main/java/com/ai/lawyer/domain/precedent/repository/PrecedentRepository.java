package com.ai.lawyer.domain.precedent.repository;

import com.ai.lawyer.domain.precedent.entity.Precedent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrecedentRepository extends JpaRepository<Precedent, Long>, PrecedentRepositoryCustom {

    /**
     * 판례일련번호로 존재 여부 확인
     */
    boolean existsByPrecedentNumber(String precedentNumber);
}
