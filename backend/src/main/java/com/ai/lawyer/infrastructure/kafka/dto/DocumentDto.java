package com.ai.lawyer.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private String text;
    private Map<String, Object> metadata;

    public static DocumentDto from(Document document) {
        return new DocumentDto(document.getText(), document.getMetadata());
    }
}