package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.History;
import com.ai.lawyer.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    List<History> findAllByMemberId(Member memberId);

    History findByHistoryIdAndMemberId(Long roomId, Member memberId);

    /**
     * member_id로 채팅 히스토리 삭제 (회원 탈퇴 시 사용)
     * Member와 OAuth2Member 모두 같은 member_id 공간을 사용하므로 Long 타입으로 삭제
     */
    @Modifying
    @Query("DELETE FROM History h WHERE h.memberId.memberId = :memberId")
    void deleteByMemberIdValue(@Param("memberId") Long memberId);

}