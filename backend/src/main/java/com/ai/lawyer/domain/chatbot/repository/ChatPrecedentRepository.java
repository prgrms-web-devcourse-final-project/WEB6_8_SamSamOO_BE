package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.ChatPrecedent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPrecedentRepository extends JpaRepository<ChatPrecedent, Long> {
}
