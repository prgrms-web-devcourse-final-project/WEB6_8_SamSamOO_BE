package com.ai.lawyer.domain.post.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {

    private Long postId;
    private Long memberId;
    private String postName;
    private String postContent;
    private String category;
    private LocalDateTime createdAt;
}