package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.entity.Jang;
import com.ai.lawyer.domain.law.entity.Jo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JoRepository extends JpaRepository<Jo, Long> {

    // Jo + Hang만 페치
    @EntityGraph(attributePaths = "hangList")
    List<Jo> findByJangId(Long jangId);

    List<Jo> findByJang(Jang jang);
}
