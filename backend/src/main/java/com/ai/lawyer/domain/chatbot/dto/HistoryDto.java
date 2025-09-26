package com.ai.lawyer.domain.chatbot.dto;

import com.ai.lawyer.domain.chatbot.entity.History;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "히스토리 DTO - 채팅방")
public class HistoryDto {

    @Schema(description = "방 ID", example = "1")
    private Long historyRoomId;

    @Schema(description = "방 제목", example = "손해배상 청구 관련 문의")
    private String title;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "업데이트 시간")
    private LocalDateTime updatedAt;

    public static HistoryDto from(History room) {
        return HistoryDto.builder()
                .historyRoomId(room.getHistoryId())
                .title(room.getTitle())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
