package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import com.ai.lawyer.domain.poll.repository.PollRepository;
import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.repository.PollOptionsRepository;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.repository.PollVoteRepository;
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
    private final PollRepository pollRepository;
    private final PollOptionsRepository pollOptionsRepository;
    private final PollVoteRepository pollVoteRepository;

    public PostServiceImpl(PostRepository postRepository, MemberRepository memberRepository, PollRepository pollRepository, PollOptionsRepository pollOptionsRepository, PollVoteRepository pollVoteRepository) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.pollRepository = pollRepository;
        this.pollOptionsRepository = pollOptionsRepository;
        this.pollVoteRepository = pollVoteRepository;
    }

    @Override
    public PostDto createPost(PostRequestDto postRequestDto, Member member) {
        Post post = Post.builder()
            .member(member)
            .postName(postRequestDto.getPostName())
            .postContent(postRequestDto.getPostContent())
            .category(postRequestDto.getCategory())
            .createdAt(LocalDateTime.now())
            .build();
        Post saved = postRepository.save(post);
        return convertToDto(saved);
    }

    @Override
    public PostDetailDto getPostById(Long postId) {
        return getPostDetailById(postId);
    }

    @Override
    public PostDetailDto getPostDetailById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        PostDto postDto = convertToDto(post);
        PollDto pollDto = null;
        pollDto = pollRepository.findByPost(post)
                .map(poll -> PollDto.builder()
                        .pollId(poll.getPollId())
                        .postId(post.getPostId())
                        .voteTitle(poll.getVoteTitle())
                        .status(PollDto.PollStatus.valueOf(poll.getStatus().name()))
                        .createdAt(poll.getCreatedAt())
                        .closedAt(poll.getClosedAt())
                        .build())
                .orElse(null);
        return PostDetailDto.builder()
                .post(postDto)
                .poll(pollDto)
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "수정할 게시글을 찾을 수 없습니다."));
        post.setPostName(postDto.getPostName());
        post.setPostContent(postDto.getPostContent());
        post.setCategory(postDto.getCategory());
        // === Poll 수정 로직 추가 ===
        if (postDto.getPoll() != null) {
            Poll poll = pollRepository.findByPost(post).orElse(null);
            if (poll != null) {
                long voteCount = pollVoteRepository.countByPollId(poll.getPollId());
                if (voteCount > 0) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이미 투표가 진행된 투표는 수정할 수 없습니다.");
                }
                // 항목 수정/삭제 처리
                if (postDto.getPoll().getPollOptions() != null) {
                    // 기존 옵션 불러오기
                    List<PollOptions> existingOptions = pollOptionsRepository.findAll().stream()
                        .filter(opt -> opt.getPoll().getPollId().equals(poll.getPollId()))
                        .toList();
                    // 전달받은 옵션 ID 목록
                    List<Long> newOptionIds = postDto.getPoll().getPollOptions().stream()
                        .map(optDto -> optDto.getPollOptionId())
                        .filter(id -> id != null)
                        .toList();
                    // 삭제: 기존 옵션 중 전달받은 목록에 없는 것 삭제
                    for (PollOptions option : existingOptions) {
                        if (!newOptionIds.contains(option.getPollItemsId())) {
                            pollOptionsRepository.delete(option);
                        }
                    }
                    // 수정: 전달받은 옵션 내용으로 기존 옵션 업데이트
                    for (var optDto : postDto.getPoll().getPollOptions()) {
                        if (optDto.getPollOptionId() != null) {
                            for (PollOptions option : existingOptions) {
                                if (option.getPollItemsId().equals(optDto.getPollOptionId())) {
                                    option.setOption(optDto.getContent());
                                    pollOptionsRepository.save(option);
                                }
                            }
                        }
                    }
                }
            }
        }
        Post updated = postRepository.save(post);
        return convertToDto(updated);
    }

    @Override
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 게시글을 찾을 수 없습니다."));
        // Poll도 명시적으로 삭제 (JPA cascade/orphanRemoval이 있으면 생략 가능)
        pollRepository.findByPost(post).ifPresent(pollRepository::delete);
        postRepository.delete(post);
    }

    @Override
    public List<PostDetailDto> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .map(post -> getPostDetailById(post.getPostId()))
                .collect(Collectors.toList());
    }

//    @Override
//    public PostDto getMyPostById(Long postId, Long requesterMemberId) {
//        Post post = postRepository.findById(postId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
//        if (!post.getMember().getMemberId().equals(requesterMemberId)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 게시글만 조회할 수 있습니다.");
//        }
//        return convertToDto(post);
//    }
//
//    @Override
//    public List<PostDto> getMyPosts(Long requesterMemberId) {
//        Member member = memberRepository.findById(requesterMemberId)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
//        List<Post> posts = postRepository.findByMember(member);
//        // 본인 게시글이 없으면 빈 리스트 반환
//        return posts.stream()
//                .map(this::convertToDto)
//                .collect(Collectors.toList());
//    }

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