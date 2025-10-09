package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.dto.*;
import com.ai.lawyer.domain.post.service.PostService;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.response.ApiResponse;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        Long memberId;
        if (principal instanceof org.springframework.security.core.userdetails.User user) {
            memberId = Long.valueOf(user.getUsername());
        } else if (principal instanceof Long) {
            memberId = (Long) principal;
        } else {
            throw new IllegalArgumentException("올바른 회원 ID가 아닙니다");
        }
        PostDto created = postService.createPost(postRequestDto, memberId);
        return ResponseEntity.ok(new ApiResponse<>(201, "게시글이 등록되었습니다.", created));
    }

    @PostMapping("/postdev")
    public ResponseEntity<ApiResponse<PostDto>> createPostDev(@RequestBody PostRequestDto postRequestDto, @RequestParam Long memberId) {
        PostDto created = postService.createPost(postRequestDto, memberId);
        return ResponseEntity.ok(new ApiResponse<>(201, "[DEV] 게시글이 등록되었습니다.", created));
    }

    @Operation(summary = "게시글 전체 조회")
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<PostDetailDto>>> getAllPosts() {
        List<PostDetailDto> posts = postService.getAllPosts();
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
        PostDetailDto postDto = postService.getPostById(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글 단일 조회 성공", postDto));
    }

    @Operation(summary = "회원별 게시글 목록 조회")
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<PostDetailDto>>> getPostsByMember(@PathVariable Long memberId) {
        List<PostDetailDto> posts = postService.getPostsByMemberId(memberId).stream()
            .map(postDto -> postService.getPostDetailById(postDto.getPostId()))
            .toList();
        return ResponseEntity.ok(new ApiResponse<>(200, "회원별 게시글 목록 조회 성공", posts));
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailDto>> updatePost(@PathVariable Long postId, @RequestBody PostUpdateDto postUpdateDto) {
        postService.updatePost(postId, postUpdateDto);
        PostDetailDto updated = postService.getPostDetailById(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글이 수정되었습니다.", updated));
    }

    @Operation(summary = "게시글 부분 수정(PATCH)")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailDto>> patchUpdatePost(@PathVariable Long postId, @RequestBody PostUpdateDto postUpdateDto) {
        postService.patchUpdatePost(postId, postUpdateDto);
        PostDetailDto updated = postService.getPostDetailById(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글이 수정되었습니다.", updated));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
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
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         Object principal = authentication.getPrincipal();
         Long memberId;
         if (principal instanceof org.springframework.security.core.userdetails.User user) {
             memberId = Long.valueOf(user.getUsername());
         } else if (principal instanceof Long) {
             memberId = (Long) principal;
         } else {
             throw new IllegalArgumentException("올바른 회원 ID가 아닙니다");
         }
         PostDto postDto = postService.getMyPostById(postId, memberId);
         return ResponseEntity.ok(new ApiResponse<>(200, "본인 게시글 단일 조회 성공", postDto));
     }

     @Operation(summary = "본인 게시글 전체 조회")
     @GetMapping("/my")
     public ResponseEntity<ApiResponse<List<PostDto>>> getMyPosts() {
         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         Object principal = authentication.getPrincipal();
         Long memberId;
         if (principal instanceof org.springframework.security.core.userdetails.User user) {
             memberId = Long.valueOf(user.getUsername());
         } else if (principal instanceof Long) {
             memberId = (Long) principal;
         } else {
             throw new IllegalArgumentException("올바른 회원 ID가 아닙니다");
         }
         List<PostDto> posts = postService.getMyPosts(memberId);
         return ResponseEntity.ok(new ApiResponse<>(200, "본인 게시글 전체 조회 성공", posts));
     }

    @Operation(summary = "게시글+투표 동시 등록")
    @PostMapping("/createPost")
    public ResponseEntity<ApiResponse<PostDetailDto>> createPostWithPoll(@RequestBody PostWithPollCreateDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        Long memberId;
        if (principal instanceof org.springframework.security.core.userdetails.User user) {
            memberId = Long.valueOf(user.getUsername());
        } else if (principal instanceof Long) {
            memberId = (Long) principal;
        } else {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않습니다.");
        }
        PostDetailDto result = postService.createPostWithPoll(dto, memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글+투표 등록 완료", result));
    }

    @Operation(summary = "게시글 페이징 조회")
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PostPageDto>> getPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostDto> posts = postService.getPostsPaged(pageable);
        if (posts == null) {
            posts = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
        }
        PostPageDto response = new PostPageDto(posts);
        return ResponseEntity.ok(new ApiResponse<>(200, "페이징 게시글 조회 성공", response));
    }

    @Operation(summary = "진행중 투표 게시글 페이징 조회")
    @GetMapping("/ongoingPaged")
    public ResponseEntity<ApiResponse<PostPageDto>> getOngoingPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostDto> posts = postService.getOngoingPostsPaged(pageable);
        if (posts == null) {
            posts = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
        }
        PostPageDto response = new PostPageDto(posts);
        return ResponseEntity.ok(new ApiResponse<>(200, "진행중 투표 게시글 페이징 조회 성공", response));
    }

    @Operation(summary = "마감 투표 게시글 페이징 조회")
    @GetMapping("/closedPaged")
    public ResponseEntity<ApiResponse<PostPageDto>> getClosedPostsPaged(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostDto> posts = postService.getClosedPostsPaged(pageable);
        if (posts == null) {
            posts = new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
        }
        PostPageDto response = new PostPageDto(posts);
        return ResponseEntity.ok(new ApiResponse<>(200, "마감된 투표 게시글 페이징 조회 성공", response));
    }
}