package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.poll.entity.PollVote;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.ai.lawyer.domain.poll.dto.PollAgeStaticsDto;
import com.ai.lawyer.domain.poll.dto.PollGenderStaticsDto;
import com.ai.lawyer.domain.poll.dto.PollTopDto;
import com.ai.lawyer.domain.poll.dto.PollStaticsDto;

public interface PollVoteRepositoryCustom {
    List<PollTopDto> findTopPollByStatus(Poll.PollStatus status);
    List<PollTopDto> findTopNPollByStatus(Poll.PollStatus status, Pageable pageable);
    Long countByPollId(Long pollId);
    Long countByPollOptionId(Long pollOptionId);
    List<PollAgeStaticsDto.AgeGroupCountDto> getOptionAgeStatics(Long pollId);
    List<PollGenderStaticsDto.GenderCountDto> getOptionGenderStatics(Long pollId);
    List<PollStaticsDto> countStaticsByPollOptionIds(List<Long> pollOptionIds);
}
