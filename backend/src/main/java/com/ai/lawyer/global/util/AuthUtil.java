package com.ai.lawyer.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public class AuthUtil {
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

}
