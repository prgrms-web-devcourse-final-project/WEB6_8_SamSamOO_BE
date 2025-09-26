package com.ai.lawyer.domain.member.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResponse {
    private String message;
    private String email;
    private LocalDateTime timestamp;
    private boolean success;

    public static EmailResponse success(String message, String email) {
        return EmailResponse.builder()
                .message(message)
                .email(email)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
}