package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRepository extends JpaRepository<Poll, Long> {}
public interface PollOptionsRepository extends JpaRepository<PollOptions, Long> {}
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {}
public interface PollStaticsRepository extends JpaRepository<PollStatics, Long> {}