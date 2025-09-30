package com.ai.lawyer.domain.member.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponse {
    private String message;
    private String email;
    private LocalDateTime timestamp;
    private boolean success;

    public static VerificationResponse success(String message, String email) {
        return VerificationResponse.builder()
                .message(message)
                .email(email)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
}