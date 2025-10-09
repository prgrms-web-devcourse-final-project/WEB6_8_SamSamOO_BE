package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollOptionCreateDto;
import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.poll.repository.PollRepository;
import com.ai.lawyer.domain.poll.repository.PollOptionsRepository;
import com.ai.lawyer.domain.poll.repository.PollVoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PollAutoCloseTest {
    @Mock
    private PollRepository pollRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PollOptionsRepository pollOptionsRepository;
    @Mock
    private PollVoteRepository pollVoteRepository;
    @InjectMocks
    private PollServiceImpl pollService;

    @Test
    @DisplayName("autoClose 예약 종료 자동 처리 기능(정책 우회)")
    void autoCloseTest() {
        // 필요한 Mock 객체 및 반환값 설정 예시
        Member member = Member.builder()
                .loginId("testuser@sample.com")
                .password("pw")
                .age(20)
                .gender(Member.Gender.MALE)
                .role(Member.Role.USER)
                .name("테스트유저")
                .build();
        // memberId를 명확히 지정
        member.setMemberId(1L);
        lenient().when(memberRepository.save(any(Member.class))).thenReturn(member);

        Post post = new Post();
        post.setPostId(1L);
        post.setPostName("테스트용 게시글");
        post.setPostContent("테스트 내용");
        post.setCategory("테스트");
        post.setCreatedAt(LocalDateTime.now());
        post.setMember(member);
        post.setPoll(null);
        lenient().when(postRepository.save(any(Post.class))).thenReturn(post);

        Poll poll = new Poll();
        poll.setPollId(1L);
        poll.setReservedCloseAt(LocalDateTime.now().plusHours(1).plusSeconds(1));
        poll.setStatus(Poll.PollStatus.ONGOING);
        lenient().when(pollRepository.save(any(Poll.class))).thenReturn(poll);

        // postRepository.save(post) 반환값에 poll이 반영된 post 객체 설정
        Post postWithPoll = new Post();
        postWithPoll.setPostId(1L);
        postWithPoll.setPostName("테스트용 게시글");
        postWithPoll.setPostContent("테스트 내용");
        postWithPoll.setCategory("테스트");
        postWithPoll.setCreatedAt(post.getCreatedAt());
        postWithPoll.setMember(member);
        postWithPoll.setPoll(poll);
        lenient().when(postRepository.save(argThat(p -> p.getPoll() != null))).thenReturn(postWithPoll);

        PollCreateDto createDto = new PollCreateDto();
        createDto.setPostId(1L);
        createDto.setVoteTitle("autoClose 테스트");
        createDto.setReservedCloseAt(LocalDateTime.now().plusHours(1).plusSeconds(1));
        PollOptionCreateDto option1 = new PollOptionCreateDto();
        option1.setContent("찬성");
        PollOptionCreateDto option2 = new PollOptionCreateDto();
        option2.setContent("반대");
        createDto.setPollOptions(asList(option1, option2));

        // PollOptions 저장에 대한 Mock 동작 추가 (여러 번 호출될 수 있으므로 각각 반환)
        PollOptions pollOptions1 = PollOptions.builder()
                .poll(poll)
                .option("찬성")
                .build();
        PollOptions pollOptions2 = PollOptions.builder()
                .poll(poll)
                .option("반대")
                .build();
        lenient().when(pollOptionsRepository.save(any(PollOptions.class))).thenReturn(pollOptions1, pollOptions2);

        // pollVoteRepository.countByPollId의 반환값 설정
        lenient().when(pollVoteRepository.countByPollId(anyLong())).thenReturn(0L);

        // reservedCloseAt을 과거로 변경하여 자동 종료 테스트
        poll.setReservedCloseAt(LocalDateTime.now().minusSeconds(1));
        poll.setStatus(Poll.PollStatus.CLOSED);
        given(pollRepository.findById(eq(1L))).willReturn(java.util.Optional.of(poll));
        PollDto closed = pollService.getPoll(1L);
        assertThat(closed.getStatus()).isEqualTo(PollDto.PollStatus.CLOSED);
    }
}
