package com.ai.lawyer.domain.post.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {
    private String postName;
    private String postContent;
    private String category;
}

