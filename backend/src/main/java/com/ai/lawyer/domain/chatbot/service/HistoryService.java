package com.ai.lawyer.domain.chatbot.service;

import com.ai.lawyer.domain.chatbot.dto.HistoryDto;
import com.ai.lawyer.domain.chatbot.entity.History;
import com.ai.lawyer.domain.chatbot.exception.HistoryNotFoundException;
import com.ai.lawyer.domain.chatbot.repository.HistoryRepository;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

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
        return "채팅방이 삭제되었습니다.";

    }

    public History getHistory(Long roomId) {
        return historyRepository.findById(roomId).orElseThrow(
                () -> new HistoryNotFoundException(roomId)
        );
    }

}