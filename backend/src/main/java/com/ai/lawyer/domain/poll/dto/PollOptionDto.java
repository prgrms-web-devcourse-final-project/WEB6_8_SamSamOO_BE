package com.ai.lawyer.domain.poll.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionDto {
    private Long pollItemsId;
    private String content;
    private Long voteCount;
    private java.util.List<PollStaticsDto> statics;
    private int pollOptionIndex;
    private boolean voted; // 해당 옵션에 투표했는지 여부
}
