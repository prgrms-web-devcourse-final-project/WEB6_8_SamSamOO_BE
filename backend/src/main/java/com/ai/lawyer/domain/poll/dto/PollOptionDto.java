package com.ai.lawyer.domain.poll.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionDto {
    private Long pollOptionId;
    private String content;
}

