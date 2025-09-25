package com.ai.lawyer.domain.poll.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PollDto {
    private Long pollId;
    private Long postId;
    private String voteTitle;
    private PollStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private List<PollOptionDto> pollOptions;

    public enum PollStatus {
        ONGOING, CLOSED
    }
}
