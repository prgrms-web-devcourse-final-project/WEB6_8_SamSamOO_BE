package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import com.ai.lawyer.domain.poll.dto.PollUpdateDto;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.PollStatics;
import com.ai.lawyer.domain.poll.entity.PollOptions;

import java.util.List;

public interface PollService {
    PollDto getPoll(Long pollId);
    List<PollOptions> getPollOptions(Long pollId);
    PollVoteDto vote(Long pollId, Long pollItemsId, Long memberId);
    List<PollStatics> getPollStatics(Long pollId);
    void closePoll(Long pollId);
    void deletePoll(Long pollId);
    PollDto getTopPollByStatus(PollDto.PollStatus status);
    Long getVoteCountByPollId(Long pollId);
    Long getVoteCountByPostId(Long postId);
    PollDto updatePoll(Long pollId, PollUpdateDto pollUpdateDto);
    PollDto getPollWithStatistics(Long pollId);
    PollDto createPoll(PollCreateDto request, Long memberId);
    void patchUpdatePoll(Long pollId, PollUpdateDto pollUpdateDto);
}