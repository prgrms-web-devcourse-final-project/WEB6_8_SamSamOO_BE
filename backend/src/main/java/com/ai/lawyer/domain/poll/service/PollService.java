package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.PollStatics;
import com.ai.lawyer.domain.poll.entity.PollOptions;

import java.util.List;

public interface PollService {
    PollDto getPoll(Long pollId);
    List<PollOptions> getPollOptions(Long pollId);
    PollVote vote(Long pollId, Long pollItemsId, Long memberId);
    List<PollStatics> getPollStatics(Long pollId);
    void closePoll(Long pollId);
    void deletePoll(Long pollId);
}