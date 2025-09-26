package com.ai.lawyer.domain.poll.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionDto {
    private Long pollItemsId; // PK
    private String content;
    private Long voteCount;
    private java.util.List<PollStaticsDto> statics;
    private int pollOptionIndex;
}
