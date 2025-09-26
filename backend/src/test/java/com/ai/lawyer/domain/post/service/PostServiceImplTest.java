package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import com.ai.lawyer.domain.poll.repository.PollRepository;
import com.ai.lawyer.domain.poll.repository.PollOptionsRepository;
import com.ai.lawyer.domain.poll.repository.PollVoteRepository;
import com.ai.lawyer.domain.poll.repository.PollStaticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PollRepository pollRepository;
    @Mock
    private PollOptionsRepository pollOptionsRepository;
    @Mock
    private PollVoteRepository pollVoteRepository;
    @Mock
    private PollStaticsRepository pollStaticsRepository;
    @InjectMocks
    private PostServiceImpl postService;

    private Member member;
    private Post post;
    private PostDto postDto;
    private PostRequestDto postRequestDto;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId(2L)
                .name("테스트회원")
                .build();
        post = Post.builder()
                .postId(1L)
                .member(member)
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
        postRequestDto = PostRequestDto.builder()
                .postName("테스트 제목")
                .postContent("테스트 내용")
                .category("일반")
                .build();
    }

    @Test
    @DisplayName("게시글 생성 성공")
    void createPost_success() {
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        PostDto result = postService.createPost(postRequestDto, 2L);
        assertThat(result.getPostName()).isEqualTo(postRequestDto.getPostName());
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 회원 없음")
    void createPost_fail_noMember() {
        when(memberRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.createPost(postRequestDto, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("게시글 단일 조회 성공")
    void getPostById_success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        PostDto result = postService.getPostDetailById(1L).getPost();
        assertThat(result.getPostId()).isEqualTo(1L);
        assertThat(result.getMemberId()).isEqualTo(member.getMemberId());
    }

    @Test
    @DisplayName("게시글 단일 조회 실패 - 존재하지 않음")
    void getPostById_fail() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getPostDetailById(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("회원별 게시글 목록 조회 성공")
    void getPostsByMemberId_success() {
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(postRepository.findByMember(member)).thenReturn(Arrays.asList(post));
        List<PostDto> results = postService.getPostsByMemberId(member.getMemberId());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMemberId()).isEqualTo(member.getMemberId());
    }

    @Test
    @DisplayName("회원별 게시글 목록 조회 실패 - 게시글 없음")
    void getPostsByMemberId_fail_noPosts() {
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(postRepository.findByMember(member)).thenReturn(List.of());
        assertThatThrownBy(() -> postService.getPostsByMemberId(member.getMemberId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("해당 회원의 게시글이 없습니다.");
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        PostDto updateDto = postDto.toBuilder().postName("수정 제목").build();
        PostDto result = postService.updatePost(1L, updateDto);
        assertThat(result.getPostName()).isEqualTo("수정 제목");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않음")
    void updatePost_fail() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.updatePost(1L, postDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("수정할 게시글을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_success() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        doNothing().when(postRepository).delete(post);
        postService.deletePost(1L);
        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 존재하지 않음")
    void deletePost_fail() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.deletePost(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("삭제할 게시글을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("전체 게시글 조회 성공")
    void getAllPosts_success() {
        when(postRepository.findAll()).thenReturn(List.of(post));
        when(postRepository.findById(post.getPostId())).thenReturn(Optional.of(post));
        List<?> results = postService.getAllPosts();
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("본인 게시글 전체 조회 성공")
    void getMyPosts_success() {
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(postRepository.findByMember(member)).thenReturn(List.of(post));
        List<PostDto> results = postService.getMyPosts(member.getMemberId());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMemberId()).isEqualTo(member.getMemberId());
    }

    @Test
    @DisplayName("본인 게시글 전체 조회 실패 - 회원 없음")
    void getMyPosts_fail_noMember() {
        when(memberRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getMyPosts(2L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("회원 정보를 찾을 수 없습니다.");
    }
}
