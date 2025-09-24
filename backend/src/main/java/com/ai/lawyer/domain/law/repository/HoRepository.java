package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.entity.Ho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoRepository extends JpaRepository<Ho, Long> {
}
