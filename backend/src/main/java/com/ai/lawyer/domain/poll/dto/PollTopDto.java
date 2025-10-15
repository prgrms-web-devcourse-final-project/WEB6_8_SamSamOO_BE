package com.ai.lawyer.domain.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollTopDto {
    private Long pollId;
    private Long voteCount;
}

