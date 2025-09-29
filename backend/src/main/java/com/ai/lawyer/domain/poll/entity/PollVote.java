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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLVOTE_MEMBER"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_items_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLVOTE_POLLOPTIONS"))
    private PollOptions pollOptions;
}