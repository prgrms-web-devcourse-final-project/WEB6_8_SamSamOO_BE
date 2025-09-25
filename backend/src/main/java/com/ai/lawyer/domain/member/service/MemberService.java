package com.ai.lawyer.domain.member.service;

import com.ai.lawyer.domain.member.dto.*;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.jwt.CookieUtil;
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

        // 토큰 생성 및 쿠키 설정
        String accessToken = tokenProvider.generateAccessToken(member);
        String refreshToken = tokenProvider.generateRefreshToken(member);
        cookieUtil.setTokenCookies(response, accessToken, refreshToken);

        // TODO: 추후 레디스에 토큰-회원 매핑 정보 저장

        return MemberResponse.from(member);
    }

    public void logout(String loginId, HttpServletResponse response) {
        // loginId가 있는 경우에만 Redis에서 리프레시 토큰 삭제
        if (loginId != null && !loginId.trim().isEmpty()) {
            tokenProvider.deleteRefreshToken(loginId);
        }

        // 쿠키는 항상 클리어 (인증 정보가 없어도 클라이언트의 쿠키는 삭제해야 함)
        cookieUtil.clearTokenCookies(response);
    }

    public MemberResponse refreshToken(String refreshToken, HttpServletResponse response) {
        // Redis에서 리프레시 토큰으로 사용자를 찾기 (Redis 키 패턴: refresh_token:loginId)
        String username = tokenProvider.findUsernameByRefreshToken(refreshToken);
        if (username == null) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // Redis에서 리프레시 토큰 유효성 검증
        if (!tokenProvider.validateRefreshToken(username, refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 회원 정보 조회
        Member member = memberRepository.findByLoginId(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 기존 리프레시 토큰 Redis에서 삭제 (RTR 패턴)
        tokenProvider.deleteRefreshToken(username);

        // 새로운 액세스 토큰과 리프레시 토큰 생성
        String newAccessToken = tokenProvider.generateAccessToken(member);
        String newRefreshToken = tokenProvider.generateRefreshToken(member);

        // 새로운 토큰들을 쿠키로 설정
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

    public Member findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private void validateDuplicateLoginId(String loginId) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
    }
}
