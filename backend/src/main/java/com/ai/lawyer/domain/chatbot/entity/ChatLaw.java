package com.ai.lawyer.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_law")
public class ChatLaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatLawId;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private Chat chatId;

    @Lob
    String content;

    String lawName;

}
