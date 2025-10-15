package com.ai.lawyer.domain.chatbot.service;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatHistoryDto;
import com.ai.lawyer.domain.chatbot.dto.HistoryDto;
import com.ai.lawyer.domain.chatbot.entity.Chat;
import com.ai.lawyer.domain.chatbot.entity.History;
import com.ai.lawyer.domain.chatbot.exception.HistoryNotFoundException;
import com.ai.lawyer.domain.chatbot.repository.HistoryRepository;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.infrastructure.redis.service.ChatCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ChatCacheService chatCacheService;

    private final HistoryRepository historyRepository;
    private final MemberRepository memberRepository;

    public List<HistoryDto> getHistoryTitle(Long memberId) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        List<History> rooms = historyRepository.findAllByMemberId(member);
        List<HistoryDto> roomDtos = new ArrayList<>();

        for (History room : rooms)
            roomDtos.add(HistoryDto.from(room));

        return roomDtos;
    }

    public String deleteHistory(Long memberId, Long roomId) {

        getHistory(roomId);

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        History room = historyRepository.findByHistoryIdAndMemberId(roomId, member);

        historyRepository.delete(room);
        chatCacheService.clearChatHistory(roomId);

        return "채팅방이 삭제되었습니다.";

    }

    @Transactional(readOnly = true)
    public ResponseEntity<List<ChatHistoryDto>> getChatHistory(Long memberId, Long roomId) {

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        // 1. Redis 캐시에서 조회 (있으면 바로 반환)
        List<ChatHistoryDto> cached = chatCacheService.getChatHistory(roomId);
        if (!cached.isEmpty()) {
            return ResponseEntity.ok(cached);
        }

        // 2. DB에서 조회 후 캐시에 저장
        History history = historyRepository.findByHistoryIdAndMemberId(roomId, member);
        List<Chat> chats = history.getChats();

        // 엔티티 -> DTO 변환
        List<ChatHistoryDto> chatDtos = chats.stream()
                .map(ChatHistoryDto::from)
                .toList();

        // DB 조회 결과를 Redis 캐시에 저장
        chatDtos.forEach(dto -> chatCacheService.cacheChatMessage(roomId, dto));

        return ResponseEntity.ok(chatDtos);

    }

    public History getHistory(Long roomId) {
        return historyRepository.findById(roomId).orElseThrow(
                () -> new HistoryNotFoundException(roomId)
        );
    }

}