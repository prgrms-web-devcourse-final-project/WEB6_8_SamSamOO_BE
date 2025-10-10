package com.ai.lawyer.domain.poll.controller;

import com.ai.lawyer.domain.poll.service.PollService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import org.springframework.context.annotation.Import;
import com.ai.lawyer.global.security.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.junit.jupiter.api.DisplayName;
import jakarta.servlet.http.Cookie;
import static org.mockito.BDDMockito.*;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.domain.poll.dto.PollStaticsResponseDto;

@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@WebMvcTest(
    controllers = PollController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration.class
    }
)
class PollControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PollService pollService;
    @MockitoBean
    private com.ai.lawyer.domain.post.service.PostService postService;
    @MockitoBean
    private com.ai.lawyer.global.jwt.TokenProvider tokenProvider;
    @MockitoBean
    private com.ai.lawyer.global.jwt.CookieUtil cookieUtil;
    @MockitoBean
    private com.ai.lawyer.domain.member.repositories.MemberRepository memberRepository;
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

    @BeforeEach
    void setUp() {
        // JWT 필터 모킹 설정 - 쿠키에서 토큰 추출 및 검증
        given(cookieUtil.getAccessTokenFromCookies(any())).willReturn("valid-access-token");
        given(tokenProvider.validateTokenWithResult("valid-access-token"))
                .willReturn(TokenProvider.TokenValidationResult.VALID);
        given(tokenProvider.getMemberIdFromToken("valid-access-token")).willReturn(1L);
        given(tokenProvider.getRoleFromToken("valid-access-token")).willReturn("USER");
    }

    @Test
    @DisplayName("투표 단일 조회")
    void t1() throws Exception {
        Mockito.when(pollService.getPoll(Mockito.anyLong(), Mockito.anyLong())).thenReturn(null);

        mockMvc.perform(get("/api/polls/1")
                .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표하기")
    void t2() throws Exception {
        Mockito.when(pollService.vote(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(null);

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/polls/1/vote")
                        .param("pollItemsId", "1")
                        .cookie(new Cookie("accessToken", "valid-access-token"))
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 통계 조회")
    void t3() throws Exception {
        Mockito.when(pollService.getPollStatics(Mockito.anyLong())).thenReturn(new PollStaticsResponseDto());

        mockMvc.perform(get("/api/polls/1/statics")
                .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 종료")
    void t4() throws Exception {
        Mockito.doNothing().when(pollService).closePoll(Mockito.anyLong());

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/polls/1/close")
                        .cookie(new Cookie("accessToken", "valid-access-token"))
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 삭제")
    void t5() throws Exception {
        PollDto pollDto = PollDto.builder().pollId(1L).postId(1L).build();
        Mockito.when(pollService.getPoll(Mockito.eq(1L), Mockito.anyLong())).thenReturn(pollDto);
        Mockito.doNothing().when(pollService).deletePoll(Mockito.anyLong());

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/polls/1")
                        .cookie(new Cookie("accessToken", "valid-access-token"))
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("진행중인 투표 Top 1 조회")
    void t6() throws Exception {
        Mockito.when(pollService.getTopPollByStatus(Mockito.any(), Mockito.anyLong())).thenReturn(null);

        mockMvc.perform(get("/api/polls/top/ongoing")
                .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("종료된 투표 Top 1 조회")
    void t7() throws Exception {
        Mockito.when(pollService.getTopPollByStatus(Mockito.any(), Mockito.anyLong())).thenReturn(null);

        mockMvc.perform(get("/api/polls/top/closed")
                .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 생성")
    void t8() throws Exception {
        Mockito.when(pollService.createPoll(Mockito.any(), Mockito.anyLong())).thenReturn(null);

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/polls")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie("accessToken", "valid-access-token"))
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 단일 조회")
    void t9() throws Exception {
        PollDto responseDto = PollDto.builder().pollId(1L).voteTitle("테스트 투표").build();
        Mockito.when(pollService.getPoll(Mockito.anyLong(), Mockito.anyLong())).thenReturn(responseDto);

        mockMvc.perform(get("/api/polls/1")
                .cookie(new Cookie("accessToken", "valid-access-token")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.pollId").value(1L))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.voteTitle").value("테스트 투표"));
    }

    @Test
    @DisplayName("투표하기")
    void t10() throws Exception {
        PollVoteDto responseDto = PollVoteDto.builder().pollId(1L).memberId(1L).build();
        Mockito.when(pollService.vote(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(responseDto);

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/polls/1/vote")
                        .param("pollItemsId", "1")
                        .cookie(new Cookie("accessToken", "valid-access-token"))
        ).andExpect(status().isOk())
         .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.pollId").value(1L))
         .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.result.memberId").value(1L));
    }
}