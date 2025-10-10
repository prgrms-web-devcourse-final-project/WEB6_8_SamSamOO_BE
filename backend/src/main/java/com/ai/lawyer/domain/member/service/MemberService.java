package com.ai.lawyer.domain.member.service;

import com.ai.lawyer.domain.member.dto.*;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.OAuth2Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.email.service.EmailService;
import com.ai.lawyer.global.email.service.EmailAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private OAuth2MemberRepository oauth2MemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;
    private final EmailService emailService;
    private final EmailAuthService emailAuthService;

    public MemberService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider,
            CookieUtil cookieUtil,
            EmailService emailService,
            EmailAuthService emailAuthService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.cookieUtil = cookieUtil;
        this.emailService = emailService;
        this.emailAuthService = emailAuthService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setOauth2MemberRepository(OAuth2MemberRepository oauth2MemberRepository) {
        this.oauth2MemberRepository = oauth2MemberRepository;
    }

    // 에러 메시지 상수
    private static final String ERR_DUPLICATE_EMAIL = "이미 존재하는 이메일입니다.";
    private static final String ERR_MEMBER_NOT_FOUND = "존재하지 않는 회원입니다.";
    private static final String ERR_PASSWORD_MISMATCH = "비밀번호가 일치하지 않습니다.";
    private static final String ERR_INVALID_REFRESH_TOKEN = "유효하지 않은 리프레시 토큰입니다.";
    private static final String ERR_MEMBER_NOT_FOUND_BY_LOGIN_ID = "해당 로그인 ID의 회원이 없습니다.";
    private static final String ERR_EMAIL_VERIFICATION_REQUIRED = "이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다.";

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
                .orElseThrow(() -> new IllegalArgumentException(ERR_MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException(ERR_PASSWORD_MISMATCH);
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
        log.debug("토큰 재발급 시작: refreshToken={}", refreshToken.substring(0, Math.min(10, refreshToken.length())) + "...");

        // 1. 리프레시 토큰으로 loginId 찾기
        String loginId = tokenProvider.findUsernameByRefreshToken(refreshToken);
        if (loginId == null) {
            log.warn("Redis에서 리프레시 토큰을 찾을 수 없습니다.");
            throw new IllegalArgumentException(ERR_INVALID_REFRESH_TOKEN);
        }
        log.debug("리프레시 토큰으로 찾은 loginId: {}", loginId);

        // 2. 리프레시 토큰 검증
        if (!tokenProvider.validateRefreshToken(loginId, refreshToken)) {
            log.warn("리프레시 토큰 검증 실패: loginId={}", loginId);
            throw new IllegalArgumentException(ERR_INVALID_REFRESH_TOKEN);
        }

        // 3. Member 또는 OAuth2Member 조회
        com.ai.lawyer.domain.member.entity.MemberAdapter member = memberRepository.findByLoginId(loginId).orElse(null);

        if (member != null) {
            log.info("로컬 회원 찾음: loginId={}, memberId={}", loginId, member.getMemberId());
        } else if (oauth2MemberRepository != null) {
            member = oauth2MemberRepository.findByLoginId(loginId).orElse(null);
            if (member != null) {
                log.info("OAuth2 회원 찾음: loginId={}, memberId={}", loginId, member.getMemberId());
            }
        }

        if (member == null) {
            log.error("회원을 찾을 수 없습니다: loginId={}", loginId);
            throw new IllegalArgumentException(ERR_MEMBER_NOT_FOUND);
        }

        // 4. 기존 토큰 삭제
        tokenProvider.deleteAllTokens(loginId);
        log.debug("기존 토큰 삭제 완료: loginId={}", loginId);

        // 5. 새 토큰 생성
        String newAccessToken = tokenProvider.generateAccessToken(member);
        String newRefreshToken = tokenProvider.generateRefreshToken(member);
        log.debug("새 토큰 생성 완료: loginId={}", loginId);

        // 6. 쿠키 설정
        cookieUtil.setTokenCookies(response, newAccessToken, newRefreshToken);
        log.info("토큰 재발급 성공: loginId={}, memberId={}, memberType={}",
                loginId, member.getMemberId(), member.getClass().getSimpleName());

        return MemberResponse.from(member);
    }


    public MemberResponse getMemberById(Long memberId) {
        // Member 또는 OAuth2Member 조회
        com.ai.lawyer.domain.member.entity.MemberAdapter member = memberRepository.findById(memberId).orElse(null);

        if (member == null && oauth2MemberRepository != null) {
            member = oauth2MemberRepository.findById(memberId).orElse(null);
        }

        if (member == null) {
            throw new IllegalArgumentException(ERR_MEMBER_NOT_FOUND);
        }

        return MemberResponse.from(member);
    }

    public String getLoginIdByMemberId(Long memberId) {
        // Member 또는 OAuth2Member 조회
        com.ai.lawyer.domain.member.entity.MemberAdapter member = memberRepository.findById(memberId).orElse(null);

        if (member == null && oauth2MemberRepository != null) {
            member = oauth2MemberRepository.findById(memberId).orElse(null);
        }

        if (member == null) {
            log.warn("회원을 찾을 수 없습니다: memberId={}", memberId);
            return null;
        }

        return member.getLoginId();
    }

    @Transactional
    public void deleteMember(String loginId) {
        // Member 또는 OAuth2Member 삭제
        java.util.Optional<Member> regularMember = memberRepository.findByLoginId(loginId);
        if (regularMember.isPresent()) {
            memberRepository.delete(regularMember.get());
            log.info("일반 회원 삭제 완료: loginId={}", loginId);
            return;
        }

        if (oauth2MemberRepository != null) {
            java.util.Optional<OAuth2Member> oauth2Member = oauth2MemberRepository.findByLoginId(loginId);
            if (oauth2Member.isPresent()) {
                oauth2MemberRepository.delete(oauth2Member.get());
                log.info("OAuth2 회원 삭제 완료: loginId={}", loginId);
                return;
            }
        }

        log.warn("삭제할 회원을 찾을 수 없습니다: loginId={}", loginId);
    }

    public void sendCodeToEmailByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_MEMBER_NOT_FOUND_BY_LOGIN_ID));
        String email = member.getLoginId();
        emailService.sendVerificationCode(email, loginId);
    }

    public boolean verifyAuthCode(String loginId, String verificationCode) {
        memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_MEMBER_NOT_FOUND));

        return emailAuthService.verifyAuthCode(loginId, verificationCode);
    }

    public boolean verifyPassword(String loginId, String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_MEMBER_NOT_FOUND));

        boolean isValid = passwordEncoder.matches(password, member.getPassword());

        // 비밀번호 검증 성공 시 Redis에 인증 성공 표시 저장
        if (isValid) {
            emailAuthService.markPasswordVerified(loginId);
        }

        return isValid;
    }

    @Transactional
    public void resetPassword(String loginId, String newPassword, Boolean success) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_MEMBER_NOT_FOUND));

        boolean clientSuccess = Boolean.TRUE.equals(success);

        if (!clientSuccess) {
            throw new IllegalArgumentException(ERR_EMAIL_VERIFICATION_REQUIRED);
        }

        boolean redisVerified = emailAuthService.isEmailVerified(loginId);
        if (!redisVerified) {
            throw new IllegalArgumentException(ERR_EMAIL_VERIFICATION_REQUIRED);
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

    public Long extractMemberIdFromToken(String token) {
        return tokenProvider.getMemberIdFromToken(token);
    }

    @Transactional
    public MemberResponse oauth2LoginTest(OAuth2LoginTestRequest request, HttpServletResponse response) {
        if (oauth2MemberRepository == null) {
            throw new IllegalStateException("OAuth2 기능이 비활성화되어 있습니다.");
        }

        // 기존 OAuth2 회원 조회
        OAuth2Member oauth2Member = oauth2MemberRepository.findByLoginId(request.getEmail()).orElse(null);

        if (oauth2Member == null) {
            // 신규 OAuth2 회원 생성
            oauth2Member = OAuth2Member.builder()
                    .loginId(request.getEmail())  // loginId와 email을 동일하게 설정
                    .email(request.getEmail())    // email 컬럼에도 저장
                    .name(request.getName())
                    .age(request.getAge())
                    .gender(Member.Gender.valueOf(request.getGender()))
                    .provider(OAuth2Member.Provider.valueOf(request.getProvider()))
                    .providerId(request.getProviderId())
                    .role(Member.Role.USER)
                    .build();
            oauth2Member = oauth2MemberRepository.save(oauth2Member);
        }

        // JWT 토큰 생성 및 쿠키 설정
        String accessToken = tokenProvider.generateAccessToken(oauth2Member);
        String refreshToken = tokenProvider.generateRefreshToken(oauth2Member);
        cookieUtil.setTokenCookies(response, accessToken, refreshToken);

        return MemberResponse.from(oauth2Member);
    }

    private void validateDuplicateLoginId(String loginId) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException(ERR_DUPLICATE_EMAIL);
        }
    }
}
