package com.ai.lawyer.domain.post.service;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.post.dto.PostDto;
import com.ai.lawyer.domain.post.dto.PostDetailDto;
import com.ai.lawyer.domain.post.dto.PostRequestDto;
import com.ai.lawyer.domain.post.dto.PostUpdateDto;
import com.ai.lawyer.domain.post.dto.PostWithPollCreateDto;
import com.ai.lawyer.domain.post.dto.PostSimpleDto;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import com.ai.lawyer.domain.poll.repository.PollRepository;
import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.poll.dto.PollUpdateDto;
import com.ai.lawyer.domain.poll.repository.PollOptionsRepository;
import com.ai.lawyer.domain.poll.entity.PollOptions;
import com.ai.lawyer.domain.poll.repository.PollVoteRepository;
import com.ai.lawyer.domain.poll.service.PollService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PollRepository pollRepository;
    private final PollOptionsRepository pollOptionsRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollService pollService;

    public PostServiceImpl(PostRepository postRepository,
                           MemberRepository memberRepository,
                           PollRepository pollRepository,
                           PollOptionsRepository pollOptionsRepository,
                           PollVoteRepository pollVoteRepository,
                           PollService pollService) {
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.pollRepository = pollRepository;
        this.pollOptionsRepository = pollOptionsRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.pollService = pollService;
    }

    @Override
    public PostDto createPost(PostRequestDto postRequestDto, Long memberId) {
        if (postRequestDto.getPostName() == null || postRequestDto.getPostName().trim().isEmpty() ||
            postRequestDto.getPostContent() == null || postRequestDto.getPostContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시글 제목과 내용은 필수입니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        Post post = Post.builder()
            .member(member)
            .postName(postRequestDto.getPostName())
            .postContent(postRequestDto.getPostContent())
            .category(postRequestDto.getCategory())
            .createdAt(LocalDateTime.now())
            .build();
        Post saved = postRepository.save(post);
        return convertToDto(saved, memberId);
    }

    public PostDetailDto getPostDetailById(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        PostDto postDto = convertToDto(post, memberId);
        return PostDetailDto.builder()
            .post(postDto)
            .build();
    }

    @Override
    public PostDetailDto getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        PostDto postDto = convertToDto(post, post.getMember().getMemberId());
        return PostDetailDto.builder()
                .post(postDto)
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
                .map(post -> convertToDto(post, memberId))
                .collect(Collectors.toList());
    }

    @Override
    public PostDto updatePost(Long postId, PostUpdateDto postUpdateDto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "수정할 게시글을 찾을 수 없습니다."));

        // 연관 투표가 있을 경우, 투표수가 0이 아니면 수정 불가
        if (post.getPoll() != null) {
            Long pollId = post.getPoll().getPollId();
            Long voteCount = pollService.getVoteCountByPollId(pollId);
            if (voteCount > 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "투표가 이미 진행된 게시글은 수정할 수 없습니다.");
            }
        }

        if (postUpdateDto.getPoll() != null) {
            if (post.getPoll() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이 게시글에는 투표가 없어 투표 수정이 불가능합니다.");
            }
            pollService.updatePoll(post.getPoll().getPollId(), postUpdateDto.getPoll(), post.getMember().getMemberId());
        }

        if (postUpdateDto.getPostName() != null) post.setPostName(postUpdateDto.getPostName());
        if (postUpdateDto.getPostContent() != null) post.setPostContent(postUpdateDto.getPostContent());
        if (postUpdateDto.getCategory() != null) post.setCategory(postUpdateDto.getCategory());
        post.setCreatedAt(java.time.LocalDateTime.now()); // 수정 시 생성일 갱신

        Post updated = postRepository.save(post);
        return convertToDto(updated, post.getMember().getMemberId());
    }

    @Override
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 게시글을 찾을 수 없습니다."));
        // Poll도 명시적으로 삭제 (JPA cascade/orphanRemoval이 있으면 생략 가능)
        postRepository.delete(post);
    }

    @Override
    public List<PostDetailDto> getAllPosts(Long memberId) {
        return postRepository.findAll().stream()
            .map(post -> PostDetailDto.builder()
                .post(convertToDto(post, memberId))
                .build())
            .collect(Collectors.toList());
    }

    public PostDto getMyPostById(Long postId, Long requesterMemberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!post.getMember().getMemberId().equals(requesterMemberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 게시글만 조회할 수 있습니다.");
        }
        return convertToDto(post, requesterMemberId);
    }

    public List<PostDto> getMyPosts(Long requesterMemberId) {
        Member member = memberRepository.findById(requesterMemberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        List<Post> posts = postRepository.findByMember(member);
        // 본인 게시글이 없으면 빈 리스트 반환
        return posts.stream()
                .map(post -> convertToDto(post, requesterMemberId))
                .collect(Collectors.toList());
    }

    @Override
    public void patchUpdatePost(Long postId, PostUpdateDto postUpdateDto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "수정할 게시글을 찾을 수 없습니다."));

        // 연관 투표가 있을 경우, 투표수가 0이 아니면 수정 불가
        if (post.getPoll() != null) {
            Long pollId = post.getPoll().getPollId();
            Long voteCount = pollService.getVoteCountByPollId(pollId);
            if (voteCount > 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "투표가 이미 진행된 게시글은 수정할 수 없습니다.");
            }
        }

        if (postUpdateDto.getPostName() != null) post.setPostName(postUpdateDto.getPostName());
        if (postUpdateDto.getPostContent() != null) post.setPostContent(postUpdateDto.getPostContent());
        if (postUpdateDto.getCategory() != null) post.setCategory(postUpdateDto.getCategory());
        post.setCreatedAt(java.time.LocalDateTime.now()); // 수정 시 생성일 갱신

        // 투표 수정이 요청된 경우
        if (postUpdateDto.getPoll() != null && post.getPoll() != null) {
            PollUpdateDto pollUpdateDto = postUpdateDto.getPoll();
            pollService.patchUpdatePoll(post.getPoll().getPollId(), pollUpdateDto);
        }
        postRepository.save(post);
    }

    @Override
    @Transactional
    public PostDetailDto createPostWithPoll(PostWithPollCreateDto dto, Long memberId) {
        PostRequestDto postDto = dto.getPost();
        if (postDto == null || postDto.getPostName() == null || postDto.getPostName().trim().isEmpty() ||
            postDto.getPostContent() == null || postDto.getPostContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시글 제목과 내용은 필수입니다.");
        }
        var pollDto = dto.getPoll();
        pollService.validatePollCreate(pollDto);
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        Post post = Post.builder()
            .member(member)
            .postName(postDto.getPostName())
            .postContent(postDto.getPostContent())
            .category(postDto.getCategory())
            .createdAt(LocalDateTime.now())
            .build();
        Post savedPost = postRepository.save(post);
        Poll poll = Poll.builder()
            .voteTitle(pollDto.getVoteTitle())
            .reservedCloseAt(pollDto.getReservedCloseAt())
            .createdAt(LocalDateTime.now())
            .status(Poll.PollStatus.ONGOING)
            .post(savedPost)
            .build();
        Poll savedPoll = pollRepository.save(poll);
        for (var optionDto : pollDto.getPollOptions()) {
            PollOptions option = PollOptions.builder()
                .poll(savedPoll)
                .option(optionDto.getContent())
                .build();
            pollOptionsRepository.save(option);
        }
        savedPost.setPoll(savedPoll);
        postRepository.save(savedPost);
        return getPostDetailById(savedPost.getPostId(), memberId);
    }

    @Override
    public List<PostSimpleDto> getAllSimplePosts() {
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .map(post -> {
                    PostSimpleDto.PollInfo pollInfo = null;
                    if (post.getPoll() != null) {
                        pollInfo = PostSimpleDto.PollInfo.builder()
                                .pollId(post.getPoll().getPollId())
                                .pollStatus(post.getPoll().getStatus().name())
                                .build();
                    }
                    return PostSimpleDto.builder()
                            .postId(post.getPostId())
                            .memberId(post.getMember().getMemberId())
                            .poll(pollInfo)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<PostDto> getPostsPaged(Pageable pageable, Long memberId) {
        return postRepository.findAll(pageable).map(post -> convertToDto(post, memberId));
    }

    @Override
    public Page<PostDto> getOngoingPostsPaged(Pageable pageable, Long memberId) {
        Page<PostDto> allPosts = postRepository.findAll(pageable).map(post -> convertToDto(post, memberId));
        List<PostDto> ongoing = allPosts.stream()
            .filter(dto -> dto.getPoll() != null && dto.getPoll().getStatus() == PollDto.PollStatus.ONGOING)
            .collect(Collectors.toList());
        return new PageImpl<>(ongoing, pageable, ongoing.size());
    }

    @Override
    public Page<PostDto> getClosedPostsPaged(Pageable pageable, Long memberId) {
        Page<PostDto> allPosts = postRepository.findAll(pageable).map(post -> convertToDto(post, memberId));
        List<PostDto> closed = allPosts.stream()
            .filter(dto -> dto.getPoll() != null && dto.getPoll().getStatus() == PollDto.PollStatus.CLOSED)
            .collect(Collectors.toList());
        return new PageImpl<>(closed, pageable, closed.size());
    }

    @Override
    public List<PostDto> getTopNPollsByStatus(PollDto.PollStatus status, int n, Long memberId) {
        return postRepository.findAll().stream()
            .map(post -> convertToDto(post, memberId))
            .filter(dto -> dto.getPoll() != null && dto.getPoll().getStatus() == status)
            .sorted(Comparator.comparing((PostDto dto) -> dto.getPoll().getTotalVoteCount() == null ? 0 : dto.getPoll().getTotalVoteCount()).reversed())
            .limit(n)
            .collect(Collectors.toList());
    }

    @Override
    public PostDto getTopPollByStatus(PollDto.PollStatus status, Long memberId) {
        return postRepository.findAll().stream()
            .map(post -> convertToDto(post, memberId))
            .filter(dto -> dto.getPoll() != null && dto.getPoll().getStatus() == status)
            .max(Comparator.comparing((PostDto dto) -> dto.getPoll().getTotalVoteCount() == null ? 0 : dto.getPoll().getTotalVoteCount()))
            .orElse(null);
    }

    private PostDto convertToDto(Post entity, Long memberId) {
        Long postMemberId = null;
        if (entity.getMember() != null) {
            postMemberId = entity.getMember().getMemberId();
        }
        PollDto pollDto = null;
        if (entity.getPoll() != null) {
            if (entity.getPoll().getStatus() == Poll.PollStatus.CLOSED) {
                pollDto = pollService.getPollWithStatistics(entity.getPoll().getPollId(), memberId);
            } else {
                pollDto = pollService.getPoll(entity.getPoll().getPollId(), memberId);
            }
        }
        return PostDto.builder()
                .postId(entity.getPostId())
                .memberId(postMemberId)
                .postName(entity.getPostName())
                .postContent(entity.getPostContent())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .poll(pollDto)
                .build();
    }
}
