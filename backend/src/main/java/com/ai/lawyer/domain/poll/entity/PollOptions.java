package com.ai.lawyer.domain.poll.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poll_items_id")
    private Long pollItemsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLOPTIONS_POLL"))
    private Poll poll;

    @Column(length = 100, nullable = false, name = "option_text")
    private String option;

    @Column(name = "count")
    private Long count;

    @OneToMany(mappedBy = "pollOptions", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<PollVote> pollVotes;
}