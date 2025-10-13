package com.ai.lawyer.domain.kafka.consumer;

import com.ai.lawyer.domain.chatbot.dto.ExtractionDto.KeywordExtractionDto;
import com.ai.lawyer.domain.chatbot.dto.ExtractionDto.TitleExtractionDto;
import com.ai.lawyer.domain.chatbot.entity.*;
import com.ai.lawyer.domain.chatbot.repository.*;
import com.ai.lawyer.domain.chatbot.service.KeywordService;
import com.ai.lawyer.domain.kafka.dto.ChatPostProcessEvent;
import com.ai.lawyer.domain.kafka.dto.DocumentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPostProcessingConsumer {

    private final KeywordService keywordService;
    private final HistoryRepository historyRepository;
    private final ChatRepository chatRepository;
    private final KeywordRankRepository keywordRankRepository;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ChatPrecedentRepository chatPrecedentRepository;
    private final ChatLawRepository chatLawRepository;

    @Value("${custom.ai.title-extraction}")
    private String titleExtraction;
    @Value("${custom.ai.keyword-extraction}")
    private String keywordExtraction;

    @KafkaListener(topics = "chat-post-processing", groupId = "chat-processing-group")
    @Transactional
    public void consume(ChatPostProcessEvent event) {
        try {
            History history = historyRepository.findById(event.getHistoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다. historyId: " + event.getHistoryId()));

            // 1. 메시지 기억 저장 (Assistant 응답)
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .maxMessages(10)
                    .chatMemoryRepository(chatMemoryRepository)
                    .build();

            chatMemory.add(String.valueOf(history.getHistoryId()), new AssistantMessage(event.getChatResponse()));
            chatMemoryRepository.saveAll(String.valueOf(history.getHistoryId()), chatMemory.get(String.valueOf(history.getHistoryId())));

            // 2. 채팅방 제목 설정 / 및 필터
            setHistoryTitle(event.getUserMessage(), history, event.getChatResponse());

            // 3. 채팅 기록 저장
            saveChatWithDocuments(history, MessageType.USER, event.getUserMessage(), event.getSimilarCaseDocuments(), event.getSimilarLawDocuments());
            saveChatWithDocuments(history, MessageType.ASSISTANT, event.getChatResponse(), event.getSimilarCaseDocuments(), event.getSimilarLawDocuments());

            // 4. 키워드 추출 및 랭킹 업데이트
            if (!event.getChatResponse().contains("해당 질문은 법률")) {
                extractAndUpdateKeywordRanks(event.getUserMessage());
            }
        } catch (Exception e) {
            log.error("Kafka 이벤트 처리 중 에러 발생 (historyId: {}): ", event.getHistoryId(), e);
        }
    }

    private void setHistoryTitle(String userMessage, History history, String fullResponse) {
        String targetText = fullResponse.contains("해당 질문은 법률") ? userMessage : fullResponse;
        TitleExtractionDto titleDto = keywordService.keywordExtract(targetText, titleExtraction, TitleExtractionDto.class);
        history.setTitle(titleDto.getTitle());
        historyRepository.save(history);
    }

    private void extractAndUpdateKeywordRanks(String message) {
        KeywordExtractionDto keywordResponse = keywordService.keywordExtract(message, keywordExtraction, KeywordExtractionDto.class);
        if (keywordResponse == null || keywordResponse.getKeyword() == null) {
            return;
        }

        KeywordRank keywordRank = keywordRankRepository.findByKeyword(keywordResponse.getKeyword());

        if (keywordRank == null) {
            keywordRank = KeywordRank.builder()
                    .keyword(keywordResponse.getKeyword())
                    .score(1L)
                    .build();
        } else {
            keywordRank.setScore(keywordRank.getScore() + 1);
        }
        keywordRankRepository.save(keywordRank);
    }

    private void saveChatWithDocuments(History history, MessageType type, String message, List<DocumentDto> similarCaseDocuments, List<DocumentDto> similarLawDocuments) {
        Chat chat = chatRepository.save(Chat.builder()
                .historyId(history)
                .type(type)
                .message(message)
                .build());

        // Ai 메시지가 저장될 때 관련 문서 저장
        if (type == MessageType.ASSISTANT) {
            if (similarCaseDocuments != null && !similarCaseDocuments.isEmpty()) {
                List<ChatPrecedent> chatPrecedents = similarCaseDocuments.stream()
                        .map(doc -> ChatPrecedent.builder()
                                .chatId(chat)
                                .precedentContent(doc.getText())
                                .caseNumber(doc.getMetadata().get("caseNumber").toString())
                                .caseName(doc.getMetadata().get("caseName").toString())
                                .build())
                        .collect(Collectors.toList());
                chatPrecedentRepository.saveAll(chatPrecedents);
            }

            if (similarLawDocuments != null && !similarLawDocuments.isEmpty()) {
                List<ChatLaw> chatLaws = similarLawDocuments.stream()
                        .map(doc -> ChatLaw.builder()
                                .chatId(chat)
                                .content(doc.getText())
                                .lawName(doc.getMetadata().get("lawName").toString())
                                .build())
                        .collect(Collectors.toList());
                chatLawRepository.saveAll(chatLaws);
            }
        }
    }
}