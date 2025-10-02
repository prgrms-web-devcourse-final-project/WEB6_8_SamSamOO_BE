package com.ai.lawyer.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuth2LoginResponse {
    private boolean success;
    private String message;
}