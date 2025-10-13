package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long>, PollVoteRepositoryCustom {
    Optional<PollVote> findByMember_MemberIdAndPoll_PollId(Long memberId, Long pollId);
    void deleteByMember_MemberIdAndPoll_PollId(Long memberId, Long pollId);
    List<PollVote> findByMember_MemberIdAndPollOptions_PollItemsId(Long memberId, Long pollItemsId);
    List<PollVote> findByMember_MemberId(Long memberId);

    /**
     * member_id로 투표 내역 삭제 (회원 탈퇴 시 사용)
     * Member와 OAuth2Member 모두 같은 member_id 공간을 사용하므로 Long 타입으로 삭제
     */
    @Modifying
    @Query("DELETE FROM PollVote pv WHERE pv.member.memberId = :memberId")
    void deleteByMemberIdValue(@Param("memberId") Long memberId);

    boolean existsByPollAndMember(Poll poll, Member member);

    @Query("SELECT v.member.memberId FROM PollVote v WHERE v.poll = :poll")
    List<Long> findMemberIdsByPoll(@Param("poll") Poll poll);
}
