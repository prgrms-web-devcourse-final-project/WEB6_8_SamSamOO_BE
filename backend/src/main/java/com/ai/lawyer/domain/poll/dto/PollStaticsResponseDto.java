package com.ai.lawyer.domain.poll.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollStaticsResponseDto {
    private Long postId;
    private Long pollId;
    private List<PollAgeStaticsDto> optionAgeStatics;
    private List<PollGenderStaticsDto> optionGenderStatics;
    private Long totalVoteCount;
}
