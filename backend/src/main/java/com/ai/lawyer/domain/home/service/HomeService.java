package com.ai.lawyer.domain.home.service;

import com.ai.lawyer.domain.chatbot.repository.ChatRepository;
import com.ai.lawyer.domain.home.dto.FullData;
import com.ai.lawyer.domain.law.repository.LawRepository;
import com.ai.lawyer.domain.poll.repository.PollRepository;
import com.ai.lawyer.domain.precedent.repository.PrecedentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final PrecedentRepository precedentRepository;
    private final LawRepository lawRepository;
    private final ChatRepository chatRepository;
    private final PollRepository pollRepository;


    public FullData getDataCount() {

        Long precedentCount = precedentRepository.count();
        Long lawCount = lawRepository.count();
        Long chatCount = chatRepository.count();
        Long voteCount = pollRepository.count();

        return FullData.builder()
                .precedentCount(precedentCount)
                .lawCount(lawCount)
                .chatCount(chatCount)
                .voteCount(voteCount)
                .build();
        
    }

}
