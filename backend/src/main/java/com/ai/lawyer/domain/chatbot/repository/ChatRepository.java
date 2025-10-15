package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * member_id에 해당하는 모든 Chat 삭제 (회원 탈퇴 시 사용)
     */
    @Modifying
    @Query("DELETE FROM Chat c WHERE c.historyId.memberId.memberId = :memberId")
    void deleteByMemberIdValue(@Param("memberId") Long memberId);
}
