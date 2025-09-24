package com.ai.lawyer.global.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponseDto {
    private List<?> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;
}
