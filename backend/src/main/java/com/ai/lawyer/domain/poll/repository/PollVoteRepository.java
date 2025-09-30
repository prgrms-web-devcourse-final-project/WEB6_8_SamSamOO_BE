package com.ai.lawyer.domain.poll.repository;

import com.ai.lawyer.domain.poll.entity.PollVote;
import com.ai.lawyer.domain.poll.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    @Query("SELECT v.poll.pollId, COUNT(v.pollVoteId) FROM PollVote v WHERE v.poll.status = :status GROUP BY v.poll.pollId ORDER BY COUNT(v.pollVoteId) DESC")
    List<Object[]> findTopPollByStatus(@Param("status") Poll.PollStatus status);

    @Query("SELECT p.pollId, COUNT(v) as voteCount FROM PollVote v JOIN v.poll p WHERE p.status = :status GROUP BY p.pollId ORDER BY voteCount DESC")
    List<Object[]> findTopNPollByStatus(@Param("status") Poll.PollStatus status, Pageable pageable);

    @Query("SELECT COUNT(v.pollVoteId) FROM PollVote v WHERE v.poll.pollId = :pollId")
    Long countByPollId(@Param("pollId") Long pollId);

    @Query("SELECT COUNT(v.pollVoteId) FROM PollVote v WHERE v.pollOptions.pollItemsId = :pollOptionId")
    Long countByPollOptionId(@Param("pollOptionId") Long pollOptionId);

    @Query("SELECT v.pollOptions.pollItemsId, v.member.gender, v.member.age, COUNT(v.pollVoteId) FROM PollVote v WHERE v.pollOptions.pollItemsId IN :pollOptionIds GROUP BY v.pollOptions.pollItemsId, v.member.gender, v.member.age")
    java.util.List<Object[]> countStaticsByPollOptionIds(@Param("pollOptionIds") java.util.List<Long> pollOptionIds);

    boolean existsByPoll_PollIdAndMember_MemberId(Long pollId, Long memberId);

    @Query(value = "SELECT po.option, m.gender, COUNT(*) FROM poll_vote pv JOIN poll_options po ON pv.poll_items_id = po.poll_items_id JOIN member m ON pv.member_id = m.member_id WHERE po.poll_id = :pollId GROUP BY po.option, m.gender", nativeQuery = true)
    List<Object[]> getGenderOptionStatics(@Param("pollId") Long pollId);

    @Query(value = "SELECT CASE WHEN m.age < 20 THEN '10대' WHEN m.age < 30 THEN '20대' " +
            "WHEN m.age < 40 THEN '30대' WHEN m.age < 50 THEN '40대' WHEN m.age < 60 THEN '50대' " +
            "WHEN m.age < 70 THEN '60대' WHEN m.age < 80 THEN '70대' ELSE '80대 이상' " +
            "END AS ageGroup, m.gender, COUNT(*) FROM poll_vote pv JOIN member m ON pv.member_id = m.member_id JOIN poll_options po ON pv.poll_items_id = po.poll_items_id WHERE po.poll_id = :pollId GROUP BY ageGroup, m.gender", nativeQuery = true)
    List<Object[]> getAgeGenderStatics(@Param("pollId") Long pollId);

    @Query("SELECT o.option, " +
           "CASE WHEN m.age < 20 THEN '10대' WHEN m.age < 30 THEN '20대' " +
           "WHEN m.age < 40 THEN '30대' WHEN m.age < 50 THEN '40대' WHEN m.age < 60 THEN '50대' " +
           "WHEN m.age < 70 THEN '60대' WHEN m.age < 80 THEN '70대' ELSE '80대 이상' END, " +
           "COUNT(v) " +
           "FROM PollVote v JOIN v.pollOptions o JOIN v.member m " +
           "WHERE o.poll.pollId = :pollId " +
           "GROUP BY o.option, " +
           "CASE WHEN m.age < 20 THEN '10대' WHEN m.age < 30 THEN '20대' " +
           "WHEN m.age < 40 THEN '30대' WHEN m.age < 50 THEN '40대' WHEN m.age < 60 THEN '50대' " +
           "WHEN m.age < 70 THEN '60대' WHEN m.age < 80 THEN '70대' ELSE '80대 이상' END")
    List<Object[]> getOptionAgeStatics(@Param("pollId") Long pollId);

    @Query("SELECT o.option, m.gender, COUNT(v) FROM PollVote v JOIN v.pollOptions o JOIN v.member m WHERE o.poll.pollId = :pollId GROUP BY o.option, m.gender")
    List<Object[]> getOptionGenderStatics(@Param("pollId") Long pollId);
}
