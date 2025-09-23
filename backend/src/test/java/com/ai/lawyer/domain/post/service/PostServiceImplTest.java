package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostServiceImpl postService;

    private Post post;
    private PostDto postDto;

    @BeforeEach
    void setUp() {
        post = Post.builder()
                .postId(1L)
                .memberId(2L)
                .postName("테스트 제목")
                .postContent("테스트 내용")
                .category("일반")
                .createdAt(LocalDateTime.now())
                .build();
        postDto = PostDto.builder()
                .postId(1L)
                .memberId(2L)
                .postName("테스트 제목")
                .postContent("테스트 내용")
                .category("일반")
                .createdAt(post.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("게시글 생성 성공")
    void t1() {
        when(postRepository.save(any(Post.class))).thenReturn(post);
        PostDto result = postService.createPost(postDto);
        assertThat(result.getPostName()).isEqualTo(postDto.getPostName());
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 단일 조회 성공")
    void t2() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        PostDto result = postService.getPostById(1L);
        assertThat(result.getPostId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("게시글 단일 조회 실패 - 존재하지 않음")
    void t3() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getPostById(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("회원별 게시글 목록 조회 성공")
    void t4() {
        when(postRepository.findByMemberId(2L)).thenReturn(Arrays.asList(post));
        List<PostDto> results = postService.getPostsByMemberId(2L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMemberId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("회원별 게시글 목록 조회 실패 - 게시글 없음")
    void t5() {
        when(postRepository.findByMemberId(2L)).thenReturn(List.of());
        assertThatThrownBy(() -> postService.getPostsByMemberId(2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("해당 회원의 게시글이 없습니다.");
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void t6() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        PostDto updateDto = postDto.toBuilder().postName("수정 제목").build();
        PostDto result = postService.updatePost(1L, updateDto);
        assertThat(result.getPostName()).isEqualTo("수정 제목");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않음")
    void t7() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.updatePost(1L, postDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("수정할 게시글을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void t8() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        doNothing().when(postRepository).delete(post);
        postService.deletePost(1L);
        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 존재하지 않음")
    void t9() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.deletePost(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("삭제할 게시글을 찾을 수 없습니다.");
    }
}
