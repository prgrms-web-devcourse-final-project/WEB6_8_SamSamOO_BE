package com.ai.lawyer.domain.poll.controller;

import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.entity.PollStatics;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.service.PollService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class PollControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private PollService pollService;

    private PollDto pollDto;
    private PollOptions pollOptions;
    private PollVote pollVote;
    private PollStatics pollStatics;

    @BeforeEach
    void setUp() {
        pollDto = PollDto.builder()
                .pollId(1L)
                .voteTitle("테스트 투표")
                .status(PollDto.PollStatus.ONGOING)
                .createdAt(LocalDateTime.now())
                .build();
        pollOptions = PollOptions.builder()
                .pollItemsId(10L)
                .option("옵션1")
                .build();
        pollVote = PollVote.builder()
                .pollOptions(pollOptions)
                .build();
        pollStatics = PollStatics.builder()
                .poll(pollDto.toEntity())
                .build();
    }

    @Test
    @DisplayName("투표 단일 조회 성공")
    void getPoll_success() throws Exception {
        when(pollService.getPoll(1L)).thenReturn(pollDto);
        mvc.perform(get("/api/polls/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollId").value(1L));
    }

    @Test
    @DisplayName("투표 옵션 목록 조회 성공")
    void getPollOptions_success() throws Exception {
        when(pollService.getPollOptions(1L)).thenReturn(List.of(pollOptions));
        mvc.perform(get("/api/polls/1/options"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표하기 성공")
    void vote_success() throws Exception {
        when(pollService.vote(eq(1L), eq(10L), eq(2L))).thenReturn(pollVote);
        mvc.perform(post("/api/polls/1/vote")
                .param("pollItemsId", "10")
                .param("memberId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 통계 조회 성공")
    void getPollStatics_success() throws Exception {
        when(pollService.getPollStatics(1L)).thenReturn(List.of(pollStatics));
        mvc.perform(get("/api/polls/1/statics"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 종료 성공")
    void closePoll_success() throws Exception {
        mvc.perform(put("/api/polls/1/close"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("투표 삭제 성공")
    void deletePoll_success() throws Exception {
        mvc.perform(delete("/api/polls/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("진행중인 투표 Top 1 조회 성공")
    void getTopOngoingPoll_success() throws Exception {
        when(pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING)).thenReturn(pollDto);
        mvc.perform(get("/api/polls/top/ongoing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollId").value(1L));
    }

    @Test
    @DisplayName("종료된 투표 Top 1 조회 성공")
    void getTopClosedPoll_success() throws Exception {
        when(pollService.getTopPollByStatus(PollDto.PollStatus.CLOSED)).thenReturn(pollDto);
        mvc.perform(get("/api/polls/top/closed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollId").value(1L));
    }
}

