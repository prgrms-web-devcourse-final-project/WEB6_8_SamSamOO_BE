package com.ai.lawyer.domain.post.repository;

import com.ai.lawyer.domain.post.entity.Post;
import com.ai.lawyer.domain.poll.entity.Poll.PollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // member_id로 직접 조회 (Member, OAuth2Member 모두 지원)
    List<Post> findByMemberId(Long memberId);

    /**
     * member_id로 게시글 삭제 (회원 탈퇴 시 사용)
     * Member와 OAuth2Member 모두 같은 member_id 공간을 사용하므로 Long 타입으로 삭제
     */
    @Modifying
    @Query("DELETE FROM Post p WHERE p.memberId = :memberId")
    void deleteByMemberIdValue(@Param("memberId") Long memberId);

    Page<Post> findByMemberId(Long memberId, Pageable pageable);
    Page<Post> findByPoll_Status(PollStatus status, Pageable pageable);
    Page<Post> findByPoll_StatusAndPoll_PollIdIn(PollStatus status, List<Long> pollIds, Pageable pageable);
    Page<Post> findByPoll_PollIdIn(List<Long> pollIds, Pageable pageable);
    boolean existsByPostName(String postName);
    List<Post> findByPostName(String postName);
}