package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.poll.entity.PollVote;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PollVoteRepositoryCustom {
    List<Object[]> findTopPollByStatus(Poll.PollStatus status);
    List<Object[]> findTopNPollByStatus(Poll.PollStatus status, Pageable pageable);
    Long countByPollId(Long pollId);
    Long countByPollOptionId(Long pollOptionId);
    List<Object[]> countStaticsByPollOptionIds(List<Long> pollOptionIds);
    List<Object[]> getOptionAgeStatics(Long pollId);
    List<Object[]> getOptionGenderStatics(Long pollId);
}
