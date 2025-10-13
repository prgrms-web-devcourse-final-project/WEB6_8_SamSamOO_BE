package com.ai.lawyer.domain.post.entity;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.poll.entity.Poll;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "postId")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true, foreignKey = @ForeignKey(name = "FK_POST_MEMBER"))
    private Member member;

    @Column(name = "post_name", length = 100, nullable = false)
    private String postName;

    @Column(name = "post_content", columnDefinition = "TEXT")
    private String postContent;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "poll_id", foreignKey = @ForeignKey(name = "FK_POST_POLL"))
    private Poll poll;
}