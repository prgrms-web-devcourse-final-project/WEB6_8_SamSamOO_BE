package com.ai.lawyer.infrastructure.redis.service;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatHistoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatCacheService {

    private final RedisTemplate<String, ChatHistoryDto> chatRedisTemplate;
    private static final String CHAT_HISTORY_KEY_PREFIX = "chat:history:";

    // 채팅 메시지 캐싱 (24시간)
    public void cacheChatMessage(Long roomId, ChatHistoryDto chatHistory) {
        String key = CHAT_HISTORY_KEY_PREFIX + roomId;
        chatRedisTemplate.opsForList().rightPush(key, chatHistory);
        chatRedisTemplate.expire(key, Duration.ofHours(24));
    }

    public List<ChatHistoryDto> getChatHistory(Long roomId) {
        String key = CHAT_HISTORY_KEY_PREFIX + roomId;
        List<ChatHistoryDto> cachedList = chatRedisTemplate.opsForList().range(key, 0, -1);
        return cachedList == null ? List.of() : cachedList;
    }

    public void clearChatHistory(Long roomId) {
        chatRedisTemplate.delete(CHAT_HISTORY_KEY_PREFIX + roomId);
    }

}
