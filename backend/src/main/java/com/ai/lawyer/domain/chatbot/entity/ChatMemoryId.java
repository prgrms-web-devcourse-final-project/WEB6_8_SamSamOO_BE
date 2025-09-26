package com.ai.lawyer.domain.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDateTime;

@Embeddable
public class ChatMemoryId implements Serializable {

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

}