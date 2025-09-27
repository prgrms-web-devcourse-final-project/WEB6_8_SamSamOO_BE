package com.ai.lawyer.domain.chatbot.controller;

import com.ai.lawyer.domain.chatbot.entity.KeywordRank;
import com.ai.lawyer.domain.chatbot.service.KeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Keyword API", description = "키워드 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/chat/keyword")
public class KeywordController {

    private final KeywordService keywordService;

    @Operation(summary = "1~5위 키워드 랭킹 조회")
    @GetMapping("/ranks")
    public ResponseEntity<List<KeywordRank>> getKeywordRanks() {
        return ResponseEntity.ok(keywordService.getTop5KeywordRanks());
    }

}
