package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.dto.PostUpdateDto;
import com.ai.lawyer.domain.post.dto.PostWithPollCreateDto;
import com.ai.lawyer.domain.post.dto.PostSimpleDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {
    // ===== 조회 관련 =====
    PostDetailDto getPostById(Long postId);
    PostDetailDto getPostDetailById(Long postId);
    List<PostDetailDto> getAllPosts();
    List<PostSimpleDto> getAllSimplePosts();
    List<PostDto> getPostsByMemberId(Long memberId);

    // ===== 생성/수정/삭제 관련 =====
    PostDto createPost(PostRequestDto postRequestDto, Long memberId);
    PostDto updatePost(Long postId, PostUpdateDto postUpdateDto);
    void patchUpdatePost(Long postId, PostUpdateDto postUpdateDto);
    void deletePost(Long postId);
    PostDetailDto createPostWithPoll(PostWithPollCreateDto dto, Long memberId);

    // ===== 본인 게시글 관련 =====
    PostDto getMyPostById(Long postId, Long requesterMemberId);
    List<PostDto> getMyPosts(Long requesterMemberId);

    // ===== 페이징 관련 =====
    Page<PostDto> getPostsPaged(Pageable pageable);
    Page<PostDto> getOngoingPostsPaged(Pageable pageable);
    Page<PostDto> getClosedPostsPaged(Pageable pageable);
}