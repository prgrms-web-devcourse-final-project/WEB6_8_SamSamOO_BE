package com.ai.lawyer.domain.poll.controller;

import com.ai.lawyer.domain.poll.service.PollService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.ai.lawyer.global.security.SecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.junit.jupiter.api.DisplayName;

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
    @MockBean
    private PollService pollService;
    @MockBean
    private com.ai.lawyer.domain.post.service.PostService postService;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private com.ai.lawyer.global.jwt.TokenProvider tokenProvider;
    @MockBean
    private com.ai.lawyer.global.jwt.CookieUtil cookieUtil;
    @MockBean
    private com.ai.lawyer.domain.member.repositories.MemberRepository memberRepository;
    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("투표 단일 조회")
    @WithMockUser(username="1")
    void t1() throws Exception {
        Mockito.when(pollService.getPoll(Mockito.anyLong())).thenReturn(null);
        mockMvc.perform(get("/api/polls/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 옵션 목록 조회")
    @WithMockUser(username="1")
    void t2() throws Exception {
        Mockito.when(pollService.getPollOptions(Mockito.anyLong())).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/api/polls/1/options"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표하기")
    @WithMockUser(username="1")
    void t3() throws Exception {
        Mockito.when(pollService.vote(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(null);
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/polls/1/vote")
                        .param("pollItemsId", "1")
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 통계 조회")
    @WithMockUser(username="1")
    void t4() throws Exception {
        Mockito.when(pollService.getPollStatics(Mockito.anyLong())).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/api/polls/1/statics"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 종료")
    @WithMockUser(username="1")
    void t5() throws Exception {
        Mockito.doNothing().when(pollService).closePoll(Mockito.anyLong());
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/polls/1/close")
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 삭제")
    @WithMockUser(username="1")
    void t6() throws Exception {
        Mockito.doNothing().when(pollService).deletePoll(Mockito.anyLong());
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/polls/1")
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("진행중인 투표 Top 1 조회")
    @WithMockUser(username="1")
    void t7() throws Exception {
        Mockito.when(pollService.getTopPollByStatus(Mockito.any())).thenReturn(null);
        mockMvc.perform(get("/api/polls/top/ongoing"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("종료된 투표 Top 1 조회")
    @WithMockUser(username="1")
    void t8() throws Exception {
        Mockito.when(pollService.getTopPollByStatus(Mockito.any())).thenReturn(null);
        mockMvc.perform(get("/api/polls/top/closed"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 생성")
    @WithMockUser(username="1")
    void t9() throws Exception {
        Mockito.when(pollService.createPoll(Mockito.any(), Mockito.anyLong())).thenReturn(null);
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/polls")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 단일 조회")
    @WithMockUser(username="1")
    void t10() throws Exception {
        PollDto responseDto = PollDto.builder().pollId(1L).voteTitle("테스트 투표").build();
        Mockito.when(pollService.getPoll(Mockito.anyLong())).thenReturn(responseDto);
        mockMvc.perform(get("/api/polls/1"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.pollId").value(1L))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.voteTitle").value("테스트 투표"));
    }

    @Test
    @DisplayName("투표하기")
    @WithMockUser(username="1")
    void t11() throws Exception {
        PollVoteDto responseDto = PollVoteDto.builder().pollId(1L).memberId(1L).build();
        Mockito.when(pollService.vote(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(responseDto);
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/polls/1/vote")
                        .param("pollItemsId", "1")
        ).andExpect(status().isOk())
         .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.pollId").value(1L))
         .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.memberId").value(1L));
    }
}
