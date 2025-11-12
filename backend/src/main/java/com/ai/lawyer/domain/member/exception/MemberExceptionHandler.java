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

    /**
     * IllegalArgumentException 고도화 처리
     * 메시지에 따라 HTTP 상태코드와 에러 메시지 다르게 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MemberErrorResponse> handleMemberIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Member 도메인 IllegalArgumentException: {}", e.getMessage());

        String msg = e.getMessage();
        HttpStatus status;
        String error = switch (msg) {
            case "이미 존재하는 이메일입니다.", "잘못된 입력입니다." -> {
                status = HttpStatus.BAD_REQUEST;
                yield "잘못된 요청";
            }
            case "존재하지 않는 회원입니다.", "비밀번호가 일치하지 않습니다." -> {
                status = HttpStatus.UNAUTHORIZED;
                yield "인증 실패";
            }
            default -> {
                status = HttpStatus.BAD_REQUEST;
                yield "오류 발생";
            }
        };

        // 메시지 기반으로 상태코드 결정

        MemberErrorResponse errorResponse = MemberErrorResponse.of(msg, status.value(), error);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * 인증 관련 예외 처리
     */
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

    /**
     * 유효성 검증 실패 처리
     */
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
