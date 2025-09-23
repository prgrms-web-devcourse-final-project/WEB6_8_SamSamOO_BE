package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;

import java.util.List;

public interface PostService {

    PostDto createPost(PostDto postDto);

    PostDto getPostById(Long postId);

    PostDetailDto getPostDetailById(Long postId);

    List<PostDto> getPostsByMemberId(Long memberId);

    PostDto updatePost(Long postId, PostDto postDto);

    void deletePost(Long postId);

    List<PostDto> getAllPosts();
}