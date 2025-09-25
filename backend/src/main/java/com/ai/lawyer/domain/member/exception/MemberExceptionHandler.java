package com.ai.lawyer.domain.member.exception;

import com.ai.lawyer.domain.member.dto.MemberErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ai.lawyer.domain.member")
@Slf4j
public class MemberExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MemberErrorResponse> handleMemberIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Member 도메인 IllegalArgumentException: {}", e.getMessage());
        MemberErrorResponse errorResponse = MemberErrorResponse.of(
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                "잘못된 요청"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MemberAuthenticationException.class)
    public ResponseEntity<MemberErrorResponse> handleMemberAuthenticationException(MemberAuthenticationException e) {
        log.warn("Member 도메인 AuthenticationException: {}", e.getMessage());
        MemberErrorResponse errorResponse = MemberErrorResponse.of(
                e.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                "인증 실패"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MemberErrorResponse> handleMemberValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        log.warn("Member 도메인 유효성 검증 실패: {}", message);
        MemberErrorResponse errorResponse = MemberErrorResponse.of(
                message,
                HttpStatus.BAD_REQUEST.value(),
                "유효성 검증 실패"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
}