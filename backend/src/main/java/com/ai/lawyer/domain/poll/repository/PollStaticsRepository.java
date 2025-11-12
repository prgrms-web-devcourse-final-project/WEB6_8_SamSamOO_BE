package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.PollStatics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollStaticsRepository extends JpaRepository<PollStatics, Long> {
    List<PollStatics> findByPoll_PollId(Long pollId);
}

