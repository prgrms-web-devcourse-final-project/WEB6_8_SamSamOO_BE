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
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "member_id")  // 회원 ID 추후 추가
    private Long memberId;

    @Column(name = "post_name", length = 100, nullable = false)
    private String postName;

    @Column(name = "post_content", columnDefinition = "TEXT")
    private String postContent;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true) // 포스트 삭제 시 연관된 투표도 삭제
    @JoinColumn(name = "poll_id")
    private Poll poll;
}