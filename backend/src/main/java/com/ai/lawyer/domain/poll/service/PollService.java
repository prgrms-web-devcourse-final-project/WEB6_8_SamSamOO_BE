package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import com.ai.lawyer.domain.poll.dto.PollUpdateDto;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.PollStatics;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.dto.PollForPostDto;
import com.ai.lawyer.domain.poll.dto.PollStaticsResponseDto;

import java.util.List;

public interface PollService {
    PollDto getPoll(Long pollId);
    List<PollOptions> getPollOptions(Long pollId);
    PollVoteDto vote(Long pollId, Long pollItemsId, Long memberId);
    PollStaticsResponseDto getPollStatics(Long pollId);
    void closePoll(Long pollId);
    void deletePoll(Long pollId);
    PollDto getTopPollByStatus(PollDto.PollStatus status);
    Long getVoteCountByPollId(Long pollId);
    Long getVoteCountByPostId(Long postId);
    PollDto updatePoll(Long pollId, PollUpdateDto pollUpdateDto);
    PollDto getPollWithStatistics(Long pollId);
    PollDto createPoll(PollCreateDto request, Long memberId);
    void patchUpdatePoll(Long pollId, PollUpdateDto pollUpdateDto);
    List<PollDto> getPollsByStatus(PollDto.PollStatus status);
    List<PollDto> getTopNPollsByStatus(PollDto.PollStatus status, int n);
    void validatePollCreate(PollCreateDto dto);
    void validatePollCreate(PollForPostDto dto);
}