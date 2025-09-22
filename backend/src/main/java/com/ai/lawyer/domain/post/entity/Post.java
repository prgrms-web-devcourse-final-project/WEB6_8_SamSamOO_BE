package com.ai.lawyer.domain.post.entity;

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
}