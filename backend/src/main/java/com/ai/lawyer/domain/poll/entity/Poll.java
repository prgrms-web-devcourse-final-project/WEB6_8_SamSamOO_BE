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

    @OneToOne(mappedBy = "poll", fetch = FetchType.LAZY)
    private Post post;

    @Column(length = 100, nullable = false, name = "vote_title")
    private String voteTitle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, name = "status")
    private PollStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "reserved_close_at")
    private LocalDateTime reservedCloseAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<PollOptions> pollOptions;

    // 투표 상태 Enum 타입
    public enum PollStatus {
        ONGOING, CLOSED
    }
}
