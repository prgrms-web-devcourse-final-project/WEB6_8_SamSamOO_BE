package com.ai.lawyer.domain.chatbot.service;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatLawDto;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatPrecedentDto;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatRequest;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatResponse;
import com.ai.lawyer.domain.chatbot.entity.History;
import com.ai.lawyer.domain.chatbot.repository.HistoryRepository;
import com.ai.lawyer.domain.kafka.dto.ChatPostProcessEvent;
import com.ai.lawyer.domain.kafka.dto.DocumentDto;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.global.qdrant.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final ChatClient chatClient;
    private final QdrantService qdrantService;
    private final HistoryService historyService;
    private final MemberRepository memberRepository;
    private final HistoryRepository historyRepository;
    private final ChatMemoryRepository chatMemoryRepository;

    // KafkaTemplate 주입
    private final KafkaTemplate<String, ChatPostProcessEvent> kafkaTemplate;

    @Value("${custom.ai.system-message}")
    private String systemMessageTemplate;

    // Kafka 토픽 이름 -> 추후 application.yml로 이동 고려
    private static final String POST_PROCESSING_TOPIC = "chat-post-processing";

    // 핵심 로직
    // 멤버 조회 -> 벡터 검색 -> 프롬프트 생성 -> LLM 호출 (스트림) -> Kafka 이벤트 발행 -> 응답 반환
    @Transactional
    public Flux<ChatResponse> sendMessage(Long memberId, ChatRequest chatRequestDto, Long roomId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 벡터 검색 (판례, 법령)
        List<Document> similarCaseDocuments = qdrantService.searchDocument(chatRequestDto.getMessage(), "type", "판례");
        List<Document> similarLawDocuments = qdrantService.searchDocument(chatRequestDto.getMessage(), "type", "법령");

        String caseContext = formatting(similarCaseDocuments);
        String lawContext = formatting(similarLawDocuments);

        // 채팅방 조회 또는 생성
        History history = getOrCreateRoom(member, roomId);

        // 메시지 기억 관리 (User 메시지 추가)
        ChatMemory chatMemory = saveChatMemory(chatRequestDto, history);

        // 프롬프트 생성
        Prompt prompt = getPrompt(caseContext, lawContext, chatMemory, history);

        // LLM 스트리밍 호출 및 클라이언트에게 즉시 응답
        return chatClient.prompt(prompt)
                .stream()
                .content()
                .collectList()
                .map(fullResponseList -> String.join("", fullResponseList))
                .doOnNext(fullResponse -> {

                    // Document를 DTO로 변환
                    List<DocumentDto> caseDtos = similarCaseDocuments.stream().map(DocumentDto::from).collect(Collectors.toList());
                    List<DocumentDto> lawDtos = similarLawDocuments.stream().map(DocumentDto::from).collect(Collectors.toList());

                    // Kafka로 보낼 이벤트 객체
                    ChatPostProcessEvent event = new ChatPostProcessEvent(
                            history.getHistoryId(),
                            chatRequestDto.getMessage(),
                            fullResponse,
                            caseDtos,
                            lawDtos
                    );
                    
                    // Kafka 이벤트 발행
                    kafkaTemplate.send(POST_PROCESSING_TOPIC, event);

                })
                .map(fullResponse -> createChatResponse(history, fullResponse, similarCaseDocuments, similarLawDocuments))
                .flux()
                .onErrorResume(throwable -> {
                    log.error("스트리밍 처리 중 에러 발생 (historyId: {})", history.getHistoryId(), throwable);
                    return Flux.just(handleError(history));
                });
    }

    private ChatResponse createChatResponse(History history, String fullResponse, List<Document> cases, List<Document> laws) {
        ChatPrecedentDto precedentDto = null;
        if (cases != null && !cases.isEmpty()) {
            Document firstCase = cases.get(0);
            precedentDto = ChatPrecedentDto.from(firstCase);
        }

        ChatLawDto lawDto = null;
        if (laws != null && !laws.isEmpty()) {
            Document firstLaw = laws.get(0);
            lawDto = ChatLawDto.from(firstLaw);
        }

        return ChatResponse.builder()
                .roomId(history.getHistoryId())
                .message(fullResponse)
                .precedent(precedentDto)
                .law(lawDto)
                .build();
    }

    private ChatMemory saveChatMemory(ChatRequest chatRequestDto, History history) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();
        // 사용자 메시지를 메모리에 추가 -> ai 답변은 Consumer에서 추가
        chatMemory.add(String.valueOf(history.getHistoryId()), new UserMessage(chatRequestDto.getMessage()));
        return chatMemory;
    }

    private Prompt getPrompt(String caseContext, String lawContext, ChatMemory chatMemory, History history) {
        Map<String, Object> promptContext = new HashMap<>();
        promptContext.put("caseContext", caseContext);
        promptContext.put("lawContext", lawContext);

        PromptTemplate promptTemplate = new PromptTemplate(systemMessageTemplate);
        Message systemMessage = new SystemMessage(promptTemplate.create(promptContext).getContents());
        UserMessage userMessage = new UserMessage(chatMemory.get(history.getHistoryId().toString()).toString());

        return new Prompt(List.of(systemMessage, userMessage));
    }

    private History getOrCreateRoom(Member member, Long roomId) {
        if (roomId != null) {
            return historyService.getHistory(roomId);
        } else {
            return historyRepository.save(History.builder().memberId(member).build());
        }
    }

    private String formatting(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        return documents.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private ChatResponse handleError(History history) {
        return ChatResponse.builder()
                .roomId(history.getHistoryId())
                .message("죄송합니다. 서비스 처리 중 오류가 발생했습니다. 요청을 다시 전송해 주세요.")
                .build();
    }
}