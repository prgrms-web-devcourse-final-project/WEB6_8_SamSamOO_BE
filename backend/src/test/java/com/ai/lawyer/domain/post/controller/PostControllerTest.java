package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.service.PostService;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(SpringExtension.class)
class PostControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberRepository memberRepository;
    @MockBean
    private PostService postService;
    @Autowired
    private TokenProvider tokenProvider;

    private Member member;
    private String jwtToken;
    private PostDto postDto;
    private PostRequestDto postRequestDto;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .loginId("test1@email.com")
                .password("pw")
                .age(20)
                .gender(Member.Gender.MALE)
                .role(Member.Role.USER)
                .name("테스트회원")
                .build();
        member = memberRepository.save(member);
        jwtToken = "Bearer " + createTestToken(member.getMemberId());
        postDto = PostDto.builder()
                .postId(1L)
                .memberId(member.getMemberId())
                .postName("테스트 제목")
                .postContent("테스트 내용")
                .category("일반")
                .createdAt(LocalDateTime.now())
                .build();
        postRequestDto = PostRequestDto.builder()
                .postName("테스트 제목")
                .postContent("테스트 내용")
                .category("일반")
                .build();
    }

    private String createTestToken(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        return tokenProvider.generateAccessToken(member);
    }

    @Test
    @DisplayName("게시글 등록 성공")
    void createPost_success() throws Exception {
        when(postService.createPost(any(PostRequestDto.class), eq(member.getMemberId()))).thenReturn(postDto);
        mvc.perform(post("/api/posts")
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.postName").value("테스트 제목"));
    }

    @Test
    @DisplayName("게시글 단일 조회 성공")
    void getPostById_success() throws Exception {
        when(postService.getPostById(1L)).thenReturn(Mockito.mock(com.ai.lawyer.domain.post.dto.PostDetailDto.class));
        mvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 전체 조회 성공")
    void getAllPosts_success() throws Exception {
        when(postService.getAllPosts()).thenReturn(List.of(Mockito.mock(com.ai.lawyer.domain.post.dto.PostDetailDto.class)));
        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원별 게시글 목록 조회 성공")
    void getPostsByMember_success() throws Exception {
        when(postService.getPostsByMemberId(member.getMemberId())).thenReturn(List.of(postDto));
        mvc.perform(get("/api/posts/member/" + member.getMemberId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("본인 게시글 단일 조회 성공")
    void getMyPostById_success() throws Exception {
        when(postService.getMyPostById(1L, member.getMemberId())).thenReturn(postDto);
        mvc.perform(get("/api/posts/my/1")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.postName").value("테스트 제목"));
    }

    @Test
    @DisplayName("본인 게시글 전체 조회 성공")
    void getMyPosts_success() throws Exception {
        when(postService.getMyPosts(member.getMemberId())).thenReturn(List.of(postDto));
        mvc.perform(get("/api/posts/my")
                .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].postName").value("테스트 제목"));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_success() throws Exception {
        when(postService.getPostDetailById(1L)).thenReturn(Mockito.mock(com.ai.lawyer.domain.post.dto.PostDetailDto.class));
        when(postService.updatePost(eq(1L), any(PostDto.class))).thenReturn(postDto);
        mvc.perform(put("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_success() throws Exception {
        mvc.perform(delete("/api/posts/1"))
                .andExpect(status().isOk());
    }
}
