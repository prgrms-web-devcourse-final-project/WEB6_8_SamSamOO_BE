package com.ai.lawyer.domain.chatbot.controller;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatRequest;
import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatResponse;
import com.ai.lawyer.domain.chatbot.service.ChatBotService;
import com.ai.lawyer.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@Tag(name = "ChatBot API", description = "챗봇 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatBotController {

    private final ChatBotService chatBotService;

    @Operation(summary = "01. 새로운 채팅", description = "첫 메시지 전송으로 새로운 채팅방을 생성하고 챗봇과 대화를 시작")
    @PostMapping(value = "/message")
    public Flux<ChatResponse> postNewMessage(@RequestBody ChatRequest chatRequest) {
        // SecurityContext에서 memberId를 미리 추출 (컨트롤러 진입 시점)
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        if (memberId == null) {
            throw new IllegalStateException("인증된 사용자가 아닙니다.");
        }
        log.info("새로운 채팅 요청: memberId={}", memberId);

        // memberId를 Flux에 전달 (SecurityContext 전파 문제 방지)
        return chatBotService.sendMessage(memberId, chatRequest, null);
    }

    @Operation(summary = "02. 기존 채팅", description = "기존 채팅방에 메시지를 보내고 챗봇과 대화를 이어감")
    @PostMapping(value = "{roomId}/message")
    public Flux<ChatResponse> postMessage(
            @RequestBody ChatRequest chatRequest,
            @PathVariable(value = "roomId", required = false) Long roomId) {
        // SecurityContext에서 memberId를 미리 추출 (컨트롤러 진입 시점)
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        log.info("기존 채팅 요청: memberId={}, roomId={}", memberId, roomId);

        // memberId를 Flux에 전달 (SecurityContext 전파 문제 방지)
        return chatBotService.sendMessage(memberId, chatRequest, roomId);
    }

}