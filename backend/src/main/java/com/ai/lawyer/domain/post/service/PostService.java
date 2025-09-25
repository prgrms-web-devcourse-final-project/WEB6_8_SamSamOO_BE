package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;

import java.util.List;

public interface PostService {

    PostDto createPost(PostRequestDto postRequestDto, Member member);

    PostDetailDto getPostById(Long postId);

    PostDetailDto getPostDetailById(Long postId);

    List<PostDto> getPostsByMemberId(Long memberId);

    PostDto updatePost(Long postId, PostDto postDto);

    void deletePost(Long postId);

    List<PostDetailDto> getAllPosts();
}