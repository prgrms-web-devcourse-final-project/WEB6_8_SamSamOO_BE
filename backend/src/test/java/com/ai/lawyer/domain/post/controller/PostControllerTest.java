package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.dto.PostSimpleDto;
import com.ai.lawyer.domain.post.dto.PostUpdateDto;
import com.ai.lawyer.domain.post.dto.PostWithPollCreateDto;
import com.ai.lawyer.domain.post.service.PostService;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.oauth.CustomOAuth2UserService;
import com.ai.lawyer.global.oauth.OAuth2FailureHandler;
import com.ai.lawyer.global.oauth.OAuth2SuccessHandler;
import com.ai.lawyer.global.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@WebMvcTest(
        controllers = PostController.class,
        excludeAutoConfiguration = {
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                DataSourceAutoConfiguration.class,
                JpaBaseConfiguration.class
        }
)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2SuccessHandler oauth2SuccessHandler;

    @MockitoBean
    private OAuth2FailureHandler oauth2FailureHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        given(cookieUtil.getAccessTokenFromCookies(any())).willReturn("valid-access-token");
        given(tokenProvider.validateTokenWithResult("valid-access-token"))
                .willReturn(TokenProvider.TokenValidationResult.VALID);
        given(tokenProvider.getMemberIdFromToken("valid-access-token")).willReturn(1L);
        given(tokenProvider.getRoleFromToken("valid-access-token")).willReturn("USER");
    }

    @Test
    @DisplayName("게시글 등록")
    void t1() throws Exception {
        PostRequestDto dto = PostRequestDto.builder().postName("테스트 제목").postContent("테스트 내용").build();
        PostDto responseDto = PostDto.builder().postId(1L).postName("테스트 제목").postContent("테스트 내용").build();
        Mockito.when(postService.createPost(Mockito.any(), Mockito.anyLong())).thenReturn(responseDto);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("게시글이 등록되었습니다."))
                .andExpect(jsonPath("$.result.postName").value("테스트 제목"));
    }

    @Test
    @DisplayName("게시글 전체 조회")
    void t2() throws Exception {
        List<PostDetailDto> posts = Collections.emptyList();
        Mockito.when(postService.getAllPosts(Mockito.anyLong())).thenReturn(posts);

        mockMvc.perform(get("/api/posts")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("게시글 단일 조회")
    void t3() throws Exception {
        PostDto postDto = PostDto.builder().postId(1L).postName("테스트 제목").build();
        PostDetailDto postDetailDto = PostDetailDto.builder().post(postDto).build();
        Mockito.when(postService.getPostDetailById(Mockito.anyLong(), Mockito.anyLong())).thenReturn(postDetailDto);

        mockMvc.perform(get("/api/posts/1")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.post.postId").value(1L));
    }

    @Test
    @DisplayName("회원별 게시글 목록 조회")
    void t4() throws Exception {
        List<PostDto> postDtoList = List.of(PostDto.builder().postId(1L).postName("테스트 제목").build());
        PostDetailDto postDetailDto = PostDetailDto.builder().post(postDtoList.getFirst()).build();
        Mockito.when(postService.getPostsByMemberId(Mockito.anyLong())).thenReturn(postDtoList);
        Mockito.when(postService.getPostDetailById(Mockito.anyLong(), Mockito.anyLong())).thenReturn(postDetailDto);

        mockMvc.perform(get("/api/posts/member/1")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("게시글 수정")
    void t5() throws Exception {
        PostDto postDto = PostDto.builder()
                .postId(1L)
                .postName("수정 제목")
                .memberId(1L)
                .build();
        PostDetailDto postDetailDto = PostDetailDto.builder().post(postDto).build();
        Mockito.doNothing().when(postService).patchUpdatePost(Mockito.anyLong(), Mockito.any());
        Mockito.when(postService.getPostDetailById(eq(1L), Mockito.anyLong())).thenReturn(postDetailDto);
        PostUpdateDto updateDto = PostUpdateDto.builder().postName("수정 제목").build();

        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.post.postName").value("수정 제목"));
    }

    @Test
    @DisplayName("게시글 삭제")
    void t6() throws Exception {
        PostDto postDto = PostDto.builder()
                .postId(1L)
                .postName("삭제 제목")
                .memberId(1L)
                .build();
        PostDetailDto postDetailDto = PostDetailDto.builder().post(postDto).build();
        Mockito.when(postService.getPostDetailById(eq(1L), Mockito.anyLong())).thenReturn(postDetailDto);
        Mockito.doNothing().when(postService).deletePost(Mockito.anyLong());

        mockMvc.perform(delete("/api/posts/1")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 페이징 API")
    void t7() throws Exception {
        List<PostDto> postList = List.of(PostDto.builder().postId(1L).postName("테스트 제목").build());
        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<PostDto> page = new PageImpl<>(postList, pageable, 1);
        Mockito.when(postService.getPostsPaged(any(Pageable.class), any(Long.class))).thenReturn(page);

        mockMvc.perform(get("/api/posts/paged")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalPages").value(1))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    @DisplayName("게시글 간편 전체 조회")
    void t8() throws Exception {
        List<PostSimpleDto> posts = Collections.emptyList();
        Mockito.when(postService.getAllSimplePosts()).thenReturn(posts);

        mockMvc.perform(get("/api/posts/simplePost")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("본인 게시글 단일 조회")
    void t9() throws Exception {
        PostDto postDto = PostDto.builder().postId(1L).postName("테스트 제목").build();
        Mockito.when(postService.getMyPostById(eq(1L), any(Long.class))).thenReturn(postDto);

        mockMvc.perform(get("/api/posts/my/1")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.postId").value(1L));
    }

    @Test
    @DisplayName("본인 게시글 전체 페이징 조회")
    void t10_paged() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<PostDto> posts = List.of(PostDto.builder().postId(1L).postName("테스트 제목").build());
        PageImpl<PostDto> page = new PageImpl<>(posts, pageable, 1);
        Mockito.when(postService.getMyPostspaged(any(Pageable.class), any(Long.class))).thenReturn(page);

        mockMvc.perform(get("/api/posts/mypaged")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.totalPages").value(1));
    }

    @Test
    @DisplayName("게시글+투표 동시 등록")
    void t11() throws Exception {
        PostDetailDto result = PostDetailDto.builder()
                .post(PostDto.builder().postId(1L).postName("테스트 제목").build())
                .build();
        PostWithPollCreateDto dto = PostWithPollCreateDto.builder().build();
        Mockito.when(postService.createPostWithPoll(any(PostWithPollCreateDto.class), any(Long.class))).thenReturn(result);

        mockMvc.perform(post("/api/posts/createPost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.post.postId").value(1L));
    }

    @Test
    @DisplayName("진행중 투표 게시글 페이징 조회")
    void t12() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<PostDto> page = new PageImpl<>(List.of(), pageable, 0);
        Mockito.when(postService.getOngoingPostsPaged(any(Pageable.class), any(Long.class))).thenReturn(page);

        mockMvc.perform(get("/api/posts/ongoingPaged")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray());
    }

    @Test
    @DisplayName("마감 투표 게시글 페이징 조회")
    void t13() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<PostDto> page = new PageImpl<>(List.of(), pageable, 0);
        Mockito.when(postService.getClosedPostsPaged(any(Pageable.class), any(Long.class))).thenReturn(page);

        mockMvc.perform(get("/api/posts/closedPaged")
                        .param("page", "0")
                        .param("size", "10")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray());
    }
}