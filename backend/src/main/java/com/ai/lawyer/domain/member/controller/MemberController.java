package com.ai.lawyer.domain.member.controller;

import com.ai.lawyer.domain.member.dto.MemberLoginRequest;
import com.ai.lawyer.domain.member.dto.MemberResponse;
import com.ai.lawyer.domain.member.dto.MemberSignupRequest;
import com.ai.lawyer.domain.member.entity.Member;
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
@Tag(name = "Member", description = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 이메일/닉네임, 유효성 검증 실패)")
    })
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody MemberSignupRequest request) {
        log.info("회원가입 요청: email={}, nickname={}", request.getLoginId(), request.getNickname());

        try {
            MemberResponse response = memberService.signup(request);
            log.info("회원가입 성공: memberId={}", response.getMemberId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (존재하지 않는 회원, 비밀번호 불일치)")
    })
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody MemberLoginRequest request,
                                              HttpServletResponse response) {
        log.info("로그인 요청: email={}", request.getLoginId());

        try {
            MemberResponse memberResponse = memberService.login(request, response);
            log.info("로그인 성공: memberId={}", memberResponse.getMemberId());
            return ResponseEntity.ok(memberResponse);
        } catch (IllegalArgumentException e) {
            log.warn("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        log.info("로그아웃 요청");

        memberService.logout(response);
        log.info("로그아웃 완료");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<MemberResponse> refreshToken(HttpServletRequest request,
                                                     HttpServletResponse response) {
        log.info("토큰 재발급 요청");

        // 쿠키에서 리프레시 토큰 추출 (간단한 방법)
        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            log.warn("리프레시 토큰이 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            MemberResponse memberResponse = memberService.refreshToken(refreshToken, response);
            log.info("토큰 재발급 성공: memberId={}", memberResponse.getMemberId());
            return ResponseEntity.ok(memberResponse);
        } catch (IllegalArgumentException e) {
            log.warn("토큰 재발급 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원탈퇴", description = "현재 로그인된 사용자의 계정을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    })
    public ResponseEntity<Void> withdraw(Authentication authentication, HttpServletResponse response) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("인증되지 않은 회원탈퇴 요청");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loginId = authentication.getName();
        log.info("회원탈퇴 요청: email={}", loginId);

        try {
            // loginId로 Member를 조회하여 실제 memberId 사용
            Member member = memberService.findByLoginId(loginId);
            memberService.withdraw(member.getMemberId());
            memberService.logout(response); // 탈퇴 후 로그아웃 처리
            log.info("회원탈퇴 성공: email={}, memberId={}", loginId, member.getMemberId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("회원탈퇴 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<MemberResponse> getMyInfo(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("인증되지 않은 정보 조회 요청");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loginId = authentication.getName();
        log.info("내 정보 조회 요청: email={}", loginId);

        try {
            // loginId로 Member를 조회하여 실제 memberId 사용
            Member member = memberService.findByLoginId(loginId);
            MemberResponse response = memberService.getMemberById(member.getMemberId());
            log.info("내 정보 조회 성공: memberId={}", response.getMemberId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("내 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

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
}