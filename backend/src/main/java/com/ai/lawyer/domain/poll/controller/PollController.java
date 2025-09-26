package com.ai.lawyer.domain.poll.controller;

import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.entity.PollStatics;
import com.ai.lawyer.domain.poll.service.PollService;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Poll API", description = "투표 관련 API")
@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final PostService postService;

    @Operation(summary = "투표 단일 조회")
    @GetMapping("/{pollId}")
    public PollDto getPoll(@PathVariable Long pollId) {
        return pollService.getPoll(pollId);
    }

    @Operation(summary = "투표 옵션 목록 조회")
    @GetMapping("/{pollId}/options")
    public List<PollOptions> getPollOptions(@PathVariable Long pollId) {
        return pollService.getPollOptions(pollId);
    }

    @Operation(summary = "투표하기")
    @PostMapping("/{pollId}/vote")
    public PollVoteDto vote(@PathVariable Long pollId, @RequestParam Long pollItemsId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = (Long) authentication.getPrincipal();
        return pollService.vote(pollId, pollItemsId, memberId);
    }

    @Operation(summary = "투표 통계 조회")
    @GetMapping("/{pollId}/statics")
    public List<PollStatics> getPollStatics(@PathVariable Long pollId) {
        return pollService.getPollStatics(pollId);
    }

    @Operation(summary = "투표 종료")
    @PutMapping("/{pollId}/close")
    public void closePoll(@PathVariable Long pollId) {
        pollService.closePoll(pollId);
    }

    @Operation(summary = "투표 삭제")
    @DeleteMapping("/{pollId}")
    public void deletePoll(@PathVariable Long pollId) {
        pollService.deletePoll(pollId);
    }

    @Operation(summary = "진행중인 투표 Top 1 조회")
    @GetMapping("/top/ongoing")
    public PollDto getTopOngoingPoll() {
        return pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING);
    }

    @Operation(summary = "종료된 투표 Top 1 조회")
    @GetMapping("/top/closed")
    public PollDto getTopClosedPoll() {
        return pollService.getTopPollByStatus(PollDto.PollStatus.CLOSED);
    }

    @Operation(summary = "진행중인 투표 상세 조회")
    @GetMapping("/top/ongoing-detail")
    public PostDetailDto getTopOngoingPollDetail() {
        PollDto pollDto = pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING);
        return postService.getPostDetailById(pollDto.getPostId());
    }

    @Operation(summary = "종료된 투표 상세 조회")
    @GetMapping("/top/closed-detail")
    public PostDetailDto getTopClosedPollDetail() {
        PollDto pollDto = pollService.getTopPollByStatus(PollDto.PollStatus.CLOSED);
        return postService.getPostDetailById(pollDto.getPostId());
    }

    @Operation(summary = "투표 생성")
    @PostMapping("")
    public PollDto createPoll(@RequestBody PollCreateDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = (Long) authentication.getPrincipal();
        return pollService.createPoll(request, memberId);
    }

    @Operation(summary = "투표 수정")
    @PutMapping("/{pollId}")
    public PollDto updatePoll(@PathVariable Long pollId, @RequestBody com.ai.lawyer.domain.poll.dto.PollUpdateDto pollUpdateDto) {
        return pollService.updatePoll(pollId, pollUpdateDto);
    }
}
