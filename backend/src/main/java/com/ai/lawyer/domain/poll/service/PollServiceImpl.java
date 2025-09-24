package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.entity.*;
import com.ai.lawyer.domain.poll.repository.*;
import com.ai.lawyer.domain.poll.dto.PollDto;
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

        PollVote pollVote = PollVote.builder()
                .poll(poll)
                .pollOptions(pollOptions)
                .memberId(memberId)
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
}