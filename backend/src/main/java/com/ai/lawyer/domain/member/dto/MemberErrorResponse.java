package com.ai.lawyer.domain.member.dto;

import java.time.LocalDateTime;

public record MemberErrorResponse(
        String message,
        int status,
        String error,
        LocalDateTime timestamp
) {
    public static MemberErrorResponse of(String message, int status, String error) {
        return new MemberErrorResponse(message, status, error, LocalDateTime.now());
    }
}
