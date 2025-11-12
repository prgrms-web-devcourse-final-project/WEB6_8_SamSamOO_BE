package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface PollRepositoryCustom {
    Optional<Poll> findByPost(Post post);
    List<Poll> findTopPollsByVoteCount(Pageable pageable);
}

