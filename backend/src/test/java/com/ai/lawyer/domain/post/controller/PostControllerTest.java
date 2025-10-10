package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.dto.PostUpdateDto;
import com.ai.lawyer.domain.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import com.ai.lawyer.global.security.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import static org.mockito.BDDMockito.*;
import com.ai.lawyer.global.jwt.TokenProvider;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@WebMvcTest(
        controllers = PostController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration.class
        }
)
class PostControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PostService postService;
    @MockitoBean
    private com.ai.lawyer.domain.member.repositories.MemberRepository memberRepository;
    @MockitoBean
    private com.ai.lawyer.global.jwt.TokenProvider tokenProvider;
    @MockitoBean
    private com.ai.lawyer.global.jwt.CookieUtil cookieUtil;
    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;
    @MockitoBean
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    @MockitoBean
    private com.ai.lawyer.global.oauth.CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private com.ai.lawyer.global.oauth.OAuth2SuccessHandler oauth2SuccessHandler;
    @MockitoBean
    private com.ai.lawyer.global.oauth.OAuth2FailureHandler oauth2FailureHandler;
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
        com.ai.lawyer.domain.post.dto.PostDto responseDto = com.ai.lawyer.domain.post.dto.PostDto.builder().postId(1L).postName("테스트 제목").postContent("테스트 내용").build();
        Mockito.when(postService.createPost(Mockito.any(), Mockito.anyLong())).thenReturn(responseDto);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(201))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("게시글이 등록되었습니다."))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.postName").value("테스트 제목"));
    }

    @Test
    @DisplayName("게시글 전체 조회")
    void t2() throws Exception {
        List<com.ai.lawyer.domain.post.dto.PostDetailDto> posts = java.util.Collections.emptyList();
        Mockito.when(postService.getAllPosts(Mockito.anyLong())).thenReturn(posts);

        mockMvc.perform(get("/api/posts")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("게시글 단일 조회")
    void t3() throws Exception {
        com.ai.lawyer.domain.post.dto.PostDto postDto = com.ai.lawyer.domain.post.dto.PostDto.builder().postId(1L).postName("테스트 제목").build();
        com.ai.lawyer.domain.post.dto.PostDetailDto postDetailDto = com.ai.lawyer.domain.post.dto.PostDetailDto.builder().post(postDto).build();
        Mockito.when(postService.getPostDetailById(Mockito.anyLong(), Mockito.anyLong())).thenReturn(postDetailDto);

        mockMvc.perform(get("/api/posts/1")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.post.postId").value(1L));
    }

    @Test
    @DisplayName("회원별 게시글 목록 조회")
    void t4() throws Exception {
        List<com.ai.lawyer.domain.post.dto.PostDto> postDtoList = List.of(com.ai.lawyer.domain.post.dto.PostDto.builder().postId(1L).postName("테스트 제목").build());
        com.ai.lawyer.domain.post.dto.PostDetailDto postDetailDto = com.ai.lawyer.domain.post.dto.PostDetailDto.builder().post(postDtoList.getFirst()).build();
        Mockito.when(postService.getPostsByMemberId(Mockito.anyLong())).thenReturn(postDtoList);
        Mockito.when(postService.getPostDetailById(Mockito.anyLong(), Mockito.anyLong())).thenReturn(postDetailDto);

        mockMvc.perform(get("/api/posts/member/1")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("게시글 수정")
    void t5() throws Exception {
        com.ai.lawyer.domain.post.dto.PostDto postDto = com.ai.lawyer.domain.post.dto.PostDto.builder()
            .postId(1L)
            .postName("수정 제목")
            .memberId(1L)
            .build();
        com.ai.lawyer.domain.post.dto.PostDetailDto postDetailDto = com.ai.lawyer.domain.post.dto.PostDetailDto.builder().post(postDto).build();
        Mockito.doNothing().when(postService).patchUpdatePost(Mockito.anyLong(), Mockito.any());
        Mockito.when(postService.getPostDetailById(Mockito.eq(1L), Mockito.anyLong())).thenReturn(postDetailDto);
        com.ai.lawyer.domain.post.dto.PostUpdateDto updateDto = com.ai.lawyer.domain.post.dto.PostUpdateDto.builder().postName("수정 제목").build();

        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.post.postName").value("수정 제목"));
    }

    @Test
    @DisplayName("게시글 삭제")
    void t6() throws Exception {
        com.ai.lawyer.domain.post.dto.PostDto postDto = com.ai.lawyer.domain.post.dto.PostDto.builder()
            .postId(1L)
            .postName("삭제 제목")
            .memberId(1L)
            .build();
        com.ai.lawyer.domain.post.dto.PostDetailDto postDetailDto = com.ai.lawyer.domain.post.dto.PostDetailDto.builder().post(postDto).build();
        Mockito.when(postService.getPostDetailById(Mockito.eq(1L), Mockito.anyLong())).thenReturn(postDetailDto);
        Mockito.doNothing().when(postService).deletePost(Mockito.anyLong());

        mockMvc.perform(delete("/api/posts/1")
                        .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 페이징 API")
    void t7() throws Exception {
        java.util.List<com.ai.lawyer.domain.post.dto.PostDto> postList = java.util.List.of(
            com.ai.lawyer.domain.post.dto.PostDto.builder().postId(1L).postName("테스트 제목").build()
        );
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.PageImpl<com.ai.lawyer.domain.post.dto.PostDto> page = new org.springframework.data.domain.PageImpl<>(postList, pageable, 1);
        Mockito.when(postService.getPostsPaged(Mockito.any(org.springframework.data.domain.Pageable.class), Mockito.anyLong())).thenReturn(page);

        mockMvc.perform(get("/api/posts/paged")
                .param("page", "0")
                .param("size", "10")
                .cookie(new Cookie("accessToken", "valid-access-token")))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.content").isArray())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.page").value(0))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.size").value(10))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.totalPages").value(1))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.totalElements").value(1));
    }
}