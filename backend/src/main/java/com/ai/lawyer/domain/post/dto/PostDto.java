package com.ai.lawyer.domain.post.dto;

import com.ai.lawyer.domain.poll.dto.PollDto;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class  PostDto {

    private Long postId;
    private Long memberId;
    private String postName;
    private String postContent;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PollDto poll;
}