package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    @Query("SELECT v.poll.pollId, COUNT(v.pollVoteId) FROM PollVote v WHERE v.poll.status = :status GROUP BY v.poll.pollId ORDER BY COUNT(v.pollVoteId) DESC")
    List<Object[]> findTopPollByStatus(@Param("status") Poll.PollStatus status);

    @Query("SELECT COUNT(v.pollVoteId) FROM PollVote v WHERE v.poll.pollId = :pollId")
    Long countByPollId(@Param("pollId") Long pollId);

    @Query("SELECT COUNT(v.pollVoteId) FROM PollVote v WHERE v.pollOptions.pollItemsId = :pollOptionId")
    Long countByPollOptionId(@Param("pollOptionId") Long pollOptionId);

    @Query("SELECT v.pollOptions.pollItemsId, v.member.gender, v.member.age, COUNT(v.pollVoteId) FROM PollVote v WHERE v.pollOptions.pollItemsId IN :pollOptionIds GROUP BY v.pollOptions.pollItemsId, v.member.gender, v.member.age")
    java.util.List<Object[]> countStaticsByPollOptionIds(@Param("pollOptionIds") java.util.List<Long> pollOptionIds);

    boolean existsByPoll_PollIdAndMember_MemberId(Long pollId, Long memberId);
}
