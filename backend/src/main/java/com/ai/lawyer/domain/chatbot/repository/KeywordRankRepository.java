package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.KeywordRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeywordRankRepository extends JpaRepository<KeywordRank, Long> {

    KeywordRank findByKeyword(String keyword);

    List<KeywordRank> findTop5ByOrderByScoreDesc();

}