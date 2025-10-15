package com.ai.lawyer.domain.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "판례수 법령수 채팅수 투표수 DTO")
public class FullData {

    @Schema(description = "판례 수", example = "1000")
    private Long precedentCount;

    @Schema(description = "법령 수", example = "500")
    private Long lawCount;

    @Schema(description = "채팅 수", example = "2000")
    private Long chatCount;

    @Schema(description = "투표 수", example = "300")
    private Long voteCount;
    
}
