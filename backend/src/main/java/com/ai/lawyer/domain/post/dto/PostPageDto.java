package com.ai.lawyer.domain.post.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PostPageDto {
    private List<PostDto> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public PostPageDto(Page<PostDto> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
    }
}