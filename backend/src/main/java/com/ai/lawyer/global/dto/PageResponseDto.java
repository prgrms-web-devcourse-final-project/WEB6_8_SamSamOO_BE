package com.ai.lawyer.global.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
public class PageResponseDto {
    private List<?> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;

    public static <T> PageResponseDto from(Page<T> page) {
        return PageResponseDto.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .build();
    }
}
