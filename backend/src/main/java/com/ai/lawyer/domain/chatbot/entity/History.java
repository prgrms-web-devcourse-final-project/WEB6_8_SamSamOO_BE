package com.ai.lawyer.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "history")
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    // Member와 OAuth2Member 모두 지원하기 위해 FK 제약 조건 제거 (ConstraintMode.NO_CONSTRAINT)
    // member_id를 직접 저장하고, 애플리케이션 레벨에서 AuthUtil로 참조 무결성 보장
    @Column(name = "member_id")
    private Long memberId;

    @OneToMany(mappedBy = "historyId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chat> chats;

    private String title;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}