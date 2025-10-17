package com.ai.lawyer.domain.poll.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_vote",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_poll_vote_member_poll",
        columnNames = {"member_id", "poll_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poll_vote_id")
    private Long pollVoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLVOTE_POLL"))
    private Poll poll;

    // Member와 OAuth2Member 모두 지원하기 위해 FK 제약 조건 제거
    // 애플리케이션 레벨에서 AuthUtil로 참조 무결성 보장
    // foreignKey 제약조건 비활성화 (ConstraintMode.NO_CONSTRAINT)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_items_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLVOTE_POLLOPTIONS"))
    private PollOptions pollOptions;
}