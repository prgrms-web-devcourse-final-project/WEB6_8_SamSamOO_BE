package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollUpdateDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import com.ai.lawyer.domain.poll.dto.PollStaticsResponseDto;
import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.poll.repository.PollRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PollAutoCloseTest {
    @Autowired
    private PollService pollService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PollRepository pollRepository;

    @Test
    @DisplayName("autoClose 예약 종료 자동 처리 기능(정책 우회)")
    void autoCloseTest() throws Exception {
        // 테스트용 member 생성
        Member member = Member.builder()
                .loginId("testuser@sample.com")
                .password("pw")
                .age(20)
                .gender(Member.Gender.MALE)
                .role(Member.Role.USER)
                .name("테스트유저")
                .build();
        member = memberRepository.save(member);

        // 테스트용 post 생성
        Post post = new Post();
        post.setPostName("테스트용 게시글");
        post.setPostContent("테스트 내용");
        post.setCategory("테스트");
        post.setCreatedAt(java.time.LocalDateTime.now());
        post.setMember(member);
        post = postRepository.save(post);

        PollCreateDto createDto = new PollCreateDto();
        createDto.setPostId(post.getPostId());
        createDto.setVoteTitle("autoClose 테스트");
        createDto.setReservedCloseAt(java.time.LocalDateTime.now().plusHours(1).plusSeconds(1));
        // 투표 항목 2개 추가
        var option1 = new com.ai.lawyer.domain.poll.dto.PollOptionCreateDto();
        option1.setContent("찬성");
        var option2 = new com.ai.lawyer.domain.poll.dto.PollOptionCreateDto();
        option2.setContent("반대");
        createDto.setPollOptions(java.util.Arrays.asList(option1, option2));
        PollDto created = pollService.createPoll(createDto, member.getMemberId());

        // 2. 생성 직후 상태는 ONGOING이어야 함
        PollDto ongoing = pollService.getPoll(created.getPollId());
        assertThat(ongoing.getStatus()).isEqualTo(PollDto.PollStatus.ONGOING);

        // 3. reservedCloseAt을 DB에서 과거로 강제 변경
        var poll = pollRepository.findById(created.getPollId()).get();
        var reservedCloseAtField = poll.getClass().getDeclaredField("reservedCloseAt");
        reservedCloseAtField.setAccessible(true);
        reservedCloseAtField.set(poll, java.time.LocalDateTime.now().minusSeconds(1));
        pollRepository.save(poll);

        // 4. getPoll 호출 시 자동 종료(CLOSED)로 바뀌는지 확인
        PollDto closed = pollService.getPoll(created.getPollId());
        assertThat(closed.getStatus()).isEqualTo(PollDto.PollStatus.CLOSED);
    }
}
