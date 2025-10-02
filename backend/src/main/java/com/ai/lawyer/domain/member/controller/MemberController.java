package com.ai.lawyer.domain.member.controller;

import com.ai.lawyer.domain.auth.dto.OAuth2LoginResponse;
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

    // --- 상수들: 중복 문자열 리터럴 방지 ---
    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String ERR_MSG_LOGIN_ID_REQUIRED = "loginId가 필요합니다. 로그인하거나 요청에 loginId를 포함해주세요.";
    private static final String LOG_JWT_EXTRACT_WARN = "JWT 토큰에서 loginId 추출 중 오류: {}";
    private static final String LOG_JWT_EXTRACT_INFO = "JWT 토큰에서 loginId 추출 성공: {}";
    private static final String LOG_JWT_EXTRACT_FAIL = "JWT 토큰에서 loginId 추출 실패";
    private static final String LOG_INVALID_AUTH = "인증 정보 없음 또는 인증되지 않음";

    // ---------------- API ----------------

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

    @GetMapping("/oauth2/kakao")
    @Operation(summary = "11. 카카오 로그인", description = "카카오 OAuth2 로그인을 시작합니다.")
    public void kakaoLogin(HttpServletResponse response) throws Exception {
        log.info("카카오 로그인 요청");
        response.sendRedirect("/oauth2/authorization/kakao");
    }

    @GetMapping("/oauth2/naver")
    @Operation(summary = "12. 네이버 로그인", description = "네이버 OAuth2 로그인을 시작합니다.")
    public void naverLogin(HttpServletResponse response) throws Exception {
        log.info("네이버 로그인 요청");
        response.sendRedirect("/oauth2/authorization/naver");
    }

    @GetMapping("/oauth2/callback/success")
    @Operation(summary = "14. OAuth2 로그인 성공 콜백", description = "OAuth2 로그인 성공 시 호출되는 콜백 엔드포인트입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
    })
    public ResponseEntity<OAuth2LoginResponse> oauth2LoginSuccess() {
        log.info("OAuth2 로그인 성공 콜백");

        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(true)
                .message("소셜 로그인에 성공했습니다.")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/callback/failure")
    @Operation(summary = "15. OAuth2 로그인 실패 콜백", description = "OAuth2 로그인 실패 시 호출되는 콜백 엔드포인트입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "401", description = "로그인 실패"),
    })
    public ResponseEntity<OAuth2LoginResponse> oauth2LoginFailure(
            @RequestParam(required = false) String error) {
        log.error("OAuth2 로그인 실패: {}", error);

        OAuth2LoginResponse response = OAuth2LoginResponse.builder()
                .success(false)
                .message("소셜 로그인에 실패했습니다: " + (error != null ? error : "알 수 없는 오류"))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/oauth2/test")
    @Operation(summary = "13. OAuth2 로그인 테스트 (개발용)", description = "OAuth2 플로우 없이 소셜 로그인 결과를 시뮬레이션합니다.")
    public ResponseEntity<MemberResponse> oauth2LoginTest(
            @Valid @RequestBody OAuth2LoginTestRequest request,
            HttpServletResponse response) {
        log.info("OAuth2 로그인 테스트: email={}, provider={}", request.getEmail(), request.getProvider());
        MemberResponse memberResponse = memberService.oauth2LoginTest(request, response);
        return ResponseEntity.ok(memberResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "09. 로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다.")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletResponse response) {
        if (authentication != null && authentication.getDetails() != null) {
            String loginId = (String) authentication.getDetails();
            memberService.logout(loginId, response);
            log.info("로그아웃 완료: {}", loginId);
        } else {
            memberService.logout("", response);
            log.info("인증 정보 없이 로그아웃 완료");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "04. 토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다. JwtAuthenticationFilter가 자동으로 토큰을 갱신합니다.")
    public ResponseEntity<MemberResponse> refreshToken(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.warn("토큰 재발급 실패: {}", LOG_INVALID_AUTH);
            throw new com.ai.lawyer.domain.member.exception.MemberAuthenticationException("인증이 필요합니다.");
        }

        Long memberId = (Long) authentication.getPrincipal();
        MemberResponse response = memberService.getMemberById(memberId);
        log.info("토큰 재발급 성공: memberId={}", memberId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "10. 회원탈퇴", description = "현재 로그인된 사용자의 계정을 삭제합니다.")
    public ResponseEntity<Void> withdraw(Authentication authentication, HttpServletResponse response) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.ai.lawyer.domain.member.exception.MemberAuthenticationException("인증이 필요합니다.");
        }
        Long memberId = (Long) authentication.getPrincipal();
        String loginId = (String) authentication.getDetails();
        memberService.withdraw(memberId);
        memberService.logout(loginId, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "03. 내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    public ResponseEntity<MemberResponse> getMyInfo(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new com.ai.lawyer.domain.member.exception.MemberAuthenticationException("인증이 필요합니다.");
        }
        Long memberId = (Long) authentication.getPrincipal();
        MemberResponse response = memberService.getMemberById(memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sendEmail")
    @Operation(summary = "05. 인증번호 전송", description = "로그인된 사용자는 자동으로 인증번호를 받고, 비로그인 사용자는 요청 바디의 loginId로 인증번호를 받습니다.")
    public ResponseEntity<EmailResponse> sendEmail(
            @RequestBody(required = false) MemberEmailRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest request) {

        String loginId = resolveLoginId(authentication, request,
                requestDto != null ? requestDto.getLoginId() : null);

        memberService.sendCodeToEmailByLoginId(loginId);
        return ResponseEntity.ok(EmailResponse.success("이메일 전송 성공", loginId));
    }

    @PostMapping("/verifyEmail")
    @Operation(summary = "06. 인증번호 검증", description = "이메일로 받은 인증번호를 검증합니다.")
    public ResponseEntity<VerificationResponse> verifyEmail(
            @RequestBody @Valid EmailVerifyCodeRequestDto requestDto) {

        if (requestDto.getLoginId() == null || requestDto.getLoginId().isBlank()) {
            throw new IllegalArgumentException("인증번호를 검증할 이메일 주소가 필요합니다.");
        }
        boolean isValid = memberService.verifyAuthCode(requestDto.getLoginId(), requestDto.getVerificationCode());
        if (isValid) {
            return ResponseEntity.ok(VerificationResponse.success("인증번호 검증 성공", requestDto.getLoginId()));
        } else {
            throw new IllegalArgumentException("잘못된 인증번호이거나 만료된 인증번호입니다.");
        }
    }

    @PostMapping("/verifyPassword")
    @Operation(summary = "07. 비밀번호 검증", description = "로그인된 사용자가 비밀번호를 통해 인증합니다.")
    public ResponseEntity<VerificationResponse> verifyPassword(
            @RequestBody @Valid PasswordVerifyRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest request){

        String loginId = resolveLoginId(authentication, request, null);
        boolean isValid = memberService.verifyPassword(loginId, requestDto.getPassword());

        if (isValid) {
            return ResponseEntity.ok(VerificationResponse.success("비밀번호 검증 성공", loginId));
        } else {
            throw new IllegalArgumentException("잘못된 입력입니다.");
        }
    }

    @PostMapping("/passwordReset")
    @Operation(summary = "08. 비밀번호 재설정", description = "인증 토큰과 함께 새 비밀번호로 재설정합니다.")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @RequestBody ResetPasswordRequestDto request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        validateResetPasswordRequest(request);

        String loginId = resolveLoginId(authentication, httpRequest, request.getLoginId());

        memberService.resetPassword(loginId, request.getNewPassword(), request.getSuccess());
        memberService.logout(loginId, httpResponse);

        return ResponseEntity.ok(
                PasswordResetResponse.success("비밀번호가 성공적으로 재설정되었습니다.", loginId)
        );
    }

    // -------------------- 공통 유틸 메서드 --------------------

    private void validateResetPasswordRequest(ResetPasswordRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 바디가 필요합니다.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }
        if (request.getSuccess() == null) {
            throw new IllegalArgumentException("인증 성공 여부가 필요합니다.");
        }
    }

    /**
     * 인증 정보(authentication) 우선으로 loginId를 찾고, 없으면 fallbackLoginId를 사용한다.
     * authentication이 존재하면 Authorization header 또는 accessToken 쿠키에서 추출을 시도한다.
     * 실패 시 fallback으로 넘어간다. fallback도 없으면 IllegalArgumentException 발생.
     * 이 메서드는 중첩(네스팅)을 줄이고, 로깅을 하나의 위치로 모아 Sonar의 Cognitive Complexity 규칙을 만족하도록 구성함.
     */
    private String resolveLoginId(Authentication authentication, HttpServletRequest request, String fallbackLoginId) {
        // 1) 인증된 사용자이고, 프린시펄이 anonymousUser가 아닌 경우 토큰에서 loginId 추출 시도
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal != null && !ANONYMOUS_USER.equals(principal)) {
                try {
                    String token = extractAccessTokenFromRequest(request);
                    if (token != null) {
                        String resolved = memberService.extractLoginIdFromToken(token);
                        if (resolved != null) {
                            log.info(LOG_JWT_EXTRACT_INFO, resolved);
                            return resolved;
                        } else {
                            log.warn(LOG_JWT_EXTRACT_FAIL);
                        }
                    } else {
                        log.debug("Authorization header / accessToken cookie 없음");
                    }
                } catch (Exception e) {
                    // 단일 위치에서 로그를 남기고, 내부 오류는 무시하여 fallback 흐름으로 진행
                    log.warn(LOG_JWT_EXTRACT_WARN, e.getMessage());
                }
            }
        }

        // 2) fallbackLoginId 검증 및 반환
        if (fallbackLoginId != null && !fallbackLoginId.isBlank()) {
            return fallbackLoginId;
        }

        // 3) 찾지 못했으면 예외
        throw new IllegalArgumentException(ERR_MSG_LOGIN_ID_REQUIRED);
    }

    private String extractAccessTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
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
