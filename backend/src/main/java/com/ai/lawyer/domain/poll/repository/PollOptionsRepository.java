package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.PollOptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionsRepository extends JpaRepository<PollOptions, Long> {
    List<PollOptions> findByPoll_PollId(Long pollId);
}
