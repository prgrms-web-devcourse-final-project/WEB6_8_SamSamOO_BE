package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long>, PollRepositoryCustom {
    Optional<Poll> findByPost(Post post);
}