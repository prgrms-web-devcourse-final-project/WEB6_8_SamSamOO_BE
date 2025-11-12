package com.ai.lawyer.domain.poll.event;

import com.ai.lawyer.domain.poll.dto.PollVoteDto;

public class PollVotedEvent {
    private final Long pollId;
    private final PollVoteDto voteDto;

    public PollVotedEvent(Long pollId, PollVoteDto voteDto) {
        this.pollId = pollId;
        this.voteDto = voteDto;
    }

    public Long getPollId() {
        return pollId;
    }

    public PollVoteDto getVoteDto() {
        return voteDto;
    }
}