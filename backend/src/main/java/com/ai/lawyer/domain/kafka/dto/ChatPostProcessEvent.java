package com.ai.lawyer.domain.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatPostProcessEvent {
    private Long historyId;
    private String userMessage;
    private String chatResponse;
    private List<DocumentDto> similarCaseDocuments;
    private List<DocumentDto> similarLawDocuments;
}
