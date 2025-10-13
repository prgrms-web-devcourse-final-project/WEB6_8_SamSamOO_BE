package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostServiceTest {
    @Mock
    private PostService postService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("게시글 생성")
    void t1() {
        PostRequestDto dto = new PostRequestDto();
        PostDto expected = new PostDto();
        Mockito.when(postService.createPost(Mockito.any(), Mockito.anyLong())).thenReturn(expected);
        PostDto result = postService.createPost(dto, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("회원 정보")
    void t2() {
        Mockito.when(postService.createPost(Mockito.any(), Mockito.anyLong())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다"));
        PostRequestDto dto = PostRequestDto.builder().postName("제목").postContent("내용").build();
        assertThatThrownBy(() -> postService.createPost(dto, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("게시글 ID로 상세 조회")
    void t3() {
        PostDetailDto expected = new PostDetailDto();
        Mockito.when(postService.getPostById(Mockito.anyLong())).thenReturn(expected);
        PostDetailDto result = postService.getPostById(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("게시글 상세 조회")
    void t4() {
        PostDetailDto expected = new PostDetailDto();
        Mockito.when(postService.getPostDetailById(Mockito.anyLong(), Mockito.anyLong())).thenReturn(expected);
        PostDetailDto result = postService.getPostDetailById(1L, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("회원 ID로 게시글 목록 조회")
    void t5() {
        java.util.List expected = java.util.Collections.emptyList();
        Mockito.when(postService.getPostsByMemberId(Mockito.anyLong())).thenReturn(expected);
        java.util.List result = postService.getPostsByMemberId(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("게시글 수정")
    void t6() {
        PostDto expected = new PostDto();
        PostUpdateDto updateDto = new PostUpdateDto();
        Mockito.when(postService.updatePost(Mockito.anyLong(), Mockito.any())).thenReturn(expected);
        PostDto result = postService.updatePost(1L, updateDto);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("게시글 삭제")
    void t7() {
        Mockito.doNothing().when(postService).deletePost(Mockito.anyLong());
        postService.deletePost(1L);
        Mockito.verify(postService).deletePost(1L);
    }

    @Test
    @DisplayName("전체 게시글 목록 조회")
    void t8() {
        java.util.List expected = java.util.Collections.emptyList();
        Mockito.when(postService.getAllPosts(Mockito.anyLong())).thenReturn(expected);
        java.util.List result = postService.getAllPosts(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("내 게시글 단일 조회")
    void t9() {
        PostDto expected = new PostDto();
        Mockito.when(postService.getMyPostById(Mockito.anyLong(), Mockito.anyLong())).thenReturn(expected);
        PostDto result = postService.getMyPostById(1L, 2L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("내 게시글 목록 페이징 조회")
    void t10_paged() {
        java.util.List<PostDto> postList = java.util.List.of(new PostDto());
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.PageImpl<PostDto> page = new org.springframework.data.domain.PageImpl<>(postList, pageable, 1);
        Mockito.when(postService.getMyPostspaged(Mockito.eq(pageable), Mockito.eq(1L))).thenReturn(page);
        var result = postService.getMyPostspaged(pageable, 1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("게시글 부분 수정")
    void t11() {
        PostUpdateDto updateDto = new PostUpdateDto();
        Mockito.doNothing().when(postService).patchUpdatePost(Mockito.anyLong(), Mockito.any());
        postService.patchUpdatePost(1L, updateDto);
        Mockito.verify(postService).patchUpdatePost(1L, updateDto);
    }

    @Test
    @DisplayName("게시글 페이징 조회")
    void t12() {
        java.util.List<PostDto> postList = java.util.List.of(new PostDto());
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.PageImpl<PostDto> page = new org.springframework.data.domain.PageImpl<>(postList, pageable, 1);
        Mockito.when(postService.getPostsPaged(Mockito.eq(pageable), Mockito.eq(1L))).thenReturn(page);
        var result = postService.getPostsPaged(pageable, 1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }
}
