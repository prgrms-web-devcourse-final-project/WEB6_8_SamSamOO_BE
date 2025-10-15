package com.ai.lawyer.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.entity.Member;

@Component
public class AuthUtil {
    private static MemberRepository memberRepository;

    @Autowired
    public AuthUtil(MemberRepository memberRepository) {
        AuthUtil.memberRepository = memberRepository;
    }

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            System.out.println("[AuthUtil] principal class: " + principal.getClass().getName() + ", value: " + principal);
            if (principal instanceof org.springframework.security.core.userdetails.User user) {
                try {
                    return Long.parseLong(user.getUsername());
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (principal instanceof String str) {
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (principal instanceof Long l) {
                return l;
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
            .map(auth -> auth.getAuthority())
            .orElse(null);
    }

    public static Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다"));
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
