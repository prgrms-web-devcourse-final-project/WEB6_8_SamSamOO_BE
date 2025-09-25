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
    public MemberResponse signup(MemberSignupRequest request) {
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
        return MemberResponse.from(savedMember);
    }

    public MemberResponse login(MemberLoginRequest request, HttpServletResponse response) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 액세스 토큰과 리프레시 토큰 생성 후 HTTP 쿠키에 설정
        String accessToken = tokenProvider.generateAccessToken(member);
        String refreshToken = tokenProvider.generateRefreshToken(member);
        cookieUtil.setTokenCookies(response, accessToken, refreshToken);

        return MemberResponse.from(member);
    }

    public void logout(String loginId, HttpServletResponse response) {
        // 로그인 ID가 존재할 경우 Redis에서 리프레시 토큰 삭제
        if (loginId != null && !loginId.trim().isEmpty()) {
            tokenProvider.deleteRefreshToken(loginId);
        }

        // 인증 상태와 관계없이 클라이언트 쿠키 클리어
        cookieUtil.clearTokenCookies(response);
    }

    public MemberResponse refreshToken(String refreshToken, HttpServletResponse response) {
        // Redis에서 리프레시 토큰으로 사용자 찾기
        String username = tokenProvider.findUsernameByRefreshToken(refreshToken);
        if (username == null) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 리프레시 토큰 유효성 검증
        if (!tokenProvider.validateRefreshToken(username, refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 회원 정보 조회
        Member member = memberRepository.findByLoginId(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // RTR(Refresh Token Rotation) 패턴: 기존 리프레시 토큰 삭제
        tokenProvider.deleteRefreshToken(username);

        // 새로운 액세스 토큰과 리프레시 토큰 생성
        String newAccessToken = tokenProvider.generateAccessToken(member);
        String newRefreshToken = tokenProvider.generateRefreshToken(member);

        // 새로운 토큰들을 HTTP 쿠키에 설정
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
        String email = member.getLoginId(); // loginId가 이메일이므로 바로 사용
        emailService.sendVerificationCode(email, loginId); // Redis에 저장 + 메일 전송
    }

    /**
     * 이메일 인증번호 검증 (일반 용도)
     */
    public boolean verifyAuthCode(String loginId, String verificationCode) {
        // 회원 존재 여부 확인
        memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 인증번호 검증
        return emailAuthService.verifyAuthCode(loginId, verificationCode);
    }


    /**
     * 비밀번호 재설정 실행
     */
    @Transactional
    public void resetPassword(String loginId, String newPassword, Boolean success) {
        // 회원 존재 여부 확인
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 인증 성공 여부 확인
        if (!Boolean.TRUE.equals(success)) {
            throw new IllegalArgumentException("이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다.");
        }

        // 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(newPassword);
        member.updatePassword(encodedPassword);
        memberRepository.save(member);

        // 기존 리프레시 토큰 삭제 (보안상 로그아웃 처리)
        tokenProvider.deleteRefreshToken(loginId);
    }

    /**
     * JWT 토큰에서 loginId 추출
     */
    public String extractLoginIdFromToken(String token) {
        return tokenProvider.getLoginIdFromToken(token);
    }

    /**
     * 로그인 ID 중복 검사
     * @param loginId 검사할 로그인 ID
     * @throws IllegalArgumentException 중복된 로그인 ID인 경우
     */
    private void validateDuplicateLoginId(String loginId) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
    }
}
