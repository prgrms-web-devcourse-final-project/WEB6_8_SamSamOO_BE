package com.ai.lawyer.domain.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVoteDto {
    private Long pollVoteId;
    private Long pollId;
    private Long pollItemsId;
    private Long memberId;
    private Long voteCount;
    private String message;
}
