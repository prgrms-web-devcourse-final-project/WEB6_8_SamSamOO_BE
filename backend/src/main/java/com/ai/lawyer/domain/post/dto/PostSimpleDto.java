package com.ai.lawyer.domain.post.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostSimpleDto {
    private Long postId;
    private Long memberId;
    private PollInfo poll;

    @Data
    @Builder
    public static class PollInfo {
        private Long pollId;
        private String pollStatus;
    }
}

