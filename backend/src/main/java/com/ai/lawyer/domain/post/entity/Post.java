package com.ai.lawyer.domain.post.entity;

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

    // Member와 OAuth2Member 모두 지원하기 위해 FK 제약 조건 제거 (ConstraintMode.NO_CONSTRAINT)
    // member_id를 직접 저장하고, 애플리케이션 레벨에서 AuthUtil로 참조 무결성 보장
    @Column(name = "member_id")
    private Long memberId;

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