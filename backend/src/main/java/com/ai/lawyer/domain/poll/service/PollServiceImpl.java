package com.ai.lawyer.domain.poll.service;

import com.ai.lawyer.domain.poll.entity.*;
import com.ai.lawyer.domain.poll.repository.*;
import com.ai.lawyer.domain.poll.service.*;
import com.ai.lawyer.domain.poll.dto.PollDto;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import com.ai.lawyer.domain.poll.dto.PollCreateDto;
import com.ai.lawyer.domain.poll.dto.PollOptionCreateDto;
import com.ai.lawyer.domain.poll.dto.PollStaticsDto;
import com.ai.lawyer.domain.poll.dto.PollOptionDto;
import com.ai.lawyer.domain.poll.dto.PollVoteDto;
import com.ai.lawyer.domain.poll.dto.PollUpdateDto;
import com.ai.lawyer.domain.poll.entity.Poll;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;
    private final PollOptionsRepository pollOptionsRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollStaticsRepository pollStaticsRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @Override
    public PollDto createPoll(PollCreateDto request, Long memberId) {
        if (request.getPostId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시글 ID(postId)는 필수입니다.");
        }
        if (request.getVoteTitle() == null || request.getVoteTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "투표 제목(voteTitle)은 필수입니다.");
        }
        if (request.getPollOptions() == null || request.getPollOptions().size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "투표 항목은 2개여야 합니다.");
        }
        for (PollOptionCreateDto option : request.getPollOptions()) {
            if (option.getContent() == null || option.getContent().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "각 투표 항목의 내용(content)은 필수입니다.");
            }
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        try {
            LocalDateTime now = java.time.LocalDateTime.now();
            LocalDateTime reservedCloseAt = request.getReservedCloseAt();
            if (reservedCloseAt != null) {
                if (reservedCloseAt.isBefore(now.plusHours(1))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약 종료 시간은 현재로부터 최소 1시간 이후여야 합니다.");
                }
                if (reservedCloseAt.isAfter(now.plusDays(7))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약 종료 시간은 최대 7일 이내여야 합니다.");
                }
            }
            Poll poll = Poll.builder()
                    .post(post)
                    .voteTitle(request.getVoteTitle())
                    .status(Poll.PollStatus.ONGOING)
                    .createdAt(now)
                    .reservedCloseAt(reservedCloseAt)
                    .build();
            Poll savedPoll = pollRepository.save(poll);
            post.setPoll(savedPoll);
            postRepository.save(post);

            for (PollOptionCreateDto optionDto : request.getPollOptions()) {
                PollOptions option = PollOptions.builder()
                        .poll(savedPoll)
                        .option(optionDto.getContent())
                        .build();
                pollOptionsRepository.save(option);
            }
            return convertToDto(savedPoll);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "투표 생성 중 오류가 발생했습니다.");
        }
    }

    @Override
    public PollDto getPoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
        autoCloseIfNeeded(poll);
        if (poll.getStatus() == Poll.PollStatus.CLOSED) {
            return getPollWithStatistics(pollId);
        }
        return convertToDto(poll);
    }

    @Override
    public List<PollDto> getPollsByStatus(PollDto.PollStatus status) {
        List<Poll> polls = pollRepository.findAll();
        for (Poll poll : polls) {
            autoCloseIfNeeded(poll);
        }
        List<PollDto> pollDtos = polls.stream()
            .filter(p -> p.getStatus().name().equals(status.name()))
            .map(this::convertToDto)
            .toList();
        return pollDtos;
    }

    @Override
    public PollVoteDto vote(Long pollId, Long pollItemsId, Long memberId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
        if (poll.getStatus() == Poll.PollStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 투표에는 참여할 수 없습니다.");
        }
        PollOptions pollOptions = pollOptionsRepository.findById(pollItemsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표 항목을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        // USER 또는 ADMIN만 투표 가능
        if (!(member.getRole().name().equals("USER") || member.getRole().name().equals("ADMIN"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "투표 권한이 없습니다.");
        }
        // 중복 투표 방지
        /*
        if (pollVoteRepository.existsByPoll_PollIdAndMember_MemberId(pollId, memberId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 이 투표에 참여하셨습니다.");
        }
        */
        PollVote pollVote = PollVote.builder()
                .poll(poll)
                .pollOptions(pollOptions)
                .member(member)
                .build();
        PollVote savedVote = pollVoteRepository.save(pollVote);
        // 해당 옵션의 투표 수 계산
        Long voteCount = pollVoteRepository.countByPollOptionId(pollItemsId);
        return PollVoteDto.builder()
                .pollVoteId(savedVote.getPollVoteId())
                .pollId(pollId)
                .pollItemsId(pollItemsId)
                .memberId(memberId)
                .voteCount(voteCount)
                .build();
    }

    @Override
    public List<PollStatics> getPollStatics(Long pollId) {
        // 투표 존재 여부 체크
        if (!pollRepository.existsById(pollId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 투표가 존재하지 않습니다.");
        }
        return pollStaticsRepository.findByPoll_PollId(pollId);
    }

    // 최대 7일 동안 투표 가능 (초기 요구사항)
    @Override
    public void closePoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
        poll.setStatus(Poll.PollStatus.CLOSED);
        poll.setClosedAt(java.time.LocalDateTime.now());
        pollRepository.save(poll);
    }

    @Override
    public void deletePoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));

        // 1. 이 Poll을 참조하는 Post가 있으면 연결 해제
        Post post = postRepository.findAll().stream()
                .filter(p -> p.getPoll() != null && p.getPoll().getPollId().equals(pollId))
                .findFirst()
                .orElse(null);
        if (post != null) {
            post.setPoll(null);
            postRepository.save(post);
        }

        // 2. Poll 삭제
        pollRepository.deleteById(pollId);
    }

    @Override
    public PollDto getTopPollByStatus(PollDto.PollStatus status) {
        List<Object[]> result = pollVoteRepository.findTopPollByStatus(Poll.PollStatus.valueOf(status.name()));
        if (result.isEmpty()) {
            // 종료된 투표가 없으면 빈 PollDto 반환
            return PollDto.builder()
                .pollId(null)
                .postId(null)
                .voteTitle(null)
                .status(status)
                .createdAt(null)
                .closedAt(null)
                .pollOptions(java.util.Collections.emptyList())
                .totalVoteCount(0L)
                .build();
        }
        Long pollId = (Long) result.get(0)[0];
        return getPoll(pollId);
    }

    @Override
    public List<PollDto> getTopNPollsByStatus(PollDto.PollStatus status, int n) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, n);
        List<Object[]> result = pollVoteRepository.findTopNPollByStatus(
            com.ai.lawyer.domain.poll.entity.Poll.PollStatus.valueOf(status.name()), pageable);
        List<PollDto> pollDtos = new java.util.ArrayList<>();
        for (Object[] row : result) {
            Long pollId = (Long) row[0];
            pollDtos.add(getPoll(pollId));
        }
        return pollDtos;
    }

    @Override
    public Long getVoteCountByPollId(Long pollId) {
        return pollVoteRepository.countByPollId(pollId);
    }

    @Override
    public Long getVoteCountByPostId(Long postId) {
        Poll poll = pollRepository.findAll().stream()
                .filter(p -> p.getPost() != null && p.getPost().getPostId().equals(postId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 게시글의 투표가 없습니다."));
        return getVoteCountByPollId(poll.getPollId());
    }


    @Override
    public PollDto updatePoll(Long pollId, PollUpdateDto pollUpdateDto) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "수정할 투표를 찾을 수 없습니다."));
        if (getVoteCountByPollId(pollId) > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "투표가 진행된 투표는 수정할 수 없습니다.");
        }
        if (pollUpdateDto.getVoteTitle() != null) poll.setVoteTitle(pollUpdateDto.getVoteTitle());
        // 투표 항목 수정
        if (pollUpdateDto.getPollOptions() != null) {
            List<PollOptions> existingOptions = pollOptionsRepository.findByPoll_PollId(pollId);
            // 전달받은 id 목록
            List<Long> incomingIds = pollUpdateDto.getPollOptions().stream()
                    .map(opt -> opt.getPollItemsId())
                    .filter(id -> id != null)
                    .toList();
            // 기존 옵션 중 전달받지 않은 id 삭제
            for (PollOptions option : existingOptions) {
                if (!incomingIds.contains(option.getPollItemsId())) {
                    pollOptionsRepository.deleteById(option.getPollItemsId());
                }
            }
            //추가/수정
            for (var optionDto : pollUpdateDto.getPollOptions()) {
                if (optionDto.getPollItemsId() != null) {
                    // update
                    PollOptions option = existingOptions.stream()
                        .filter(o -> o.getPollItemsId().equals(optionDto.getPollItemsId()))
                        .findFirst().orElse(null);
                    if (option != null) {
                        option.setOption(optionDto.getContent());
                        pollOptionsRepository.save(option);
                    }
                } else {
                    PollOptions newOption = PollOptions.builder()
                        .poll(poll)
                        .option(optionDto.getContent())
                        .build();
                    pollOptionsRepository.save(newOption);
                }
            }
        }
        // 예약 종료 시간 수정
        LocalDateTime now = java.time.LocalDateTime.now();
        LocalDateTime reservedCloseAt = pollUpdateDto.getReservedCloseAt();
        if (reservedCloseAt != null) {
            if (reservedCloseAt.isBefore(now.plusHours(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약 종료 시간은 현재로부터 최소 1시간 이후여야 합니다.");
            }
            if (reservedCloseAt.isAfter(poll.getCreatedAt().plusDays(7))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약 종료 시간은 최대 7일 이내여야 합니다.");
            }
            poll.setReservedCloseAt(reservedCloseAt);
        }
        Poll updated = pollRepository.save(poll);
        return convertToDto(updated);
    }

    @Override
    public void patchUpdatePoll(Long pollId, PollUpdateDto pollUpdateDto) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "수정할 투표를 찾을 수 없습니다."));
        if (getVoteCountByPollId(pollId) > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "투표가 이미 진행된 투표는 수정할 수 없습니다.");
        }
        if (pollUpdateDto.getVoteTitle() != null) poll.setVoteTitle(pollUpdateDto.getVoteTitle());
        // 투표 항목 수정
        if (pollUpdateDto.getPollOptions() != null && pollUpdateDto.getPollOptions().size() == 2) {
            pollOptionsRepository.deleteAll(pollOptionsRepository.findByPoll_PollId(pollId));
            pollUpdateDto.getPollOptions().forEach(optionDto -> {
                PollOptions option = PollOptions.builder()
                        .poll(poll)
                        .option(optionDto.getContent())
                        .build();
                pollOptionsRepository.save(option);
            });
        }
        pollRepository.save(poll);
    }

    @Override
    public PollDto getPollWithStatistics(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
        List<PollOptions> options = pollOptionsRepository.findByPoll_PollId(poll.getPollId());
        List<Long> optionIds = options.stream().map(PollOptions::getPollItemsId).toList();
        Long totalVoteCount = pollVoteRepository.countByPollId(poll.getPollId());
        List<PollOptionDto> optionDtos;
        if (poll.getStatus() == Poll.PollStatus.CLOSED && !optionIds.isEmpty()) {
            List<Object[]> staticsRaw = pollVoteRepository.countStaticsByPollOptionIds(optionIds);
            optionDtos = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                PollOptions option = options.get(i);
                Long voteCount = pollVoteRepository.countByPollOptionId(option.getPollItemsId());
                List<PollStaticsDto> statics = staticsRaw.stream()
                    .filter(arr -> ((Long)arr[0]).equals(option.getPollItemsId()))
                    .map(arr -> {
                        String gender = arr[1] != null ? arr[1].toString() : null;
                        Integer age = arr[2] != null ? ((Number)arr[2]).intValue() : null;
                        String ageGroup = getAgeGroup(age);
                        return PollStaticsDto.builder()
                            .gender(gender)
                            .ageGroup(ageGroup)
                            .voteCount((Long)arr[3])
                            .build();
                    })
                    .toList();
                optionDtos.add(PollOptionDto.builder()
                    .pollItemsId(option.getPollItemsId())
                    .content(option.getOption())
                    .voteCount(voteCount)
                    .statics(statics)
                    .pollOptionIndex(i + 1)
                    .build());
            }
        } else {
            optionDtos = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                PollOptions option = options.get(i);
                Long voteCount = pollVoteRepository.countByPollOptionId(option.getPollItemsId());
                optionDtos.add(PollOptionDto.builder()
                    .pollItemsId(option.getPollItemsId())
                    .content(option.getOption())
                    .voteCount(voteCount)
                    .statics(null)
                    .pollOptionIndex(i + 1)
                    .build());
            }
        }
        return PollDto.builder()
            .pollId(poll.getPollId())
            .postId(poll.getPost() != null ? poll.getPost().getPostId() : null)
            .voteTitle(poll.getVoteTitle())
            .status(PollDto.PollStatus.valueOf(poll.getStatus().name()))
            .createdAt(poll.getCreatedAt())
            .closedAt(poll.getClosedAt())
            .pollOptions(optionDtos)
            .totalVoteCount(totalVoteCount)
            .build();
    }

    private PollDto convertToDto(Poll poll) {
        List<PollOptions> options = pollOptionsRepository.findByPoll_PollId(poll.getPollId());
        List<PollOptionDto> optionDtos = new ArrayList<>();
        Long totalVoteCount = pollVoteRepository.countByPollId(poll.getPollId());
        for (int i = 0; i < options.size(); i++) {
            PollOptions option = options.get(i);
            Long voteCount = pollVoteRepository.countByPollOptionId(option.getPollItemsId());
            optionDtos.add(PollOptionDto.builder()
                .pollItemsId(option.getPollItemsId())
                .content(option.getOption())
                .voteCount(voteCount)
                .statics(null)
                .pollOptionIndex(i + 1)
                .build());
        }
        LocalDateTime expectedCloseAt = poll.getReservedCloseAt() != null ? poll.getReservedCloseAt() : poll.getCreatedAt().plusDays(7);
        return PollDto.builder()
                .pollId(poll.getPollId())
                .postId(poll.getPost() != null ? poll.getPost().getPostId() : null)
                .voteTitle(poll.getVoteTitle())
                .status(PollDto.PollStatus.valueOf(poll.getStatus().name()))
                .createdAt(poll.getCreatedAt())
                .closedAt(poll.getClosedAt())
                .expectedCloseAt(expectedCloseAt)
                .pollOptions(optionDtos)
                .totalVoteCount(totalVoteCount)
                .build();
    }

    private String getAgeGroup(Integer age) {
        if (age == null) return "기타";
        if (age < 20) return "10대";
        if (age < 30) return "20대";
        if (age < 40) return "30대";
        if (age < 50) return "40대";
        if (age < 60) return "50대";
        if (age < 70) return "60대";
        if (age < 80) return "70대";
        return "80대 이상";
    }

    // 자동 종료 로직 보강
    private void autoCloseIfNeeded(Poll poll) {
        LocalDateTime now = java.time.LocalDateTime.now();
        if (poll.getStatus() == Poll.PollStatus.ONGOING) {
            if (poll.getReservedCloseAt() != null && poll.getReservedCloseAt().isBefore(now)) {
                poll.setStatus(Poll.PollStatus.CLOSED);
                poll.setClosedAt(now);
                pollRepository.save(poll);
            } else if (poll.getCreatedAt() != null && poll.getCreatedAt().plusDays(7).isBefore(now)) {
                poll.setStatus(Poll.PollStatus.CLOSED);
                poll.setClosedAt(now);
                pollRepository.save(poll);
            }
        }
    }

    @Override
    public List<PollOptions> getPollOptions(Long pollId) {
        return pollOptionsRepository.findByPoll_PollId(pollId);
    }
}
