package com.ai.lawyer.domain.post.dto;

import com.ai.lawyer.domain.poll.dto.PollDto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDetailDto {

    private PostDto post;
    private PollDto poll;  // 추후추가
}