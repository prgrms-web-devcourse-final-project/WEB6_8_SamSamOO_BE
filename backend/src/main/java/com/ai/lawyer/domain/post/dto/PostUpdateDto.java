package com.ai.lawyer.domain.post.dto;

import com.ai.lawyer.domain.poll.dto.PollUpdateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostUpdateDto {
    private String postName;
    private String postContent;
    private String category;
    private PollUpdateDto poll;
}
