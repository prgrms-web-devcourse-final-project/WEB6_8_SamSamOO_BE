package com.ai.lawyer.domain.poll.entity;

import com.ai.lawyer.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_vote")
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_items_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLVOTE_POLLOPTIONS"))
    private PollOptions pollOptions;
}