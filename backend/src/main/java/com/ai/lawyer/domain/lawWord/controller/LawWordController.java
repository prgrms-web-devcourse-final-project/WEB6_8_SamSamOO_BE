package com.ai.lawyer.domain.lawWord.controller;

import com.ai.lawyer.domain.lawWord.service.LawWordService;
import com.ai.lawyer.domain.precedent.entity.Precedent;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/law-word")
public class LawWordController {

    private final LawWordService lawWordService;

    @GetMapping("/{word}")
    @Operation(summary = "법령 용어 검색", description = "법령 용어에 대한 정의를 반환합니다. \n" +
            "예시: /api/law-word/선박")
    public ResponseEntity<?> getPrecedent(@PathVariable String  word) {
        return ResponseEntity.ok(lawWordService.findDefinition(word));
    }
}
