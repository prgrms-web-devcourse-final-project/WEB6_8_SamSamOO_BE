package com.ai.lawyer.domain.lawWord.controller;

import com.ai.lawyer.domain.lawWord.service.LawWordService;
import com.ai.lawyer.domain.precedent.entity.Precedent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/law-word")
@Tag(name = "법령 용어", description = "법령 용어 API")
public class LawWordController {

    private final LawWordService lawWordService;

    @GetMapping("/v1/{word}")
    @Operation(summary = "법령 용어 검색 version 1", description = "법령 용어에 대한 정의를 반환합니다. \n" +
            "예시: /api/law-word/승소")
    public ResponseEntity<?> getPrecedentV1(@PathVariable String  word) {
        try {
            return ResponseEntity.ok(lawWordService.findDefinition(word));
        }catch (Exception e){
            return ResponseEntity.badRequest().body("법령 용어 검색 에러 : " + e.getMessage());
        }
    }

    @GetMapping("/v2/{word}")
    @Operation(summary = "법령 용어 검색 version 2", description = "법령 용어에 대한 정의를 반환합니다. \n" +
            "예시: /api/law-word/승소")
    public ResponseEntity<?> getPrecedentV2(@PathVariable String  word) {
        try {
            return ResponseEntity.ok(lawWordService.findDefinitionV2(word));
        }catch (Exception e){
            return ResponseEntity.badRequest().body("법령 용어 검색 에러 : " + e.getMessage());
        }
    }
}
