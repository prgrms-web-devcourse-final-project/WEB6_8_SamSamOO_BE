package com.ai.lawyer.domain.member.controller;

import com.ai.lawyer.domain.member.dto.*;
import com.ai.lawyer.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "회원 관리", description = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "01. 회원가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 이메일/닉네임, 유효성 검증 실패)")
    })
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody MemberSignupRequest request, HttpServletResponse response) {
        log.info("회원가입 요청: email={}, name={}", request.getLoginId(), request.getName());

        MemberResponse memberResponse = memberService.signup(request, response);
        log.info("회원가입 및 자동 로그인 성공: memberId={}", memberResponse.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "02. 로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (존재하지 않는 회원, 비밀번호 불일치)")
    })
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody MemberLoginRequest request,
                                              HttpServletResponse response) {
        log.info("로그인 요청: email={}", request.getLoginId());

        MemberResponse memberResponse = memberService.login(request, response);
        log.info("로그인 성공: memberId={}", memberResponse.getMemberId());
        return ResponseEntity.ok(memberResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "08. 로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletResponse response) {
        log.info("로그아웃 요청");

        if (authentication != null && authentication.getName() != null) {
            String loginId = authentication.getName();
            memberService.logout(loginId, response);
            log.info("로그아웃 완료: memberId={}", loginId);
        } else {
            // 인증되지 않은 상태에서도 클라이언트 쿠키 클리어 처리
            memberService.logout("", response);
            log.info("인증 정보 없이 로그아웃 완료");
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "04. 토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<MemberResponse> refreshToken(HttpServletRequest request,
                                                     HttpServletResponse response) {
        log.info("토큰 재발급 요청");

        // HTTP 쿠키에서 리프레시 토큰 추출
        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            throw new com.ai.lawyer.domain.member.exception.MemberAuthenticationException("리프레시 토큰이 없습니다.");
        }

        MemberResponse memberResponse = memberService.refreshToken(refreshToken, response);
        log.info("토큰 재발급 성공: memberId={}", memberResponse.getMemberId());
        return ResponseEntity.ok(memberResponse);
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "09. 회원탈퇴", description = "현재 로그인된 사용자의 계정을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    })
    public ResponseEntity<Void> withdraw(Authentication authentication, HttpServletResponse response) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.ai.lawyer.domain.member.exception.MemberAuthenticationException("인증이 필요합니다.");
        }

        Long memberId = (Long) authentication.getPrincipal();
        String loginId = (String) authentication.getDetails();
        log.info("회원탈퇴 요청: memberId={}, email={}", memberId, loginId);

        memberService.withdraw(memberId);
        memberService.logout(loginId, response); // 회원 탈퇴 후 세션 및 토큰 정리
        log.info("회원탈퇴 성공: memberId={}, email={}", memberId, loginId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "03. 내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<MemberResponse> getMyInfo(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.ai.lawyer.domain.member.exception.MemberAuthenticationException("인증이 필요합니다.");
        }

        Long memberId = (Long) authentication.getPrincipal();
        log.info("내 정보 조회 요청: memberId={}", memberId);

        MemberResponse response = memberService.getMemberById(memberId);
        log.info("내 정보 조회 성공: memberId={}", response.getMemberId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sendEmail")
    @Operation(summary = "05. 인증번호 전송", description = "로그인된 사용자는 자동으로 인증번호를 받고, 비로그인 사용자는 요청 바디의 loginId(이메일)로 인증번호를 받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (loginId 없음)")
    })
    public ResponseEntity<EmailResponse> sendEmail(
            @RequestBody(required = false) MemberEmailRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest request) {

        String loginId = null;

        // 1. 로그인된 사용자인 경우 JWT 토큰에서 loginId 추출 (우선순위 1)
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {

            // JWT 토큰에서 직접 loginid claim 추출
            try {
                String token = extractAccessTokenFromRequest(request);
                if (token != null) {
                    loginId = memberService.extractLoginIdFromToken(token);
                    if (loginId != null) {
                        log.info("JWT 토큰에서 loginId 추출 성공: {}", loginId);
                    } else {
                        log.warn("JWT 토큰에서 loginId 추출 실패");
                    }
                }
            } catch (Exception e) {
                log.warn("JWT 토큰에서 loginId 추출 중 오류: {}", e.getMessage());
            }
        }

        // 2. 비로그인 사용자인 경우 요청 바디에서 loginId 추출 (우선순위 2)
        if (loginId == null) {
            if (requestDto != null && requestDto.getLoginId() != null && !requestDto.getLoginId().isBlank()) {
                loginId = requestDto.getLoginId();
                log.info("요청 바디에서 loginId 추출 성공: {}", loginId);
            } else {
                log.error("로그인하지 않은 상태에서 요청 바디에 loginId가 없음");
                throw new IllegalArgumentException("인증번호를 전송할 이메일 주소가 필요합니다. 로그인하거나 요청 바디에 loginId를 포함해주세요.");
            }
        }

        try {
            // 서비스 호출
            memberService.sendCodeToEmailByLoginId(loginId);
            log.info("이메일 인증번호 전송 성공: {}", loginId);
            return ResponseEntity.ok(EmailResponse.success("이메일 전송 성공", loginId));

        } catch (IllegalArgumentException e) {
            log.error("이메일 전송 실패 - 존재하지 않는 회원: {}", loginId);
            throw e;
        } catch (Exception e) {
            log.error("이메일 전송 실패: loginId={}, error={}", loginId, e.getMessage());
            throw new RuntimeException("이메일 전송 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/verifyEmail")
    @Operation(summary = "06. 인증번호 검증", description = "로그인된 사용자는 자동으로 인증번호를 검증하고, 비로그인 사용자는 요청 바디의 loginId(이메일)와 함께 인증번호를 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증번호 검증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (인증번호 불일치, loginId 없음)")
    })
    public ResponseEntity<EmailResponse> verifyEmail(
            @RequestBody @Valid EmailVerifyCodeRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest request) {

        String loginId = null;

        // 1. 로그인된 사용자인 경우 JWT 토큰에서 loginId 추출 (우선순위 1)
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {

            // JWT 토큰에서 직접 loginid claim 추출
            try {
                String token = extractAccessTokenFromRequest(request);
                if (token != null) {
                    loginId = memberService.extractLoginIdFromToken(token);
                    if (loginId != null) {
                        log.info("JWT 토큰에서 loginId 추출 성공: {}", loginId);
                    } else {
                        log.warn("JWT 토큰에서 loginId 추출 실패");
                    }
                }
            } catch (Exception e) {
                log.warn("JWT 토큰에서 loginId 추출 중 오류: {}", e.getMessage());
            }
        }

        // 2. 비로그인 사용자인 경우 요청 바디에서 loginId 추출 (우선순위 2)
        if (loginId == null) {
            if (requestDto.getLoginId() != null && !requestDto.getLoginId().isBlank()) {
                loginId = requestDto.getLoginId();
                log.info("요청 바디에서 loginId 추출 성공: {}", loginId);
            } else {
                log.error("로그인하지 않은 상태에서 요청 바디에 loginId가 없음");
                throw new IllegalArgumentException("인증번호를 검증할 이메일 주소가 필요합니다. 로그인하거나 요청 바디에 loginId를 포함해주세요.");
            }
        }

        try {
            // 서비스 호출 - 인증번호 검증
            boolean isValid = memberService.verifyAuthCode(loginId, requestDto.getVerificationCode());

            if (isValid) {
                log.info("이메일 인증번호 검증 성공: {}", loginId);
                return ResponseEntity.ok(EmailResponse.success("인증번호 검증 성공", loginId));
            } else {
                log.error("이메일 인증번호 검증 실패 - 잘못된 인증번호: {}", loginId);
                throw new IllegalArgumentException("잘못된 인증번호이거나 만료된 인증번호입니다.");
            }

        } catch (IllegalArgumentException e) {
            log.error("이메일 인증번호 검증 실패: loginId={}, error={}", loginId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이메일 인증번호 검증 중 오류 발생: loginId={}, error={}", loginId, e.getMessage());
            throw new RuntimeException("인증번호 검증 중 오류가 발생했습니다.");
        }
    }

    // ===== 비밀번호 재설정 엔드포인트 =====

    @PostMapping("/password-reset/reset")
    @Operation(summary = "07. 비밀번호 재설정", description = "인증 토큰과 함께 새 비밀번호로 재설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "인증되지 않았거나 잘못된 요청")
    })
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @RequestBody ResetPasswordRequestDto request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // 입력값 검증
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }
        if (request.getSuccess() == null) {
            throw new IllegalArgumentException("인증 성공 여부가 필요합니다.");
        }

        String loginId = null;

        // 1. 로그인된 사용자인 경우 JWT 토큰에서 loginId 추출 (우선순위 1)
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {

            // JWT 토큰에서 직접 loginid claim 추출
            try {
                String token = extractAccessTokenFromRequest(httpRequest);
                if (token != null) {
                    loginId = memberService.extractLoginIdFromToken(token);
                    if (loginId != null) {
                        log.info("JWT 토큰에서 loginId 추출 성공: {}", loginId);
                    } else {
                        log.warn("JWT 토큰에서 loginId 추출 실패");
                    }
                }
            } catch (Exception e) {
                log.warn("JWT 토큰에서 loginId 추출 중 오류: {}", e.getMessage());
            }
        }

        // 2. 비로그인 사용자인 경우 요청 바디에서 loginId 추출 (우선순위 2)
        if (loginId == null) {
            if (request.getLoginId() != null && !request.getLoginId().isBlank()) {
                loginId = request.getLoginId();
                log.info("요청 바디에서 loginId 추출 성공: {}", loginId);
            } else {
                log.error("로그인하지 않은 상태에서 요청 바디에 loginId가 없음");
                throw new IllegalArgumentException("비밀번호를 재설정할 이메일 주소가 필요합니다. 로그인하거나 요청 바디에 loginId를 포함해주세요.");
            }
        }

        log.info("비밀번호 재설정 요청: email={}", loginId);

        memberService.resetPassword(loginId, request.getNewPassword(), request.getSuccess());

        log.info("비밀번호 재설정 성공: email={}", loginId);
        return ResponseEntity.ok(PasswordResetResponse.success("비밀번호가 성공적으로 재설정되었습니다.", loginId));
    }

    /**
     * HTTP 쿠키에서 리프레시 토큰을 추출합니다.
     * @param request HTTP 요청 객체
     * @return 리프레시 토큰 값 또는 null
     */
    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * HTTP 쿠키에서 액세스 토큰을 추출합니다.
     * @param request HTTP 요청 객체
     * @return 액세스 토큰 값 또는 null
     */
    private String extractAccessTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 추출 시도
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. 쿠키에서 추출 시도
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}