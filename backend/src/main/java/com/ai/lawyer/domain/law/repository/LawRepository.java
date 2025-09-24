package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.entity.Law;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LawRepository extends JpaRepository<Law, Long>, LawRepositoryCustom {

    // Law + Jang만 페치
    @EntityGraph(attributePaths = "jangList")
    Optional<Law> findWithJangById(Long id);
}
