package com.ai.lawyer.domain.law.repository;

import com.ai.lawyer.domain.law.entity.Hang;
import com.ai.lawyer.domain.law.entity.Ho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoRepository extends JpaRepository<Ho, Long> {
    List<Ho> findByHang(Hang hang);
}
