package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import com.ai.lawyer.domain.poll.service.PollService;
import com.ai.lawyer.domain.poll.dto.PollDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PollService pollService;

    private PostDto entityToDto(Post post) {
        return PostDto.builder()
                .postId(post.getPostId())
                .memberId(post.getMemberId())
                .postName(post.getPostName())
                .postContent(post.getPostContent())
                .category(post.getCategory())
                .createdAt(post.getCreatedAt())
                .build();
    }

    private Post dtoToEntity(PostDto dto) {
        return Post.builder()
                .postId(dto.getPostId())
                .memberId(dto.getMemberId())
                .postName(dto.getPostName())
                .postContent(dto.getPostContent())
                .category(dto.getCategory())
                .createdAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    @Override
    public PostDto createPost(PostDto postDto) {
        Post post = dtoToEntity(postDto);
        post.setCreatedAt(LocalDateTime.now());
        Post saved = postRepository.save(post);
        return entityToDto(saved);
    }

    @Override
    public PostDto getPostById(Long postId) {
        return postRepository.findById(postId)
                .map(this::entityToDto)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    @Override
    public PostDetailDto getPostDetailById(Long postId) {
        PostDto postDto = getPostById(postId);
        PollDto pollDto = pollService.getPollByPostId(postId);
        return PostDetailDto.builder()
                .post(postDto)
                .poll(pollDto)
                .build();
    }

    @Override
    public List<PostDto> getPostsByMemberId(Long memberId) {
        return postRepository.findByMemberId(memberId)
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PostDto updatePost(Long postId, PostDto postDto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setPostName(postDto.getPostName());
        post.setPostContent(postDto.getPostContent());
        post.setCategory(postDto.getCategory());
        Post updated = postRepository.save(post);
        return entityToDto(updated);
    }

    @Override
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
        // Poll과 연동해 추후 같이 삭제 요청
    }
}