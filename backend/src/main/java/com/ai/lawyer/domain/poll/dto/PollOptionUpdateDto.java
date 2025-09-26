package com.ai.lawyer.domain.poll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionUpdateDto {
    @Schema(description = "투표 항목 내용", example = "항목1 내용")
    private String content;
}
