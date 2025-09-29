package com.ai.lawyer.domain.chatbot.dto;

import com.ai.lawyer.domain.chatbot.entity.Chat;
import com.ai.lawyer.domain.chatbot.entity.ChatLaw;
import com.ai.lawyer.domain.chatbot.entity.ChatPrecedent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "채팅 관련 DTO")
public class ChatDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "채팅 요청 DTO")
    public static class ChatRequest {
        @Schema(description = "사용자가 입력한 메시지", example = "보험 회사에서 손해배상 청구를 거절당했어요. 어떻게 해야 하나요?")
        private String message;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "채팅 응답 DTO")
    public static class ChatResponse {

        @Schema(description = "채팅방 ID", example = "1")
        private Long roomId;

        @Schema(description = "History 방 제목", example = "손해배상 청구 관련 문의")
        private String title;

        @Schema(description = "AI 챗봇의 응답 메시지", example = "네, 관련 법령과 판례를 바탕으로 답변해 드리겠습니다.")
        private String message;

        @Schema(description = "응답 생성에 참고한 유사 판례 정보 목록")
        private List<Document> similarCases;

        @Schema(description = "응답 생성에 참고한 유사 법령 정보 목록")
        private List<Document> similarLaws;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "판례 내용 DTO")
    public static class ChatPrecedentDto {

        @Schema(description = "판례 내용", example = "이 사건은 손해배상 청구에 관한 판례입니다...")
        private String precedentContent;

        @Schema(description = "케이스 넘버", example = "[2020다12345]")
        private String caseNumber;

        @Schema(description = "사건명", example = "손해배상 청구 사건")
        private String caseName;

        public static ChatPrecedentDto from(ChatPrecedent cp) {
            return ChatPrecedentDto.builder()
                    .precedentContent(cp.getPrecedentContent())
                    .caseNumber(cp.getCaseNumber())
                    .caseName(cp.getCaseName())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatLawDto {

        @Schema(description = "법령 내용", example = "제123조(손해배상) ① 누구든지 타인에게 손해를 가한 때에는 그 손해를 배상할 책임이 있다...")
        private String content;

        @Schema(description = "법령명", example = "민법")
        private String lawName;

        public static ChatLawDto from(ChatLaw cl) {
            return ChatLawDto.builder()
                    .content(cl.getContent())
                    .lawName(cl.getLawName())
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "특정 채팅방의 대화 내역 DTO")
    public static class ChatHistoryDto {

        @Schema(description = "AI 인지 USER 인지", example = "USER")
        private String type;

        @Schema(description = "메시지 내용", example = "안녕하세요~~")
        private String message;

        private List<ChatPrecedentDto> precedents;

        private List<ChatLawDto> laws;

        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;

        public static ChatHistoryDto from(Chat chat) {

            List<ChatPrecedentDto> precedentDtos = new ArrayList<>();
            for (ChatPrecedent cp : chat.getChatPrecedents()) {
                precedentDtos.add(ChatPrecedentDto.from(cp));
            }

            List<ChatLawDto> lawDtos = new ArrayList<>();
            for (ChatLaw cl : chat.getChatLaws()) {
                lawDtos.add(ChatLawDto.from(cl));
            }

            return ChatHistoryDto.builder()
                    .type(chat.getType().toString())
                    .message(chat.getMessage())
                    .createdAt(chat.getCreatedAt())
                    .precedents(precedentDtos)
                    .laws(lawDtos)
                    .build();
        }
    }
}