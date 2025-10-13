package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.ChatPrecedent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatPrecedentRepository extends JpaRepository<ChatPrecedent, Long> {

    /**
     * member_id에 해당하는 모든 ChatPrecedent 삭제 (회원 탈퇴 시 사용)
     */
    @Modifying
    @Query("DELETE FROM ChatPrecedent cp WHERE cp.chatId.historyId.memberId.memberId = :memberId")
    void deleteByMemberIdValue(@Param("memberId") Long memberId);
}
