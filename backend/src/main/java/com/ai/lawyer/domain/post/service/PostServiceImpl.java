package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    public PostServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public PostDto createPost(PostDto postDto) {
        Post post = convertToEntity(postDto);
        post.setCreatedAt(LocalDateTime.now());
        Post saved = postRepository.save(post);
        return convertToDto(saved);
    }

    @Override
    public PostDto getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        return convertToDto(post);
    }

    @Override
    public PostDetailDto getPostDetailById(Long postId) {
        PostDto postDto = getPostById(postId);
        // PollDto pollDto = null; // poll 도메인 완성 후 추가
        return PostDetailDto.builder()
                .post(postDto)
                // .poll(pollDto)
                .build();
    }

    @Override
    public List<PostDto> getPostsByMemberId(Long memberId) {
        List<Post> posts = postRepository.findByMemberId(memberId);
        if (posts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 회원의 게시글이 없습니다.");
        }
        return posts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PostDto updatePost(Long postId, PostDto postDto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "수정할 게시글을 찾을 수 없습니다."));
        if (post.getVoteCount() > 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "투표가 진행된 게시글은 수정할 수 없습니다.");
        }

        post.setPostName(postDto.getPostName());
        post.setPostContent(postDto.getPostContent());
        post.setCategory(postDto.getCategory());
        Post updated = postRepository.save(post);
        return convertToDto(updated);
    }

    @Override
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "삭제할 게시글을 찾을 수 없습니다."));
        postRepository.delete(post);
    }

    @Override
    public List<PostDto> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .map(this::convertToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    // 엔티티/DTO 변환 메서드들
    private Post convertToEntity(PostDto dto) {
        return Post.builder()
                .postId(dto.getPostId())
                .memberId(dto.getMemberId())
                .postName(dto.getPostName())
                .postContent(dto.getPostContent())
                .category(dto.getCategory())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    private PostDto convertToDto(Post entity) {
        return PostDto.builder()
                .postId(entity.getPostId())
                .memberId(entity.getMemberId())
                .postName(entity.getPostName())
                .postContent(entity.getPostContent())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PostDetailDto convertToDetailDto(Post entity) {
        return PostDetailDto.builder()
                .post(convertToDto(entity))
                // .poll(null) // poll 도메인 완성 후 추가
                .build();
    }
}