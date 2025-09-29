package com.ai.lawyer.domain.poll.dto;

import com.ai.lawyer.domain.post.dto.PostDto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollWithPostDto {
    private PollDto poll;
    private PostDto post;
}

