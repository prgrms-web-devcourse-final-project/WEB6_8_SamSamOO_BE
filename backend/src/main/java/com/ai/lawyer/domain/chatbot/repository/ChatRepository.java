package com.ai.lawyer.domain.chatbot.repository;

import com.ai.lawyer.domain.chatbot.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
}
