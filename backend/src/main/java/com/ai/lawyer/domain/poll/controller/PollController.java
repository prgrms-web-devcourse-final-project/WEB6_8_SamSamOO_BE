package com.ai.lawyer.domain.poll.controller;

import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.entity.PollStatics;
import com.ai.lawyer.domain.poll.service.PollService;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final PostService postService;

    @GetMapping("/{pollId}")
    public PollDto getPoll(@PathVariable Long pollId) {
        return pollService.getPoll(pollId);
    }

    @GetMapping("/{pollId}/options")
    public List<PollOptions> getPollOptions(@PathVariable Long pollId) {
        return pollService.getPollOptions(pollId);
    }

    @PostMapping("/{pollId}/vote")
    public PollVote vote(@PathVariable Long pollId, @RequestParam Long pollItemsId, @RequestParam Long memberId) {
        return pollService.vote(pollId, pollItemsId, memberId);
    }

    @GetMapping("/{pollId}/statics")
    public List<PollStatics> getPollStatics(@PathVariable Long pollId) {
        return pollService.getPollStatics(pollId);
    }

    @PutMapping("/{pollId}/close")
    public void closePoll(@PathVariable Long pollId) {
        pollService.closePoll(pollId);
    }

    @DeleteMapping("/{pollId}")
    public void deletePoll(@PathVariable Long pollId) {
        pollService.deletePoll(pollId);
    }

    @GetMapping("/top/ongoing")
    public PollDto getTopOngoingPoll() {
        return pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING);
    }

    @GetMapping("/top/closed")
    public PollDto getTopClosedPoll() {
        return pollService.getTopPollByStatus(PollDto.PollStatus.CLOSED);
    }

    @GetMapping("/top/ongoing-detail")
    public PostDetailDto getTopOngoingPollDetail() {
        PollDto pollDto = pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING);
        return postService.getPostDetailById(pollDto.getPostId());
    }

    @GetMapping("/top/closed-detail")
    public PostDetailDto getTopClosedPollDetail() {
        PollDto pollDto = pollService.getTopPollByStatus(PollDto.PollStatus.CLOSED);
        return postService.getPostDetailById(pollDto.getPostId());
    }
}