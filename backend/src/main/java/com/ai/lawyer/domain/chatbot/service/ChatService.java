package com.ai.lawyer.domain.chatbot.service;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatHistoryDto;
import com.ai.lawyer.domain.chatbot.entity.Chat;
import com.ai.lawyer.domain.chatbot.repository.HistoryRepository;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final HistoryRepository historyRepository;
    private final MemberRepository memberRepository;

    public ResponseEntity<List<ChatHistoryDto>> getChatHistory(Long memberId, Long roomId) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        List<Chat> chats = historyRepository.findByHistoryIdAndMemberId(roomId, member).getChats();
        List<ChatHistoryDto> chatDtos = new ArrayList<>();

        for (Chat chat : chats) {
            ChatHistoryDto dto = ChatHistoryDto.from(chat);
            chatDtos.add(dto);
        }

        return ResponseEntity.ok(chatDtos);

    }

}
