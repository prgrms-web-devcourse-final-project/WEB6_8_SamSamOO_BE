package com.ai.lawyer.domain.chatbot.service;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatLawDto;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatPrecedentDto;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatRequest;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatResponse;
import com.ai.lawyer.domain.chatbot.dto.ExtractionDto.KeywordExtractionDto;
import com.ai.lawyer.domain.chatbot.dto.ExtractionDto.TitleExtractionDto;
import com.ai.lawyer.domain.chatbot.entity.*;
import com.ai.lawyer.domain.chatbot.repository.*;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.global.qdrant.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    private final KeywordService keywordService;

    private final ChatRepository chatRepository;
    private final HistoryRepository historyRepository;
    private final KeywordRankRepository keywordRankRepository;
    private final ChatMemoryRepository chatMemoryRepository;
    private final MemberRepository memberRepository;
    private final ChatPrecedentRepository chatPrecedentRepository;
    private final ChatLawRepository chatLawRepository;

    @Value("${custom.ai.system-message}")
    private String systemMessageTemplate;
    @Value("${custom.ai.title-extraction}")
    private String titleExtraction;
    @Value("{$custom.ai.keyword-extraction}")
    private String keywordExtraction;

    // 핵심 로직
    // 멤버 조회 -> 벡터 검색 (판례, 법령) -> 프롬프트 생성 (시스템, 유저) -> 채팅 클라이언트 호출 (스트림) -> 응답 저장, 제목/키워드 추출
    public Flux<ChatResponse> sendMessage(Long memberId, ChatRequest chatChatRequestDto, Long roomId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다.")
        );

        // 벡터 검색 (판례, 법령)
        List<Document> similarCaseDocuments = qdrantService.searchDocument(chatChatRequestDto.getMessage(), "type", "판례");
        List<Document> similarLawDocuments = qdrantService.searchDocument(chatChatRequestDto.getMessage(), "type", "법령");

        // 판례와 법령 정보를 구분 있게 포맷팅
        String caseContext = formatting(similarCaseDocuments);
        String lawContext = formatting(similarLawDocuments);

        // 채팅방 조회 or 생성 -> 없으면 생성
        History history = getOrCreateRoom(member, roomId);

        // 메시지 기억 관리 (최대 10개)
        // 멀티턴 -> 10개까지 기억 이거 안하면 매번 처음부터 대화 (멍충한 AI)
        ChatMemory chatMemory = saveChatMemory(chatChatRequestDto, history);

        // 프롬프트 생성
        Prompt prompt = getPrompt(caseContext, lawContext, chatMemory, history);

        // 복잡하긴 한데 이게 제일 깔끔한듯
        return chatClient.prompt(prompt)
                .stream()
                .content()
                .collectList()
                .map(fullResponseList -> String.join("", fullResponseList))
                .doOnNext(fullResponse -> handlerTasks(chatChatRequestDto, history, fullResponse, chatMemory, similarCaseDocuments, similarLawDocuments)) // 응답이 완성되면 후처리 실행 (대화 저장, 키워드/제목 추출 등)
                .map(fullResponse -> ChatResponse(history, fullResponse, similarCaseDocuments, similarLawDocuments)  // 최종적으로 ChatResponse DTO 생성
                ).flux()
                .onErrorResume(throwable -> Flux.just(handleError(history)));  // 에러 발생 시 에러 핸들링 -> 재전송 유도
    }

    private ChatResponse ChatResponse(History history, String fullResponse, List<Document> cases, List<Document> laws) {

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
                .title(history.getTitle())
                .message(fullResponse)
                .precedent(precedentDto)
                .law(lawDto)
                .build();
    }

    private ChatMemory saveChatMemory(ChatRequest chatChatRequestDto, History history) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();
        chatMemory.add(String.valueOf(history.getHistoryId()), new UserMessage(chatChatRequestDto.getMessage()));
        return chatMemory;
    }

    private Prompt getPrompt(String caseContext, String lawContext, ChatMemory chatMemory, History history) {

        Map<String, Object> promptContext = new HashMap<>();
        promptContext.put("caseContext", caseContext);
        promptContext.put("lawContext", lawContext);

        // 시스템 메시지와 사용자 메시지 생성 가공
        PromptTemplate promptTemplate = new PromptTemplate(systemMessageTemplate);
        Message systemMessage = new SystemMessage(promptTemplate.create(promptContext).getContents());
        UserMessage userMessage = new UserMessage(chatMemory.get(history.getHistoryId().toString()).toString());
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return prompt;
    }

    private ChatResponse handleError(History history) {
        return ChatResponse.builder()
                .roomId(history.getHistoryId())
                .message("죄송합니다. 서비스 처리 중 오류가 발생했습니다. 요청을 다시 전송해 주세요.")
                .build();
    }

    private void handlerTasks(ChatRequest chatDto, History history, String fullResponse, ChatMemory chatMemory, List<Document> similarCaseDocuments, List<Document> similarLawDocuments) {

        // 메시지 기억 저장
        chatMemory.add(String.valueOf(history.getHistoryId()), new AssistantMessage(fullResponse));
        chatMemoryRepository.saveAll(String.valueOf(history.getHistoryId()), chatMemory.get(String.valueOf(history.getHistoryId())));

        // 채팅방 제목 설정 / 및 필터 (법과 관련 없는 질문)
        setHistoryTitle(chatDto, history, fullResponse);

        // 채팅 기록 저장
        saveChatWithDocuments(history, MessageType.USER, chatDto.getMessage(), similarCaseDocuments, similarLawDocuments);
        saveChatWithDocuments(history, MessageType.ASSISTANT, fullResponse, similarCaseDocuments, similarLawDocuments);

        // 키워드 추출 및 키워드 랭킹 저장 (법과 관련 없는 질문은 제외)
        if (!fullResponse.contains("해당 질문은 법률")) {
            extractAndUpdateKeywordRanks(chatDto.getMessage());
        }

    }

    private void extractAndUpdateKeywordRanks(String message) {
        KeywordExtractionDto keywordResponse = keywordService.keywordExtract(message, keywordExtraction, KeywordExtractionDto.class);

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

    private void setHistoryTitle(ChatRequest chatDto, History history, String fullResponse) {
        String targetText = fullResponse.contains("해당 질문은 법률") ? chatDto.getMessage() : fullResponse;
        TitleExtractionDto titleDto = keywordService.keywordExtract(targetText, titleExtraction, TitleExtractionDto.class);
        history.setTitle(titleDto.getTitle());
        historyRepository.save(history);
    }

    private void saveChatWithDocuments(History history, MessageType type, String message, List<Document> similarCaseDocuments, List<Document> similarLawDocuments) {
        Chat chat = chatRepository.save(Chat.builder()
                .historyId(history)
                .type(type)
                .message(message)
                .build());

        if (type == MessageType.USER && similarCaseDocuments != null) {
            List<ChatPrecedent> chatPrecedents = similarCaseDocuments.stream()
                    .map(doc -> ChatPrecedent.builder()
                            .chatId(chat)
                            .precedentContent(doc.getText())
                            .caseNumber(doc.getMetadata().get("caseNumber").toString())
                            .caseName(doc.getMetadata().get("caseName").toString())
                            .build())
                    .toList();
            chatPrecedentRepository.saveAll(chatPrecedents);

            List<ChatLaw> chatLaws = similarLawDocuments.stream()
                    .map(doc -> ChatLaw.builder()
                            .chatId(chat)
                            .content(doc.getText())
                            .lawName(doc.getMetadata().get("lawName").toString())
                            .build())
                    .toList();

            chatLawRepository.saveAll(chatLaws);
        }

    }

    private History getOrCreateRoom(Member member, Long roomId) {
        if (roomId != null) {
            return historyService.getHistory(roomId);
        } else {
            return historyRepository.save(History.builder().memberId(member).build());
        }
    }

    private String formatting(List<Document> similarCaseDocuments) {
        String context = similarCaseDocuments.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n---\n\n"));
        return context;
    }

}