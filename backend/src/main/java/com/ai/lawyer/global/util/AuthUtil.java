package com.ai.lawyer.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import com.ai.lawyer.domain.member.entity.Member;

@Component
public class AuthUtil {
    private static MemberRepository memberRepository;
    private static OAuth2MemberRepository oauth2MemberRepository;

    @Autowired
    public AuthUtil(MemberRepository memberRepository) {
        AuthUtil.memberRepository = memberRepository;
    }

    @Autowired(required = false)
    public void setOauth2MemberRepository(OAuth2MemberRepository oauth2MemberRepository) {
        AuthUtil.oauth2MemberRepository = oauth2MemberRepository;
    }

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            System.out.println("[AuthUtil] principal class: " + principal.getClass().getName() + ", value: " + principal);
            switch (principal) {
                case org.springframework.security.core.userdetails.User user -> {
                    try {
                        return Long.parseLong(user.getUsername());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                case String str -> {
                    try {
                        return Long.parseLong(str);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                case Long l -> {
                    return l;
                }
                default -> {
                }
            }
        }
        return null;
    }

    public static String getCurrentMemberRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse(null);
    }

    /**
     * memberId로 회원을 조회합니다. (Member 또는 OAuth2Member)
     * OAuth2Member인 경우 Member 객체로 변환하여 반환합니다.
     * @param memberId 회원 ID
     * @return Member 객체
     * @throws ResponseStatusException 회원을 찾을 수 없는 경우
     */
    public static Member getMemberOrThrow(Long memberId) {
        // 먼저 Member 테이블에서 조회
        java.util.Optional<Member> member = memberRepository.findById(memberId);
        if (member.isPresent()) {
            return member.get();
        }

        // Member 테이블에 없으면 OAuth2Member 테이블에서 조회
        if (oauth2MemberRepository != null) {
            java.util.Optional<com.ai.lawyer.domain.member.entity.OAuth2Member> oauth2Member =
                oauth2MemberRepository.findById(memberId);
            if (oauth2Member.isPresent()) {
                // OAuth2Member를 Member로 변환 (엔티티 호환성을 위해)
                com.ai.lawyer.domain.member.entity.OAuth2Member oauth = oauth2Member.get();
                return Member.builder()
                        .memberId(oauth.getMemberId())
                        .loginId(oauth.getLoginId())
                        .name(oauth.getName())
                        .age(oauth.getAge())
                        .gender(oauth.getGender())
                        .role(oauth.getRole())
                        .password("") // OAuth2는 비밀번호 없음
                        .build();
            }
        }

        // 둘 다 없으면 예외 발생
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다");
    }

    /**
     * memberId와 loginType으로 회원을 조회합니다.
     * loginType이 "LOCAL"이면 Member 테이블에서, "OAUTH2"이면 OAuth2Member 테이블에서 조회합니다.
     * @param memberId 회원 ID
     * @param loginType 로그인 타입 ("LOCAL" 또는 "OAUTH2")
     * @return Member 객체
     * @throws ResponseStatusException 회원을 찾을 수 없는 경우
     */
    public static Member getMemberOrThrow(Long memberId, String loginType) {
        if ("OAUTH2".equals(loginType)) {
            // OAuth2 회원 조회
            if (oauth2MemberRepository != null) {
                java.util.Optional<com.ai.lawyer.domain.member.entity.OAuth2Member> oauth2Member =
                    oauth2MemberRepository.findById(memberId);
                if (oauth2Member.isPresent()) {
                    // OAuth2Member를 Member로 변환
                    com.ai.lawyer.domain.member.entity.OAuth2Member oauth = oauth2Member.get();
                    return Member.builder()
                            .memberId(oauth.getMemberId())
                            .loginId(oauth.getLoginId())
                            .name(oauth.getName())
                            .age(oauth.getAge())
                            .gender(oauth.getGender())
                            .role(oauth.getRole())
                            .password("") // OAuth2는 비밀번호 없음
                            .build();
                }
            }
        } else {
            // LOCAL 회원 조회 (기본값)
            java.util.Optional<Member> member = memberRepository.findById(memberId);
            if (member.isPresent()) {
                return member.get();
            }
        }

        // 찾지 못한 경우 예외 발생
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다");
    }

    public static Long getAuthenticatedMemberId() {
        try {
            Long memberId = getCurrentMemberId();
            if (memberId == null) {
                throw new IllegalArgumentException();
            }
            return memberId;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
    }

    public static void validateOwnerOrAdmin(Long ownerId) {
        Long currentMemberId = getAuthenticatedMemberId();
        String currentRole = getCurrentMemberRole();
        if (!ownerId.equals(currentMemberId) && !"ADMIN".equals(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 또는 관리자만 수정 가능합니다.");
        }
    }

}
