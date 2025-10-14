package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.poll.entity.Poll;
import com.ai.lawyer.domain.poll.entity.QPoll;
import com.ai.lawyer.domain.poll.entity.QPollOptions;
import com.ai.lawyer.domain.poll.entity.QPollVote;
import com.ai.lawyer.domain.member.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.querydsl.core.Tuple;

import java.util.List;

import com.ai.lawyer.domain.poll.dto.PollAgeStaticsDto.AgeGroupCountDto;
import com.ai.lawyer.domain.poll.dto.PollGenderStaticsDto.GenderCountDto;
import com.ai.lawyer.domain.poll.dto.PollTopDto;
import com.ai.lawyer.domain.poll.dto.PollStaticsDto;

@Repository
@RequiredArgsConstructor
public class PollVoteRepositoryImpl implements PollVoteRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QPollVote pollVote = QPollVote.pollVote;
    private final QPoll poll = QPoll.poll;
    private final QPollOptions pollOptions = QPollOptions.pollOptions;
    private final QMember member = QMember.member;

    @Override
    public List<PollTopDto> findTopPollByStatus(Poll.PollStatus status) {
        List<Tuple> tuples = queryFactory.select(poll.getPollId(), pollVote.count())
                .from(pollVote)
                .join(pollVote.getPoll(), poll)
                .where(poll.getStatus().eq(status))
                .groupBy(poll.getPollId())
                .orderBy(pollVote.count().desc())
                .fetch();
        return tuples.stream()
                .map(t -> new PollTopDto(
                        t.get(0, Long.class),
                        t.get(1, Long.class)
                ))
                .toList();
    }

    @Override
    public List<PollTopDto> findTopNPollByStatus(Poll.PollStatus status, Pageable pageable) {
        List<Tuple> tuples = queryFactory.select(poll.getPollId(), pollVote.count())
                .from(pollVote)
                .join(pollVote.getPoll(), poll)
                .where(poll.getStatus().eq(status))
                .groupBy(poll.getPollId())
                .orderBy(pollVote.count().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return tuples.stream()
                .map(t -> new PollTopDto(
                        t.get(0, Long.class),
                        t.get(1, Long.class)
                ))
                .toList();
    }

    @Override
    public Long countByPollId(Long pollId) {
        return queryFactory.select(pollVote.count())
                .from(pollVote)
                .join(pollVote.getPoll(), poll)
                .where(poll.getPollId().eq(pollId))
                .fetchOne();
    }

    @Override
    public Long countByPollOptionId(Long pollOptionId) {
        return queryFactory.select(pollVote.count())
                .from(pollVote)
                .join(pollVote.getPollOptions(), pollOptions)
                .where(pollOptions.getPollItemsId().eq(pollOptionId))
                .fetchOne();
    }

    @Override
    public List<PollStaticsDto> countStaticsByPollOptionIds(List<Long> pollOptionIds) {
        List<Tuple> tuples = queryFactory.select(
                pollOptions.getPollItemsId(),
                member.getGender(),
                member.getAge(),
                pollVote.count())
                .from(pollVote)
                .join(pollVote.getPollOptions(), pollOptions)
                .join(pollVote.getMember(), member)
                .where(pollOptions.getPollItemsId().in(pollOptionIds))
                .groupBy(pollOptions.getPollItemsId(), member.getGender(), member.getAge())
                .fetch();
        return tuples.stream()
                .map(t -> {
                    String gender = t.get(1, String.class);
                    Integer age = t.get(2, Integer.class);
                    String ageGroup = getAgeGroup(age);
                    Long voteCount = t.get(3, Long.class);
                    return PollStaticsDto.builder()
                            .gender(gender)
                            .ageGroup(ageGroup)
                            .voteCount(voteCount)
                            .build();
                })
                .toList();
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

    @Override
    public List<AgeGroupCountDto> getOptionAgeStatics(Long pollId) {
        List<Tuple> tuples = queryFactory.select(
                pollOptions.getOption(),
                new com.querydsl.core.types.dsl.CaseBuilder()
                        .when(member.getAge().lt(20)).then("10대")
                        .when(member.getAge().lt(30)).then("20대")
                        .when(member.getAge().lt(40)).then("30대")
                        .when(member.getAge().lt(50)).then("40대")
                        .when(member.getAge().lt(60)).then("50대")
                        .when(member.getAge().lt(70)).then("60대")
                        .when(member.getAge().lt(80)).then("70대")
                        .otherwise("80대 이상"),
                pollVote.count())
                .from(pollVote)
                .join(pollVote.getPollOptions(), pollOptions)
                .join(pollVote.getMember(), member)
                .where(pollOptions.getPoll().getPollId().eq(pollId))
                .groupBy(pollOptions.getOption(),
                        new com.querydsl.core.types.dsl.CaseBuilder()
                                .when(member.getAge().lt(20)).then("10대")
                                .when(member.getAge().lt(30)).then("20대")
                                .when(member.getAge().lt(40)).then("30대")
                                .when(member.getAge().lt(50)).then("40대")
                                .when(member.getAge().lt(60)).then("50대")
                                .when(member.getAge().lt(70)).then("60대")
                                .when(member.getAge().lt(80)).then("70대")
                                .otherwise("80대 이상"))
                .fetch();
        return tuples.stream()
                .map(t -> new AgeGroupCountDto(
                        t.get(0, String.class),
                        t.get(1, String.class),
                        t.get(2, Long.class)
                ))
                .toList();
    }

    @Override
    public List<GenderCountDto> getOptionGenderStatics(Long pollId) {
        List<Tuple> tuples = queryFactory.select(
                pollOptions.getOption(),
                member.getGender(),
                pollVote.count())
                .from(pollVote)
                .join(pollVote.getPollOptions(), pollOptions)
                .join(pollVote.getMember(), member)
                .where(pollOptions.getPoll().getPollId().eq(pollId))
                .groupBy(pollOptions.getOption(), member.getGender())
                .fetch();
        return tuples.stream()
                .map(t -> new GenderCountDto(
                        t.get(0, String.class),
                        t.get(1, Member.Gender.class).name(),
                        t.get(2, Long.class)
                ))
                .toList();
    }
}
