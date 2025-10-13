package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollDto.PollStatus;
import com.ai.lawyer.domain.post.dto.*;
import com.ai.lawyer.domain.post.service.PostService;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.response.ApiResponse;
import com.ai.lawyer.global.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static com.ai.lawyer.domain.poll.entity.Poll.PollStatus.CLOSED;
import static com.ai.lawyer.domain.poll.entity.Poll.PollStatus.ONGOING;

@Tag(name = "Post API", description = "게시글 관련 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;

    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> createPost(@RequestBody PostRequestDto postRequestDto) {
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        PostDto created = postService.createPost(postRequestDto, memberId);
        return ResponseEntity.ok(new ApiResponse<>(201, "게시글이 등록되었습니다.", created));
    }

//    @PostMapping("/postdev")
//    public ResponseEntity<ApiResponse<PostDto>> createPostDev(@RequestBody PostRequestDto postRequestDto, @RequestParam Long memberId) {
//        PostDto created = postService.createPost(postRequestDto, memberId);
//        return ResponseEntity.ok(new ApiResponse<>(201, "[DEV] 게시글이 등록되었습니다.", created));
//    }

    @Operation(summary = "게시글 전체 조회")
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<PostDetailDto>>> getAllPosts() {
        Long memberId = AuthUtil.getCurrentMemberId();
        List<PostDetailDto> posts = postService.getAllPosts(memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글 전체 조회 성공", posts));
    }

    @Operation(summary = "게시글 간편 전체 조회")
    @GetMapping("/simplePost")
    public ResponseEntity<ApiResponse<List<PostSimpleDto>>> getAllSimplePosts() {
        List<PostSimpleDto> posts = postService.getAllSimplePosts();
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글 간편 전체 조회 성공", posts));
    }

    @Operation(summary = "게시글 단일 조회")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailDto>> getPostById(@PathVariable Long postId) {
        Long memberId = AuthUtil.getCurrentMemberId();
        PostDetailDto postDto = postService.getPostDetailById(postId, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글 단일 조회 성공", postDto));
    }

    @Operation(summary = "회원별 게시글 목록 조회")
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<PostDetailDto>>> getPostsByMember(@PathVariable Long memberId) {
        List<PostDetailDto> posts = postService.getPostsByMemberId(memberId).stream()
            .map(postDto -> postService.getPostDetailById(postDto.getPostId(), AuthUtil.getCurrentMemberId()))
            .toList();
        return ResponseEntity.ok(new ApiResponse<>(200, "회원별 게시글 목록 조회 성공", posts));
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailDto>> updatePost(@PathVariable Long postId, @RequestBody PostUpdateDto postUpdateDto) {
        PostDetailDto postDetail = postService.getPostDetailById(postId, AuthUtil.getAuthenticatedMemberId());
        Long postOwnerId = postDetail.getPost().getMemberId();
        AuthUtil.validateOwnerOrAdmin(postOwnerId);
        postService.updatePost(postId, postUpdateDto);
        PostDetailDto updated = postService.getPostDetailById(postId, AuthUtil.getAuthenticatedMemberId());
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글이 수정되었습니다.", updated));
    }

    @Operation(summary = "게시글 부분 수정(PATCH)")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailDto>> patchUpdatePost(@PathVariable Long postId, @RequestBody PostUpdateDto postUpdateDto) {
        PostDetailDto postDetail = postService.getPostDetailById(postId, AuthUtil.getAuthenticatedMemberId());
        Long postOwnerId = postDetail.getPost().getMemberId();
        AuthUtil.validateOwnerOrAdmin(postOwnerId);
        postService.patchUpdatePost(postId, postUpdateDto);
        PostDetailDto updated = postService.getPostDetailById(postId, AuthUtil.getAuthenticatedMemberId());
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글이 수정되었습니다.", updated));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
        PostDetailDto postDetail = postService.getPostDetailById(postId, AuthUtil.getAuthenticatedMemberId());
        Long postOwnerId = postDetail.getPost().getMemberId();
        AuthUtil.validateOwnerOrAdmin(postOwnerId);
        postService.deletePost(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글이 삭제되었습니다.", null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        String message = ex.getReason();
        return ResponseEntity.status(code).body(new ApiResponse<>(code, message, null));
    }

     @Operation(summary = "본인 게시글 단일 조회")
     @GetMapping("/my/{postId}")
     public ResponseEntity<ApiResponse<PostDto>> getMyPostById(@PathVariable Long postId) {
         Long memberId = AuthUtil.getAuthenticatedMemberId();
         PostDto postDto = postService.getMyPostById(postId, memberId);
         return ResponseEntity.ok(new ApiResponse<>(200, "본인 게시글 단일 조회 성공", postDto));
     }

    @Operation(summary = "본인 게시글 전체 조회")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<PostDto>>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("updatedAt").descending());
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        Page<PostDto> response = postService.getMyPosts(pageable, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "본인 게시글 전체 조회 성공", response));
    }

    @Operation(summary = "게시글+투표 동시 등록")
    @PostMapping("/createPost")
    public ResponseEntity<ApiResponse<PostDetailDto>> createPostWithPoll(@RequestBody PostWithPollCreateDto dto) {
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        PostDetailDto result = postService.createPostWithPoll(dto, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글+투표 등록 완료", result));
    }

    @Operation(summary = "게시글 페이징 조회")
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PostPageDto>> getPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Long memberId = AuthUtil.getCurrentMemberId();
        Page<PostDto> pageResult = postService.getPostsPaged(pageable, memberId);
        PostPageDto response = new PostPageDto(pageResult);
        return ResponseEntity.ok(new ApiResponse<>(200, "페이징 게시글 조회 성공", response));
    }

    @Operation(summary = "진행중 투표 게시글 페이징 조회")
    @GetMapping("/ongoingPaged")
    public ResponseEntity<ApiResponse<PostPageDto>> getOngoingPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Long memberId = AuthUtil.getCurrentMemberId();
        Page<PostDto> pageResult = postService.getOngoingPostsPaged(pageable, memberId);
        PostPageDto response = new PostPageDto(pageResult);
        return ResponseEntity.ok(new ApiResponse<>(200, "진행중 투표 게시글 페이징 조회 성공", response));
    }

    @Operation(summary = "마감 투표 게시글 페이징 조회")
    @GetMapping("/closedPaged")
    public ResponseEntity<ApiResponse<PostPageDto>> getClosedPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Long memberId = AuthUtil.getCurrentMemberId();
        Page<PostDto> pageResult = postService.getClosedPostsPaged(pageable, memberId);
        PostPageDto response = new PostPageDto(pageResult);
        return ResponseEntity.ok(new ApiResponse<>(200, "마감된 투표 게시글 페이징 조회 성공", response));
    }

    @Operation(summary = "진행중인 투표 Top N 조회")
    @GetMapping("/top/ongoingList")
    public ResponseEntity<ApiResponse<List<PostDto>>> getTopNOngoingPolls(@RequestParam(defaultValue = "3") int size) {
        Long memberId = AuthUtil.getCurrentMemberId();
        List<PostDto> posts = postService.getTopNPollsByStatus(
            PollStatus.valueOf(ONGOING.name()), size, memberId);
        String message = String.format("진행중인 투표 Top %d 조회 성공", size);
        return ResponseEntity.ok(new ApiResponse<>(200, message, posts));
    }

    @Operation(summary = "마감된 투표 Top N 조회")
    @GetMapping("/top/closedList")
    public ResponseEntity<ApiResponse<List<PostDto>>> getTopNClosedPolls(@RequestParam(defaultValue = "3") int size) {
        Long memberId = AuthUtil.getCurrentMemberId();
        List<PostDto> posts = postService.getTopNPollsByStatus(
            PollStatus.valueOf(CLOSED.name()), size, memberId);
        String message = String.format("종료된 투표 Top %d 조회 성공", size);
        return ResponseEntity.ok(new ApiResponse<>(200, message, posts));
    }

    @Operation(summary = "진행중인 투표 Top 1 조회")
    @GetMapping("/top/ongoing")
    public ResponseEntity<ApiResponse<PostDto>> getTopOngoingPoll() {
        Long memberId = AuthUtil.getCurrentMemberId();
        PostDto post = postService.getTopPollByStatus(
            PollStatus.valueOf(ONGOING.name()), memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "진행중인 투표 Top 1 조회 성공", post));
    }

    @Operation(summary = "마감된 투표 Top 1 조회")
    @GetMapping("/top/closed")
    public ResponseEntity<ApiResponse<PostDto>> getTopClosedPoll() {
        Long memberId = AuthUtil.getCurrentMemberId();
        PostDto post = postService.getTopPollByStatus(
            PollStatus.valueOf(CLOSED.name()), memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "마감된 투표 Top 1 조회 성공", post));
    }

    @Operation(summary = "내가 참여한 진행중 투표 게시글 페이징 조회")
    @GetMapping("/my/ongoingPaged")
    public ResponseEntity<ApiResponse<PostPageDto>> getMyOngoingPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        Page<PostDto> pageResult = postService.getMyOngoingPostsPaged(pageable, memberId);
        PostPageDto response = new PostPageDto(pageResult);
        return ResponseEntity.ok(new ApiResponse<>(200, "내가 참여한 진행중 투표 게시글 페이징 조회 성공", response));
    }

    @Operation(summary = "내가 참여한 마감 투표 게시글 페이징 조회")
    @GetMapping("/my/closedPaged")
    public ResponseEntity<ApiResponse<PostPageDto>> getMyClosedPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        Page<PostDto> pageResult = postService.getMyClosedPostsPaged(pageable, memberId);
        PostPageDto response = new PostPageDto(pageResult);
        return ResponseEntity.ok(new ApiResponse<>(200, "내가 참여한 마감 투표 게시글 페이징 조회 성공", response));
    }

    @Operation(summary = "내가 참여한 모든 투표 게시글 페이징 조회")
    @GetMapping("/my/votedPaged")
    public ResponseEntity<ApiResponse<PostPageDto>> getMyVotedPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Long memberId = AuthUtil.getAuthenticatedMemberId();
        Page<PostDto> pageResult = postService.getMyVotedPostsPaged(pageable, memberId);
        PostPageDto response = new PostPageDto(pageResult);
        return ResponseEntity.ok(new ApiResponse<>(200, "내가 참여한 모든 투표 게시글 페이징 조회 성공", response));
    }
}
