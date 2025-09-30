package com.ai.lawyer.domain.member.service;

import com.ai.lawyer.domain.member.dto.*;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.email.service.EmailService;
import com.ai.lawyer.global.email.service.EmailAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;
    private final EmailService emailService;
    private final EmailAuthService emailAuthService;

    @Transactional
    public MemberResponse signup(MemberSignupRequest request, HttpServletResponse response) {
        validateDuplicateLoginId(request.getLoginId());

        Member member = Member.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .age(request.getAge())
                .gender(request.getGender())
                .name(request.getName())
                .role(Member.Role.USER)
                .build();

        Member savedMember = memberRepository.save(member);

        String accessToken = tokenProvider.generateAccessToken(savedMember);
        String refreshToken = tokenProvider.generateRefreshToken(savedMember);
        cookieUtil.setTokenCookies(response, accessToken, refreshToken);

        return MemberResponse.from(savedMember);
    }

    public MemberResponse login(MemberLoginRequest request, HttpServletResponse response) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = tokenProvider.generateAccessToken(member);
        String refreshToken = tokenProvider.generateRefreshToken(member);
        cookieUtil.setTokenCookies(response, accessToken, refreshToken);

        return MemberResponse.from(member);
    }

    public void logout(String loginId, HttpServletResponse response) {
        if (loginId != null && !loginId.trim().isEmpty()) {
            tokenProvider.deleteAllTokens(loginId);
        }

        cookieUtil.clearTokenCookies(response);
    }

    public MemberResponse refreshToken(String refreshToken, HttpServletResponse response) {
        String loginId = tokenProvider.findUsernameByRefreshToken(refreshToken);
        if (loginId == null) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        if (!tokenProvider.validateRefreshToken(loginId, refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        tokenProvider.deleteAllTokens(loginId);

        String newAccessToken = tokenProvider.generateAccessToken(member);
        String newRefreshToken = tokenProvider.generateRefreshToken(member);

        cookieUtil.setTokenCookies(response, newAccessToken, newRefreshToken);

        return MemberResponse.from(member);
    }

    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        memberRepository.delete(member);
    }

    public MemberResponse getMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return MemberResponse.from(member);
    }

    public void sendCodeToEmailByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("해당 로그인 ID의 회원이 없습니다."));
        String email = member.getLoginId();
        emailService.sendVerificationCode(email, loginId);
    }

    public boolean verifyAuthCode(String loginId, String verificationCode) {
        memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return emailAuthService.verifyAuthCode(loginId, verificationCode);
    }

    public boolean verifyPassword(String loginId, String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return passwordEncoder.matches(password, member.getPassword());
    }

    @Transactional
    public void resetPassword(String loginId, String newPassword, Boolean success) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        boolean clientSuccess = Boolean.TRUE.equals(success);

        if (!clientSuccess) {
            throw new IllegalArgumentException("이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다.");
        }

        boolean redisVerified = emailAuthService.isEmailVerified(loginId);
        if (!redisVerified) {
            throw new IllegalArgumentException("이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다.");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        member.updatePassword(encodedPassword);
        memberRepository.save(member);

        emailAuthService.clearAuthData(loginId);

        tokenProvider.deleteAllTokens(loginId);
    }

    public String extractLoginIdFromToken(String token) {
        return tokenProvider.getLoginIdFromToken(token);
    }

    private void validateDuplicateLoginId(String loginId) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
    }
}
