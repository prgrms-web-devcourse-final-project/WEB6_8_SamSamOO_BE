package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.ChatLaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatLawRepository extends JpaRepository<ChatLaw, Long> {

    /**
     * member_id에 해당하는 모든 ChatLaw 삭제 (회원 탈퇴 시 사용)
     */
    @Modifying
    @Query("DELETE FROM ChatLaw cl WHERE cl.chatId.historyId.memberId.memberId = :memberId")
    void deleteByMemberIdValue(@Param("memberId") Long memberId);
}
