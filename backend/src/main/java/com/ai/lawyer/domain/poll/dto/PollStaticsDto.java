package com.ai.lawyer.domain.poll.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollStaticsDto {
    private String gender;
    private String ageGroup;
    private Long voteCount;
}

