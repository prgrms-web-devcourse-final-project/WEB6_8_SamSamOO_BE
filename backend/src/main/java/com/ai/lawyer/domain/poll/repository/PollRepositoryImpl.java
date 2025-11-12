package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.poll.entity.QPoll;
import com.ai.lawyer.domain.poll.entity.QPollVote;
import com.ai.lawyer.domain.post.entity.Post;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PollRepositoryImpl implements PollRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QPoll poll = QPoll.poll;
    private final QPollVote pollVote = QPollVote.pollVote;

    @Override
    public Optional<Poll> findByPost(Post post) {
        return Optional.ofNullable(
            queryFactory.selectFrom(poll)
                .where(poll.getPost().eq(post))
                .fetchOne()
        );
    }

    @Override
    public List<Poll> findTopPollsByVoteCount(Pageable pageable) {
        return queryFactory.selectFrom(poll)
                .leftJoin(pollVote).on(poll.getPollId().eq(pollVote.getPoll().getPollId()))
                .groupBy(poll.getPollId())
                .orderBy(pollVote.count().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
