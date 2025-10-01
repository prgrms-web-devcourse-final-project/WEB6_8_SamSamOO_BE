package com.ai.lawyer.domain.member.entity;

/**
 * Member와 OAuth2Member를 통합해서 처리하기 위한 어댑터 인터페이스
 * TokenProvider, JwtAuthenticationFilter 등에서 동일한 방식으로 처리 가능
 */
public interface MemberAdapter {
    Long getMemberId();
    String getLoginId();
    String getName();
    Integer getAge();
    Member.Gender getGender();
    Member.Role getRole();

    /**
     * 이메일 반환
     * - Member: loginId 반환 (loginId가 이메일)
     * - OAuth2Member: email 컬럼 반환
     */
    default String getEmail() {
        if (isLocalMember()) {
            return getLoginId(); // 로컬 회원은 loginId가 이메일
        } else if (isOAuth2Member()) {
            return ((OAuth2Member) this).getEmail();
        }
        return null;
    }

    /**
     * 로컬 회원인지 확인
     */
    default boolean isLocalMember() {
        return this instanceof Member;
    }

    /**
     * OAuth2 회원인지 확인
     */
    default boolean isOAuth2Member() {
        return this instanceof OAuth2Member;
    }
}
