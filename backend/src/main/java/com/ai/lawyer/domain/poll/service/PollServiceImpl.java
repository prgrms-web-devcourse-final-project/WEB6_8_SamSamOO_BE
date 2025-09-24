package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.entity.*;
import com.ai.lawyer.domain.poll.repository.*;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;
    private final PollOptionsRepository pollOptionsRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollStaticsRepository pollStaticsRepository;
    private final MemberRepository memberRepository;

    @Override
    public PollDto getPoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
        return convertToDto(poll);
    }

    @Override
    public List<PollOptions> getPollOptions(Long pollId) {
        return pollOptionsRepository.findAll().stream()
                .filter(opt -> opt.getPoll().getPollId().equals(pollId))
                .toList();
    }

    @Override
    public PollVote vote(Long pollId, Long pollItemsId, Long memberId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
        PollOptions pollOptions = pollOptionsRepository.findById(pollItemsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표 항목을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        PollVote pollVote = PollVote.builder()
                .poll(poll)
                .pollOptions(pollOptions)
                .member(member)
                .build();
        return pollVoteRepository.save(pollVote);
    }

    @Override
    public List<PollStatics> getPollStatics(Long pollId) {
        return pollStaticsRepository.findAll().stream()
                .filter(stat -> stat.getPoll().getPollId().equals(pollId))
                .toList();
    }

    @Override
    public void closePoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
        poll.setStatus(Poll.PollStatus.CLOSED);
        pollRepository.save(poll);
    }

    @Override
    public void deletePoll(Long pollId) {
        if (!pollRepository.existsById(pollId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다.");
        }
        pollRepository.deleteById(pollId);
    }

    @Override
    public PollDto getTopPollByStatus(PollDto.PollStatus status) {
        List<Object[]> result = pollVoteRepository.findTopPollByStatus(Poll.PollStatus.valueOf(status.name()));
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 상태의 투표가 없습니다.");
        }
        Long pollId = (Long) result.get(0)[0];
        return getPoll(pollId);
    }

    @Override
    public Long getVoteCountByPollId(Long pollId) {
        return pollVoteRepository.countByPollId(pollId);
    }

    @Override
    public Long getVoteCountByPostId(Long postId) {
        Poll poll = pollRepository.findAll().stream()
                .filter(p -> p.getPost() != null && p.getPost().getPostId().equals(postId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 게시글의 투표가 없습니다."));
        return getVoteCountByPollId(poll.getPollId());
    }


    public PollDto updatePoll(Long pollId, PollDto pollDto) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "수정할 투표를 찾을 수 없습니다."));
        if (getVoteCountByPollId(pollId) > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "투표가 진행된 투표는 수정할 수 없습니다.");
        }
        poll.setVoteTitle(pollDto.getVoteTitle());
        poll.setStatus(Poll.PollStatus.valueOf(pollDto.getStatus().name()));
        poll.setClosedAt(pollDto.getClosedAt());
        Poll updated = pollRepository.save(poll);
        return convertToDto(updated);
    }

    private PollDto convertToDto(Poll poll) {
        return PollDto.builder()
                .pollId(poll.getPollId())
                .postId(poll.getPost() != null ? poll.getPost().getPostId() : null)
                .voteTitle(poll.getVoteTitle())
                .status(PollDto.PollStatus.valueOf(poll.getStatus().name()))
                .createdAt(poll.getCreatedAt())
                .closedAt(poll.getClosedAt())
                .build();
    }

    private String getAgeGroup(Integer age) {
        if (age == null) return "기타";
        if (age < 20) return "10대";
        if (age < 30) return "20대";
        if (age < 40) return "30대";
        if (age < 50) return "40대";
        if (age < 60) return "50대";
        if (age < 70) return "60대";
        if (age < 80) return "70대";
        return "80대 이상";
    }
}