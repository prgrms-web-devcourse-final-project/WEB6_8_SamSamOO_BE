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
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(length = 100, nullable = false, name = "option")
    private String option;

    @Column(name = "count")
    private Long count;
}