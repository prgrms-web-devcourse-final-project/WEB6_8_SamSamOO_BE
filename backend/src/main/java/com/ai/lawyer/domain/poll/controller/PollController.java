package com.ai.lawyer.domain.poll.controller;

import com.ai.lawyer.domain.poll.dto.*;
import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.service.PollService;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.service.PostService;
import com.ai.lawyer.global.response.ApiResponse;
import com.ai.lawyer.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Poll API", description = "투표 관련 API")
@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
@Slf4j
public class PollController {

    private final PollService pollService;
    private final PostService postService;

    @Operation(summary = "투표 단일 조회")
    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResponse<PollDto>> getPoll(@PathVariable Long pollId) {
        Long memberId = AuthUtil.getCurrentMemberId();
        log.info("PollController getPoll: memberId={}", memberId);
        PollDto poll = pollService.getPoll(pollId, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "투표 단일 조회 성공", poll));
    }

    @Operation(summary = "투표하기")
    @PostMapping("/{pollId}/vote")
    public ResponseEntity<ApiResponse<PollVoteDto>> vote(@PathVariable Long pollId, @RequestParam Long pollItemsId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = Long.parseLong(authentication.getName());
        PollVoteDto result = pollService.vote(pollId, pollItemsId, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "투표가 성공적으로 완료되었습니다.", result));
    }

    @Operation(summary = "투표 통계 조회 (항목별 나이/성별 카운트)")
    @GetMapping("/{pollId}/statics")
    public ResponseEntity<ApiResponse<PollStaticsResponseDto>> getPollStatics(@PathVariable Long pollId) {
        PollStaticsResponseDto statics = pollService.getPollStatics(pollId);
        return ResponseEntity.ok(new ApiResponse<>(200, "투표 통계 조회 성공", statics));
    }

    @Operation(summary = "투표 종료")
    @PutMapping("/{pollId}/close")
    public ResponseEntity<ApiResponse<Void>> closePoll(@PathVariable Long pollId) {
        pollService.closePoll(pollId);
        return ResponseEntity.ok(new ApiResponse<>(200, "투표가 종료되었습니다.", null));
    }

    @Operation(summary = "투표 수정")
    @PutMapping("/{pollId}")
    public ResponseEntity<ApiResponse<PollDto>> updatePoll(@PathVariable Long pollId, @RequestBody PollUpdateDto pollUpdateDto) {
        Long currentMemberId = AuthUtil.getCurrentMemberId();
        PollDto updated = pollService.updatePoll(pollId, pollUpdateDto, currentMemberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "투표가 수정되었습니다.", updated));
    }

    @Operation(summary = "투표 삭제")
    @DeleteMapping("/{pollId}")
    public ResponseEntity<ApiResponse<Void>> deletePoll(@PathVariable Long pollId) {
        Long currentMemberId = AuthUtil.getCurrentMemberId();
        PollDto poll = pollService.getPoll(pollId, currentMemberId);
        if (!poll.getPostId().equals(currentMemberId)) {
            return ResponseEntity.status(403).body(new ApiResponse<>(403, "본인만 투표를 삭제할 수 있습니다.", null));
        }
        pollService.deletePoll(pollId);
        return ResponseEntity.ok(new ApiResponse<>(200, "투표가 삭제되었습니다.", null));
    }

    @Operation(summary = "진행중인 투표 Top 1 조회")
    @GetMapping("/top/ongoing")
    public ResponseEntity<ApiResponse<PollDto>> getTopOngoingPoll() {
        Long memberId = AuthUtil.getCurrentMemberId();
        PollDto poll = pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "진행중인 투표 Top 1 조회 성공", poll));
    }

    @Operation(summary = "종료된 투표 Top 1 조회")
    @GetMapping("/top/closed")
    public ResponseEntity<ApiResponse<PollDto>> getTopClosedPoll() {
        Long memberId = AuthUtil.getCurrentMemberId();
        PollDto poll = pollService.getTopPollByStatus(PollDto.PollStatus.CLOSED, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "종료된 투표 Top 1 조회 성공", poll));
    }

