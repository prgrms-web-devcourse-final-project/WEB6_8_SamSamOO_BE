package com.ai.lawyer.global.util;

import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.ai.lawyer.domain.member.entity.Member;

@Component
public class AuthUtil {
    private static EntityManager entityManager;

    @Autowired
    public AuthUtil(EntityManager entityManager) {
        AuthUtil.entityManager = entityManager;
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
     * 현재 인증된 사용자의 로그인 타입을 가져옵니다.
     * @return "LOCAL" 또는 "OAUTH2", 없으면 null
     */
    public static String getCurrentLoginType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object details = authentication.getDetails();
        if (details instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> detailsMap = (java.util.Map<String, String>) details;
            return detailsMap.get("loginType");
        }

        return null;
    }

    /**
     * memberId로 회원을 조회합니다. (Member 또는 OAuth2Member)
     * SecurityContext에서 현재 인증된 사용자의 loginType을 자동으로 확인하여 적절한 테이블에서 조회합니다.
     * OAuth2Member인 경우 Member 객체로 변환하여 반환합니다.
     * EntityManager를 직접 사용하여 무한 루프를 방지합니다.
     * @param memberId 회원 ID
     * @return Member 객체
     * @throws ResponseStatusException 회원을 찾을 수 없는 경우
     */
    public static Member getMemberOrThrow(Long memberId) {
        // SecurityContext에서 loginType 자동 추출
        String loginType = getCurrentLoginType();

        // loginType이 있으면 해당 테이블에서만 조회 (성능 최적화)
        if (loginType != null) {
            return getMemberOrThrow(memberId, loginType);
        }

        // loginType이 없으면 하위 호환성을 위해 두 테이블 모두 조회
        // 먼저 Member 테이블에서 조회 (EntityManager 직접 사용)
        Member member = entityManager.find(Member.class, memberId);
        if (member != null) {
            return member;
        }

        // Member 테이블에 없으면 OAuth2Member 테이블에서 조회 (EntityManager 직접 사용)
        com.ai.lawyer.domain.member.entity.OAuth2Member oauth2Member =
            entityManager.find(com.ai.lawyer.domain.member.entity.OAuth2Member.class, memberId);
        if (oauth2Member != null) {
            // OAuth2Member를 Member로 변환 (엔티티 호환성을 위해)
            return Member.builder()
                    .memberId(oauth2Member.getMemberId())
                    .loginId(oauth2Member.getLoginId())
                    .name(oauth2Member.getName())
                    .age(oauth2Member.getAge())
                    .gender(oauth2Member.getGender())
                    .role(oauth2Member.getRole())
                    .password("") // OAuth2는 비밀번호 없음
                    .build();
        }

        // 둘 다 없으면 예외 발생
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다");
    }

    /**
     * memberId와 loginType으로 회원을 조회합니다.
     * loginType이 "LOCAL"이면 Member 테이블에서, "OAUTH2"이면 OAuth2Member 테이블에서 조회합니다.
     * EntityManager를 직접 사용하여 무한 루프를 방지합니다.
     * @param memberId 회원 ID
     * @param loginType 로그인 타입 ("LOCAL" 또는 "OAUTH2")
     * @return Member 객체
     * @throws ResponseStatusException 회원을 찾을 수 없는 경우
     */
    public static Member getMemberOrThrow(Long memberId, String loginType) {
        if ("OAUTH2".equals(loginType)) {
            // OAuth2 회원 조회 (EntityManager 직접 사용)
            com.ai.lawyer.domain.member.entity.OAuth2Member oauth2Member =
                entityManager.find(com.ai.lawyer.domain.member.entity.OAuth2Member.class, memberId);

            if (oauth2Member != null) {
                // OAuth2Member를 Member로 변환
                return Member.builder()
                        .memberId(oauth2Member.getMemberId())
                        .loginId(oauth2Member.getLoginId())
                        .name(oauth2Member.getName())
                        .age(oauth2Member.getAge())
                        .gender(oauth2Member.getGender())
                        .role(oauth2Member.getRole())
                        .password("") // OAuth2는 비밀번호 없음
                        .build();
            }
        } else {
            // LOCAL 회원 조회 (EntityManager 직접 사용)
            Member member = entityManager.find(Member.class, memberId);
            if (member != null) {
                return member;
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
