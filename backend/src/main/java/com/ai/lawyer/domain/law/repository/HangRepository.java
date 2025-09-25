package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.entity.Hang;
import com.ai.lawyer.domain.law.entity.Jo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HangRepository extends JpaRepository<Hang, Long> {

    // Hang + Ho만 페치
    @EntityGraph(attributePaths = "hoList")
    List<Hang> findByJoId(Long joId);

    List<Hang> findByJo(Jo jo);
}
