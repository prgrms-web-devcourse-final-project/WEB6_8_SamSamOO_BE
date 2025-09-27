package com.ai.lawyer.domain.chatbot.service;

import com.ai.lawyer.domain.chatbot.entity.KeywordRank;
import com.ai.lawyer.domain.chatbot.repository.KeywordRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRankRepository keywordRepository;

    public List<KeywordRank> getTop5KeywordRanks() {
        return keywordRepository.findTop5ByOrderByScoreDesc();
    }

}
