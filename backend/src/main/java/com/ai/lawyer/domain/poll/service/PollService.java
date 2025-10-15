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
    // ===== 조회 관련 =====
    PollDto getPoll(Long pollId, Long memberId);
    PollDto getPollWithStatistics(Long pollId, Long memberId);
    List<PollOptions> getPollOptions(Long pollId);
    List<PollDto> getPollsByStatus(PollDto.PollStatus status, Long memberId);
    PollDto getTopPollByStatus(PollDto.PollStatus status, Long memberId);
    List<PollDto> getTopNPollsByStatus(PollDto.PollStatus status, int n, Long memberId);

    // ===== 통계 관련 =====
    PollStaticsResponseDto getPollStatics(Long pollId);
    Long getVoteCountByPollId(Long pollId);
    Long getVoteCountByPostId(Long postId);

    // ===== 투표 관련 =====
    PollVoteDto vote(Long pollId, Long pollItemsId, Long memberId);

    // ===== 투표 취소 관련 =====
    void cancelVote(Long pollId, Long memberId);

    // ===== 생성/수정/삭제 관련 =====
    PollDto createPoll(PollCreateDto request, Long memberId);
    PollDto updatePoll(Long pollId, PollUpdateDto pollUpdateDto, Long memberId);
    void patchUpdatePoll(Long pollId, PollUpdateDto pollUpdateDto);
    void closePoll(Long pollId);
     void deletePoll(Long pollId, Long memberId);

    // ===== 검증 관련 =====
    void validatePollCreate(PollCreateDto dto);
    void validatePollCreate(PollForPostDto dto);
}