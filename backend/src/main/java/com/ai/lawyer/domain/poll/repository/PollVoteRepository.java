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
}
