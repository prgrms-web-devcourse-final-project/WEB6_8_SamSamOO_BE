package com.ai.lawyer.domain.chatbot.controller;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatRequest;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatResponse;
import com.ai.lawyer.domain.chatbot.service.ChatBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@Slf4j
@Tag(name = "ChatBot API", description = "챗봇 관련 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatBotController {

    private final ChatBotService chatBotService;

    @Operation(summary = "새로운 채팅", description = "첫 메시지 전송으로 새로운 채팅방을 생성하고 챗봇과 대화를 시작")
    @PostMapping("/message")
    public ResponseEntity<Flux<ChatResponse>> postNewMessage(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(chatBotService.sendMessage(memberId, chatRequest, null));
    }

    @Operation(summary = "기존 채팅", description = "기존 채팅방에 메시지를 보내고 챗봇과 대화를 이어감")
    @PostMapping("{roomId}/message")
    public ResponseEntity<Flux<ChatResponse>> postMessage(@AuthenticationPrincipal Long memberId, @RequestBody ChatRequest chatRequest, @PathVariable(value = "roomId", required = false) Long roomId) {
        return ResponseEntity.ok(chatBotService.sendMessage(memberId, chatRequest, roomId));
    }

}