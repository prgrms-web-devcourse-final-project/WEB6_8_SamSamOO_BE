package com.ai.lawyer.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.MessageType;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "SPRING_AI_CHAT_MEMORY",
       indexes = @Index(name = "SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX", columnList = "conversation_id, timestamp"))
public class ChatMemory {

    @EmbeddedId
    private ChatMemoryId id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type;

}