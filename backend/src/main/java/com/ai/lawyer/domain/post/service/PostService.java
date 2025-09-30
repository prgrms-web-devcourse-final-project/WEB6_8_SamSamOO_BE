package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.dto.PostUpdateDto;
import com.ai.lawyer.domain.post.dto.PostWithPollCreateDto;

import java.util.List;

public interface PostService {

    PostDto createPost(PostRequestDto postRequestDto, Long memberId);

    PostDetailDto getPostById(Long postId);

    PostDetailDto getPostDetailById(Long postId);

    List<PostDto> getPostsByMemberId(Long memberId);

    PostDto updatePost(Long postId, PostUpdateDto postUpdateDto);

    void deletePost(Long postId);

    List<PostDetailDto> getAllPosts();

    PostDto getMyPostById(Long postId, Long requesterMemberId);

    List<PostDto> getMyPosts(Long requesterMemberId);

    void patchUpdatePost(Long postId, PostUpdateDto postUpdateDto);

    PostDetailDto createPostWithPoll(PostWithPollCreateDto dto, Long memberId);
}