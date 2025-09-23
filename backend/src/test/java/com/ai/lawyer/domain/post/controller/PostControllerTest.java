package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private PostService postService;

    private Long postId;

    @BeforeEach
    void setUp() {
        PostDto postDto = PostDto.builder()
                .memberId(1L)
                .postName("테스트 제목")
                .postContent("테스트 내용")
                .category("일반")
                .createdAt(LocalDateTime.now())
                .build();
        PostDto saved = postService.createPost(postDto);
        postId = saved.getPostId();
    }

    @Test
    @DisplayName("게시글 등록")
    void t1() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "memberId": 2,
                        "postName": "이혼하고 싶어요",
                        "postContent": "이혼하고 싶은데 어떻게 해야 하나요?",
                        "category": "이혼"
                    }
                """.stripIndent()))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("게시글이 등록되었습니다."))
                .andExpect(jsonPath("$.result.postName").value("이혼하고 싶어요"));
    }

    @Test
    @DisplayName("게시글 단일 조회")
    void t2() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/posts/" + postId))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.postId").value(postId));
    }

    @Test
    @DisplayName("게시글 전체 조회")
    void t3() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/posts"))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("게시글 수정")
    void t4() throws Exception {
        ResultActions resultActions = mvc.perform(put("/api/posts/" + postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "postName": "부동산 사기",
                        "postContent": "전세사기당했어요ㅠㅠ",
                        "category": "부동산"
                    }
                """.stripIndent()))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.postName").value("수정된 제목"));
    }

    @Test
    @DisplayName("게시글 삭제")
    void t5() throws Exception {
        ResultActions resultActions = mvc.perform(delete("/api/posts/" + postId))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(PostController.class))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("게시글이 삭제되었습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외 발생")
    void t6() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/posts/999999"))
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
    }
}
