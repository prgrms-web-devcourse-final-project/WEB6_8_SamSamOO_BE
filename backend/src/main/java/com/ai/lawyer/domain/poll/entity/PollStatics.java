package com.ai.lawyer.domain.poll.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_statics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollStatics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id")
    private Long statId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLSTATICS_POLL"))
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_items_id", nullable = false, foreignKey = @ForeignKey(name = "FK_POLLSTATICS_POLLOPTIONS"))
    private PollOptions pollOptions;

    @Column(length = 10, name = "gender")
    private String gender;

    @Column(length = 20, name = "age_group")
    private String ageGroup;

    @Column(name = "count")
    private Long count;
}