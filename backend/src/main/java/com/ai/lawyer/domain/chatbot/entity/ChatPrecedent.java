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
@Table(name = "chat_precedent")
public class ChatPrecedent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatPrecedentId;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private Chat chatId;

    @Lob
    private String precedentContent;

    private String caseNumber;

    private String caseName;

}
