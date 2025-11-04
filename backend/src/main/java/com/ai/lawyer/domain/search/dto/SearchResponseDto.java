package com.ai.lawyer.domain.search.dto;

import com.ai.lawyer.global.dto.PageResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDto {

    @Schema(description = "법령 검색 결과 페이지 (없으면 null)")
    private PageResponseDto laws;

    @Schema(description = "판례 검색 결과 페이지 (없으면 null)")
    private PageResponseDto precedents;
}

