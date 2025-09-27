package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.History;
import com.ai.lawyer.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    List<History> findAllByMemberId(Member memberId);

    History findByHistoryIdAndMemberId(Long roomId, Member memberId);

}