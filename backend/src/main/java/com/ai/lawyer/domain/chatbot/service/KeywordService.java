package com.ai.lawyer.domain.chatbot.service;

import com.ai.lawyer.domain.chatbot.entity.KeywordRank;
import com.ai.lawyer.domain.chatbot.repository.KeywordRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final ChatClient chatClient;

    private final KeywordRankRepository keywordRepository;

    public List<KeywordRank> getTop5KeywordRanks() {
        return keywordRepository.findTop5ByOrderByScoreDesc();
    }

    // 키워드 추출 메서드
    public <T> T keywordExtract(String content, String promptTemplate, Class<T> classType) {
        String prompt = promptTemplate + content;
        return chatClient.prompt(new Prompt(new UserMessage(prompt)))
                .call()
                .entity(classType);
    }

}
