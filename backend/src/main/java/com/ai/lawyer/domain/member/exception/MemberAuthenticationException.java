package com.ai.lawyer.domain.member.exception;

public class MemberAuthenticationException extends RuntimeException {
    public MemberAuthenticationException(String message) {
        super(message);
    }
}