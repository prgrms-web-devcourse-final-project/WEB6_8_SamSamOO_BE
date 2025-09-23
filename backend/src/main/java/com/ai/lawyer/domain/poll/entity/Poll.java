package com.ai.lawyer.domain.poll.entity;

import com.ai.lawyer.domain.post.entity.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "poll")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poll_id")
    private Long pollId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = true)
    private Post post;

    @Column(length = 100, nullable = false, name = "vote_title")
    private String voteTitle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, name = "status")
    private PollStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // 투표 상태 Enum 타입
    public enum PollStatus {
        ONGOING, CLOSED
    }
}
