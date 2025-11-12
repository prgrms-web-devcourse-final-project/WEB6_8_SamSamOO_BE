package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.entity.Jang;
import com.ai.lawyer.domain.law.entity.Law;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JangRepository extends JpaRepository<Jang, Long> {
    // Jang + Jo만 페치
    @EntityGraph(attributePaths = "joList")
    List<Jang> findByLawId(Long lawId);

    List<Jang> findByLaw(Law law);
}
