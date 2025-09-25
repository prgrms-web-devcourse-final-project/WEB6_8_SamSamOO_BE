package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.service.PostService;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
    @Autowired
    private MemberRepository memberRepository;

    private Long postId;
    private Long memberId;
    private Member member;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 저장
        member = Member.builder()
            .loginId("test1@email.com")
            .password("pw")
            .age(20)
            .gender(Member.Gender.MALE)
            .role(Member.Role.USER)
            .name("테스트회원")
            .build();
        member = memberRepository.save(member); // 실제 PK 할당
        this.memberId = member.getMemberId();

        PostRequestDto postRequestDto = PostRequestDto.builder()
                .postName("테스트 제목")
                .postContent("테스트 내용")
                .category("일반")
                .build();
        PostDto saved = postService.createPost(postRequestDto, member);
        postId = saved.getPostId();
    }

//    @Test
//    @DisplayName("게시글 등록")
//    void t1() throws Exception {
//        String body = """
//            {
//                \"postName\": \"이혼하고 싶어요\",
//                \"postContent\": \"이혼하고 싶은데 어떻게 해야 하나요?\",
//                \"category\": \"이혼\"
//            }
//        """;
//
//        ResultActions resultActions = mvc.perform(post("/api/posts")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(body)
//                .param("memberId", String.valueOf(memberId)))
//                .andDo(print());
//
//        resultActions
//                .andExpect(handler().handlerType(PostController.class))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(201))
//                .andExpect(jsonPath("$.message").value("게시글이 등록되었습니다."))
//                .andExpect(jsonPath("$.result.postName").value("이혼하고 싶어요"));
//    }
//
//    @Test
//    @DisplayName("게시글 단일 조회")
//    void t2() throws Exception {
//        ResultActions resultActions = mvc.perform(get("/api/posts/" + postId))
//                .andDo(print());
//
//        resultActions
//                .andExpect(handler().handlerType(PostController.class))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.result.postId").value(postId));
//    }
//
//    @Test
//    @DisplayName("게시글 전체 조회")
//    void t3() throws Exception {
//        ResultActions resultActions = mvc.perform(get("/api/posts"))
//                .andDo(print());
//
//        resultActions
//                .andExpect(handler().handlerType(PostController.class))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.result").isArray());
//    }
//
//    @Test
//    @DisplayName("게시글 수정")
//    void t4() throws Exception {
//        ResultActions resultActions = mvc.perform(put("/api/posts/" + postId)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content("""
//                    {
//                        "postName": "부동산 사기",
//                        "postContent": "전세사기당했어요ㅠㅠ",
//                        "category": "부동산"
//                    }
//                """.stripIndent()))
//                .andDo(print());
//
//        resultActions
//                .andExpect(handler().handlerType(PostController.class))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.result.postName").value("부동산 사기"));
//    }
//
//    @Test
//    @DisplayName("게시글 삭제")
//    void t5() throws Exception {
//        ResultActions resultActions = mvc.perform(delete("/api/posts/" + postId))
//                .andDo(print());
//
//        resultActions
//                .andExpect(handler().handlerType(PostController.class))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.message").value("게시글이 삭제되었습니다."));
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 게시글 조회 시 예외 발생")
//    void t6() throws Exception {
//        ResultActions resultActions = mvc.perform(get("/api/posts/999999"))
//                .andDo(print());
//
//        resultActions
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.code").value(404))
//                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
//    }
}
