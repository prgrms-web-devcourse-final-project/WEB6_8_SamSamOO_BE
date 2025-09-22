package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody PostDto postDto) {
        PostDto created = postService.createPost(postDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailDto> getPostDetail(@PathVariable Long postId) {
        PostDetailDto postDetail = postService.getPostDetailById(postId);
        return ResponseEntity.ok(postDetail);
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<PostDto>> getPostsByMember(@PathVariable Long memberId) {
        List<PostDto> posts = postService.getPostsByMemberId(memberId);
        return ResponseEntity.ok(posts);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostDto> updatePost(@PathVariable Long postId, @RequestBody PostDto postDto) {
        PostDto updated = postService.updatePost(postId, postDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}