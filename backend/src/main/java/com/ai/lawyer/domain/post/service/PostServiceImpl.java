package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
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
    private final MemberRepository memberRepository;

    public PostServiceImpl(PostRepository postRepository, MemberRepository memberRepository) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public PostDto createPost(PostDto postDto) {
        Member member = memberRepository.findById(postDto.getMemberId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        Post post = Post.builder()
            .postId(postDto.getPostId())
            .member(member)
            .postName(postDto.getPostName())
            .postContent(postDto.getPostContent())
            .category(postDto.getCategory())
            .createdAt(LocalDateTime.now())
            .build();
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
        return PostDetailDto.builder()
                .post(postDto)
                .poll(null)
                .build();
    }

    @Override
    public List<PostDto> getPostsByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        List<Post> posts = postRepository.findByMember(member);
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
                .collect(Collectors.toList());
    }

    private PostDto convertToDto(Post entity) {
        Long memberId = null;
        if (entity.getMember() != null) {
            memberId = entity.getMember().getMemberId();
        }
        return PostDto.builder()
                .postId(entity.getPostId())
                .memberId(memberId)
                .postName(entity.getPostName())
                .postContent(entity.getPostContent())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}