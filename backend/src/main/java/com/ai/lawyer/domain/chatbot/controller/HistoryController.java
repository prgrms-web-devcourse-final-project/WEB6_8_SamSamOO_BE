package com.ai.lawyer.domain.chatbot.controller;

import com.ai.lawyer.domain.chatbot.dto.ChatDto.ChatHistoryDto;
import com.ai.lawyer.domain.chatbot.dto.HistoryDto;
import com.ai.lawyer.domain.chatbot.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "History API", description = "채팅 방 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/chat/history")
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "채팅방 제목 목록 조회")
    @GetMapping("/")
    public ResponseEntity<List<HistoryDto>> getHistoryTitles(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(historyService.getHistoryTitle(memberId));
    }

    @Operation(summary = "채팅 조회")
    @GetMapping("/{historyId}")
    public ResponseEntity<List<ChatHistoryDto>> getChatHistory(@AuthenticationPrincipal Long memberId, @PathVariable("historyId") Long roomId) {
        return historyService.getChatHistory(memberId, roomId);
    }

    @Operation(summary = "채팅방 삭제")
    @DeleteMapping("/{historyId}")
    public ResponseEntity<String> deleteHistory(@AuthenticationPrincipal Long memberId, @PathVariable("historyId") Long roomId) {
        return ResponseEntity.ok(historyService.deleteHistory(memberId, roomId));
    }

}
