package com.ai.lawyer.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogoutResponse {
    private boolean success;
    private String message;
    private String oauth2LogoutUrl; // OAuth2 제공자 로그아웃 URL (없으면 null)

    public static LogoutResponse of(String oauth2LogoutUrl) {
        return LogoutResponse.builder()
                .success(true)
                .message("로그아웃 성공")
                .oauth2LogoutUrl(oauth2LogoutUrl)
                .build();
    }
}
