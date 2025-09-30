package com.ai.lawyer.domain.post.dto;

import com.ai.lawyer.domain.poll.dto.PollForPostDto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostWithPollCreateDto {
    private PostRequestDto post;
    private PollForPostDto poll;
}
