package com.ai.lawyer.global.qdrant.repository;

import com.ai.lawyer.global.qdrant.entity.Qdrent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QdrantRepository extends JpaRepository<Qdrent, Long> {
}
