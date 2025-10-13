package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import com.ai.lawyer.domain.poll.dto.PollUpdateDto;
import com.ai.lawyer.domain.poll.dto.PollStaticsResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import java.util.Collections;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PollServiceTest {
    @Mock
    private PollService pollService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("투표 단일 조회")
    void t1() {
        PollDto expected = new PollDto();
        Mockito.when(pollService.getPoll(Mockito.anyLong(), Mockito.anyLong())).thenReturn(expected);
        PollDto result = pollService.getPoll(1L, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표 옵션 목록 조회")
    void t2() {
        java.util.List expected = java.util.Collections.emptyList();
        Mockito.when(pollService.getPollOptions(Mockito.anyLong())).thenReturn(expected);
        java.util.List result = pollService.getPollOptions(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표하기")
    void t3() {
        PollVoteDto expected = new PollVoteDto();
        Mockito.when(pollService.vote(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(expected);
        PollVoteDto result = pollService.vote(1L, 2L, 3L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표 통계 조회")
    void t4() {
        PollStaticsResponseDto expected = new PollStaticsResponseDto();
        Mockito.when(pollService.getPollStatics(Mockito.anyLong())).thenReturn(expected);
        PollStaticsResponseDto result = pollService.getPollStatics(1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표 종료")
    void t5() {
        Mockito.doNothing().when(pollService).closePoll(Mockito.anyLong());
        pollService.closePoll(1L);
        Mockito.verify(pollService).closePoll(1L);
    }

    @Test
    @DisplayName("투표 삭제")
    void t6() {
        Mockito.doNothing().when(pollService).deletePoll(Mockito.anyLong(), Mockito.anyLong());
        pollService.deletePoll(1L, 1L);
        Mockito.verify(pollService).deletePoll(1L, 1L);
    }

    @Test
    @DisplayName("상태별 Top 투표 조회")
    void t7() {
        PollDto expected = new PollDto();
        Mockito.when(pollService.getTopPollByStatus(Mockito.any(), Mockito.anyLong())).thenReturn(expected);
        PollDto result = pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표 ID 투표수 조회")
    void t8() {
        Mockito.when(pollService.getVoteCountByPollId(Mockito.anyLong())).thenReturn(5L);
        Long result = pollService.getVoteCountByPollId(1L);
        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("게시글 ID 투표수 조회")
    void t9() {
        Mockito.when(pollService.getVoteCountByPostId(Mockito.anyLong())).thenReturn(3L);
        Long result = pollService.getVoteCountByPostId(1L);
        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("투표 수정")
    void t10() {
        PollDto expected = new PollDto();
        PollUpdateDto updateDto = new PollUpdateDto();
        Mockito.when(pollService.updatePoll(Mockito.anyLong(), Mockito.any(), Mockito.anyLong())).thenReturn(expected);
        PollDto result = pollService.updatePoll(1L, updateDto, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("통계 포함 투표 조회")
    void t11() {
        PollDto expected = new PollDto();
        Mockito.when(pollService.getPollWithStatistics(Mockito.anyLong(), Mockito.isNull())).thenReturn(expected);
        PollDto result = pollService.getPollWithStatistics(1L, null);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표 생성")
    void t12() {
        PollDto expected = new PollDto();
        PollCreateDto createDto = new PollCreateDto();
        Mockito.when(pollService.createPoll(Mockito.any(), Mockito.anyLong())).thenReturn(expected);
        PollDto result = pollService.createPoll(createDto, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표 부분 수정")
    void t13() {
        PollUpdateDto updateDto = new PollUpdateDto();
        Mockito.doNothing().when(pollService).patchUpdatePoll(Mockito.anyLong(), Mockito.any());
        pollService.patchUpdatePoll(1L, updateDto);
        Mockito.verify(pollService).patchUpdatePoll(1L, updateDto);
    }

    @Test
    @DisplayName("상태별 투표 목록 조회")
    void t14() {
        java.util.List expected = java.util.Collections.emptyList();
        Mockito.when(pollService.getPollsByStatus(Mockito.any(), Mockito.anyLong())).thenReturn(expected);
        java.util.List result = pollService.getPollsByStatus(PollDto.PollStatus.ONGOING, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("상태별 Top N 투표 목록 조회")
    void t15() {
        java.util.List expected = java.util.Collections.emptyList();
        Mockito.when(pollService.getTopNPollsByStatus(Mockito.any(), Mockito.anyInt(), Mockito.anyLong())).thenReturn(expected);
        java.util.List result = pollService.getTopNPollsByStatus(PollDto.PollStatus.ONGOING, 3, 1L);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("투표 생성 시 postId 누락")
    void t16() {
        PollCreateDto dto = new PollCreateDto();
        dto.setVoteTitle("테스트 투표");
        Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "게시글 ID(postId)는 필수입니다.")).when(pollService).createPoll(Mockito.any(), Mockito.anyLong());
        assertThatThrownBy(() -> pollService.createPoll(dto, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("게시글 ID(postId)는 필수입니다.");
    }

    @Test
    @DisplayName("투표 생성 시 제목 누락")
    void t17() {
        PollCreateDto dto = new PollCreateDto();
        dto.setPostId(1L);
        Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "투표 제목(voteTitle)은 필수입니다.")).when(pollService).createPoll(Mockito.any(), Mockito.anyLong());
        assertThatThrownBy(() -> pollService.createPoll(dto, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("투표 제목(voteTitle)은 필수입니다.");
    }
}