//    @Operation(summary = "진행중인 투표 상세 조회")
//    @GetMapping("/top/ongoing-detail")
//    public PostDetailDto getTopOngoingPollDetail() {
//        PollDto pollDto = pollService.getTopPollByStatus(PollDto.PollStatus.ONGOING);
//        return postService.getPostDetailById(pollDto.getPostId());
//    }
//
//    @Operation(summary = "종료된 투표 상세 조회")
//    @GetMapping("/top/closed-detail")
//    public PostDetailDto getTopClosedPollDetail() {
//        PollDto pollDto = pollService.getTopPollByStatus(PollDto.PollStatus.CLOSED);
//        return postService.getPostDetailById(pollDto.getPostId());
//    }

    @Operation(summary = "투표 생성")
    @PostMapping("")
    public ResponseEntity<ApiResponse<PollDto>> createPoll(@RequestBody PollCreateDto pollCreateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = Long.parseLong(authentication.getName());
        PollDto created = pollService.createPoll(pollCreateDto, memberId);
        return ResponseEntity.ok(new ApiResponse<>(201, "투표가 생성되었습니다.", created));
    }

    @Operation(summary = "진행중인 투표 전체 목록 조회")
    @GetMapping("/ongoing")
    public ResponseEntity<ApiResponse<List<PollDto>>> getOngoingPolls() {
        Long memberId = AuthUtil.getCurrentMemberId();
        List<PollDto> polls = pollService.getPollsByStatus(PollDto.PollStatus.ONGOING, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "진행중인 투표 전체 목록 조회 성공", polls));
    }

    @Operation(summary = "종료된 투표 전체 목록 조회")
    @GetMapping("/closed")
    public ResponseEntity<ApiResponse<List<PollDto>>> getClosedPolls() {
        Long memberId = AuthUtil.getCurrentMemberId();
        List<PollDto> polls = pollService.getPollsByStatus(PollDto.PollStatus.CLOSED, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "종료된 투표 전체 목록 조회 성공", polls));
    }

//    @Operation(summary = "종료된 투표 Top N 조회")
//    @GetMapping("/top/closed-list") //검색조건 : pi/polls/top/closed-list?size=3
//    public ResponseEntity<ApiResponse<List<PollDto>>> getTopClosedPolls(@RequestParam(defaultValue = "3") int size) {
//        List<PollDto> polls = pollService.getTopNPollsByStatus(PollDto.PollStatus.CLOSED, size);
//        String message = String.format("종료된 투표 Top %d 조회 성공", size);
//        return ResponseEntity.ok(new ApiResponse<>(200, message, polls));
//    }
//
//    @Operation(summary = "진행중인 투표 Top N 조회")
//    @GetMapping("/top/ongoing-list") //검색조건 : api/polls/top/ongoing-list?size=3
//    public ResponseEntity<ApiResponse<List<PollDto>>> getTopOngoingPolls(@RequestParam(defaultValue = "3") int size) {
//        List<PollDto> polls = pollService.getTopNPollsByStatus(PollDto.PollStatus.ONGOING, size);
//        String message = String.format("진행중인 투표 Top %d 조회 성공", size);
//        return ResponseEntity.ok(new ApiResponse<>(200, message, polls));
//    }

    @Operation(summary = "index(순번)로 투표하기")
    @PostMapping("/{pollId}/voting")
    public ResponseEntity<ApiResponse<PollVoteDto>> voteByIndex(@PathVariable Long pollId, @RequestParam int index) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = Long.parseLong(authentication.getName());
        List<PollOptions> options = pollService.getPollOptions(pollId);
        if (index < 1 || index > options.size()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "index가 옵션 범위를 벗어났습니다.");
        }
        Long pollItemsId = options.get(index - 1).getPollItemsId();
        PollVoteDto result = pollService.vote(pollId, pollItemsId, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "투표가 성공적으로 완료되었습니다.", result));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        String message = ex.getReason();
        return ResponseEntity.status(code).body(new ApiResponse<>(code, message, null));
    }
}
