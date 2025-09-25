package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.service.PostService;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Post API", description = "게시글 관련 API")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final MemberRepository memberRepository;

    @Autowired
    public PostController(PostService postService, MemberRepository memberRepository) {
        this.postService = postService;
        this.memberRepository = memberRepository;
    }

    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> createPost(@RequestBody PostRequestDto postRequestDto, @RequestParam Long memberId) {
        // public ResponseEntity<ApiResponse<PostDto>> createPost(@RequestBody PostRequestDto postRequestDto, HttpServletRequest request){
        // Long memberId = tokenProvider.getMemberIdFromToken(accessToken);
        // Member member = memberRepository.findById(memberId)
        //         .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        // PostDto created = postService.createPost(postRequestDto, member);
        // return ResponseEntity.ok(new ApiResponse<>(201, "게시글이 등록되었습니다.", created));
        //
        // 현재는 memberId를 직접 입력
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        PostDto created = postService.createPost(postRequestDto, member);
        return ResponseEntity.ok(new ApiResponse<>(201, "게시글이 등록되었습니다.", created));
    }

    @Operation(summary = "게시글 전체 조회")
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<PostDetailDto>>> getAllPosts() {
        List<PostDetailDto> posts = postService.getAllPosts();
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글 전체 조회 성공", posts));
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
    public ResponseEntity<ApiResponse<PostDetailDto>> updatePost(@PathVariable Long postId, @RequestBody PostDto postDto) {
        PostDetailDto updated = postService.getPostDetailById(postId);
        postService.updatePost(postId, postDto);
        updated = postService.getPostDetailById(postId);
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

    // @Operation(summary = "본인 게시글 단일 조회")
    // @GetMapping("/my/{postId}")
    // public ResponseEntity<ApiResponse<PostDto>> getMyPostById(@PathVariable Long postId, HttpServletRequest request) {
    //     String accessToken = cookieUtil.getAccessTokenFromCookies(request);
    //     Long memberId = tokenProvider.getMemberIdFromToken(accessToken);
    //     PostDto postDto = postService.getMyPostById(postId, memberId);
    //     return ResponseEntity.ok(new ApiResponse<>(200, "본인 게시글 단일 조회 성공", postDto));
    // }
    //
    // @Operation(summary = "본인 게시글 전체 조회")
    // @GetMapping("/my")
    // public ResponseEntity<ApiResponse<List<PostDto>>> getMyPosts(HttpServletRequest request) {
    //     String accessToken = cookieUtil.getAccessTokenFromCookies(request);
    //     Long memberId = tokenProvider.getMemberIdFromToken(accessToken);
    //     List<PostDto> posts = postService.getMyPosts(memberId);
    //     return ResponseEntity.ok(new ApiResponse<>(200, "본인 게시글 전체 조회 성공", posts));
    // }

    @Data
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T result;
    }
}