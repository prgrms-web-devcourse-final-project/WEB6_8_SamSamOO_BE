package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long>, PollVoteRepositoryCustom {
    Optional<PollVote> findByMember_MemberIdAndPoll_PollId(Long memberId, Long pollId);
    void deleteByMember_MemberIdAndPoll_PollId(Long memberId, Long pollId);
    Optional<PollVote> findByMember_MemberIdAndPollOptions_PollItemsId(Long memberId, Long pollItemsId);
}
