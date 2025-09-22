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
    private Long postId;

    private Long memberId;

    @Column(length = 100, nullable = false)
    private String postName;

    @Column(columnDefinition = "TEXT")
    private String postContent;

    @Column(length = 100)
    private String category;

    private LocalDateTime createdAt;
}