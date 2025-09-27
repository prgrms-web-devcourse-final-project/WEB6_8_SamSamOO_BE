package com.ai.lawyer.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ExtractionDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TitleExtractionDto {
        private String title;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class KeywordExtractionDto {
        private List<String> keyword;
    }

}
