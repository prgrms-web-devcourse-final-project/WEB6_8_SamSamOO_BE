package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.entity.*;
import com.ai.lawyer.domain.poll.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceImplTest {
    @Mock PollRepository pollRepository;
    @Mock PollOptionsRepository pollOptionsRepository;
    @Mock PollVoteRepository pollVoteRepository;
    @Mock PollStaticsRepository pollStaticsRepository;
    @Mock MemberRepository memberRepository;
    @InjectMocks PollServiceImpl pollService;

    private Poll poll;
    private PollOptions pollOptions;
    private Member member;
    private PollVote pollVote;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(1L).name("테스터").build();
        poll = Poll.builder().pollId(10L).voteTitle("테스트 투표").status(Poll.PollStatus.ONGOING).build();
        pollOptions = PollOptions.builder().pollItemsId(100L).poll(poll).option("옵션1").build();
        pollVote = PollVote.builder().poll(poll).pollOptions(pollOptions).member(member).build();
    }

    @Test
    @DisplayName("투표 단일 조회 성공")
    void getPoll_success() {
        when(pollRepository.findById(10L)).thenReturn(Optional.of(poll));
        PollDto result = pollService.getPoll(10L);
        assertThat(result.getPollId()).isEqualTo(10L);
        assertThat(result.getVoteTitle()).isEqualTo("테스트 투표");
    }

    @Test
    @DisplayName("투표 단일 조회 실패 - 없음")
    void getPoll_fail() {
        when(pollRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pollService.getPoll(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("투표를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("투표 옵션 목록 조회")
    void getPollOptions() {
        when(pollOptionsRepository.findAll()).thenReturn(List.of(pollOptions));
        List<PollOptions> options = pollService.getPollOptions(10L);
        assertThat(options).hasSize(1);
        assertThat(options.get(0).getOption()).isEqualTo("옵션1");
    }

    @Test
    @DisplayName("투표하기 성공")
    void vote_success() {
        when(pollRepository.findById(10L)).thenReturn(Optional.of(poll));
        when(pollOptionsRepository.findById(100L)).thenReturn(Optional.of(pollOptions));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(pollVoteRepository.save(any(PollVote.class))).thenReturn(pollVote);
        PollVote result = pollService.vote(10L, 100L, 1L);
        assertThat(result.getMember().getMemberId()).isEqualTo(1L);
        assertThat(result.getPoll().getPollId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("투표하기 실패 - 투표 없음")
    void vote_fail_noPoll() {
        when(pollRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pollService.vote(10L, 100L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("투표를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("투표하기 실패 - 옵션 없음")
    void vote_fail_noOption() {
        when(pollRepository.findById(10L)).thenReturn(Optional.of(poll));
        when(pollOptionsRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pollService.vote(10L, 100L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("투표 항목을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("투표하기 실패 - 회원 없음")
    void vote_fail_noMember() {
        when(pollRepository.findById(10L)).thenReturn(Optional.of(poll));
        when(pollOptionsRepository.findById(100L)).thenReturn(Optional.of(pollOptions));
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pollService.vote(10L, 100L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다.");
    }
}
