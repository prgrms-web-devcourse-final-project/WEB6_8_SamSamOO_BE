package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    Optional<Poll> findByPost(Post post);

    @Query("SELECT p FROM Poll p LEFT JOIN PollVote v ON p = v.poll GROUP BY p ORDER BY COUNT(v) DESC")
    List<Poll> findTopPollsByVoteCount(Pageable pageable);
}
