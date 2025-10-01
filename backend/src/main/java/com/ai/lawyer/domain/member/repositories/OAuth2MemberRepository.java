package com.ai.lawyer.domain.member.repositories;

import com.ai.lawyer.domain.member.entity.OAuth2Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuth2MemberRepository extends JpaRepository<OAuth2Member, Long> {

    /**
     * loginId(email)로 OAuth2 회원 조회
     */
    Optional<OAuth2Member> findByLoginId(String loginId);

    /**
     * loginId(email) 존재 여부 확인
     */
    boolean existsByLoginId(String loginId);

    /**
     * Provider와 ProviderId로 OAuth2 회원 조회
     */
    Optional<OAuth2Member> findByProviderAndProviderId(OAuth2Member.Provider provider, String providerId);
}
