package com.ai.lawyer.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MemberErrorResponse {
    private final String message;
    private final int status;
    private final String error;
    private final LocalDateTime timestamp;

    public static MemberErrorResponse of(String message, int status, String error) {
        return new MemberErrorResponse(message, status, error, LocalDateTime.now());
    }
}