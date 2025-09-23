package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "Post API", description = "게시글 관련 API")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> createPost(@RequestBody PostDto postDto) {
        PostDto created = postService.createPost(postDto);
        return ResponseEntity.ok(new ApiResponse<>(201, "게시글이 등록되었습니다.", created));
    }

    @Operation(summary = "게시글 전체 조회")
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<PostDto>>> getAllPosts() {
        List<PostDto> posts = postService.getAllPosts();
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글 전체 조회 성공", posts));
    }

    @Operation(summary = "게시글 단일 조회")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDto>> getPostById(@PathVariable Long postId) {
        PostDto postDto = postService.getPostById(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "게시글 단일 조회 성공", postDto));
    }

    @Operation(summary = "회원별 게시글 목록 조회")
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<PostDto>>> getPostsByMember(@PathVariable Long memberId) {
        List<PostDto> posts = postService.getPostsByMemberId(memberId);
        return ResponseEntity.ok(new ApiResponse<>(200, "회원별 게시글 목록 조회 성공", posts));
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDto>> updatePost(@PathVariable Long postId, @RequestBody PostDto postDto) {
        PostDto updated = postService.updatePost(postId, postDto);
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

    @Data
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T result;
    }
}