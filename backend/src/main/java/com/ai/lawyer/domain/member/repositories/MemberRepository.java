package com.ai.lawyer.domain.member.repositories;

import com.ai.lawyer.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MemberRepository
 * findById 호출 시 SmartMemberRepositoryImpl을 통해 AuthUtil로 자동 리다이렉트됩니다.
 * 이를 통해 loginType에 따라 Member 또는 OAuth2Member 테이블에서 조회합니다.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<Member> findByLoginIdIn(List<String> loginIds);
}
