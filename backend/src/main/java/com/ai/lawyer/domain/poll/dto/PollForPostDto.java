package com.ai.lawyer.domain.poll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollForPostDto {
    @Schema(description = "투표 제목", example = "당신의 선택은?")
    private String voteTitle;
    @Schema(description = "투표 항목(2개 필수)", example = "[{\"content\": \"항목1 내용\"}, {\"content\": \"항목2 내용\"}]")
    private List<PollOptionCreateDto> pollOptions;
    private LocalDateTime reservedCloseAt;
}
