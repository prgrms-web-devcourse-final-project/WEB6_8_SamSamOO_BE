package com.ai.lawyer.domain.post.controller;

import com.ai.lawyer.domain.post.service.PostDummyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@Tag(name = "더미생성")
@RestController
@RequestMapping("/api/dummy")
public class PostDummyController {
    private final PostDummyService dummyService;

    @Autowired
    public PostDummyController(PostDummyService dummyService) {
        this.dummyService = dummyService;
    }

    @Operation(summary = "더미 멤버 추가")
    @PostMapping("/members")
    public ResponseEntity<String> createDummyMembers(@RequestParam(defaultValue = "100") int count) {
        int created = dummyService.createDummyMembers(count);
        return ResponseEntity.ok("더미 멤버 " + created + "명 생성 완료");
    }

    @Operation(summary = "더미 멤버 투표")
    @PostMapping("/vote")
    public ResponseEntity<String> dummyVote(@RequestParam Long postId) {
        int voteCount = dummyService.dummyVote(postId);
        return ResponseEntity.ok("더미 멤버 " + voteCount + "명 투표 완료");
    }

    @Operation(summary = "더미 멤버 삭제")
    @DeleteMapping("/members")
    public ResponseEntity<String> deleteDummyMembers() {
        int deleted = dummyService.deleteDummyMembers();
        return ResponseEntity.ok("더미 멤버 " + deleted + "명 삭제 완료");
    }

    //모든 더미 유저가 1번 옵션에 투표
    @Operation(summary = "더미 멤버 1번 투표")
    @PostMapping("/vote1")
    public ResponseEntity<String> dummyVote1Option(@RequestParam Long postId) {
        int voteCount = dummyService.dummyVote1Option(postId);
        return ResponseEntity.ok("더미 멤버 " + voteCount + "명 투표 완료");
    }

    //모든 더미 유저가 2번 옵션에 투표
    @Operation(summary = "더미 멤버 2번 투표")
    @PostMapping("/vote2")
    public ResponseEntity<String> dummyVote2Option(@RequestParam Long postId) {
        int voteCount = dummyService.dummyVote2Option(postId);
        return ResponseEntity.ok("더미 멤버 " + voteCount + "명 투표 완료");
    }
}