package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.entity.MemberAdapter;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private TokenProvider tokenProvider;
    private CookieUtil cookieUtil;
    private MemberRepository memberRepository;
    private OAuth2MemberRepository oauth2MemberRepository;

    public JwtAuthenticationFilter() {
        // Default constructor for testing
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setTokenProvider(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setCookieUtil(CookieUtil cookieUtil) {
        this.cookieUtil = cookieUtil;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setMemberRepository(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setOauth2MemberRepository(OAuth2MemberRepository oauth2MemberRepository) {
        this.oauth2MemberRepository = oauth2MemberRepository;
    }

    // 권한 관련 상수
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String DEFAULT_ROLE = "USER";

    // 로그 메시지 상수
    private static final String LOG_TOKEN_EXPIRED = "액세스 토큰 만료, 리프레시 토큰으로 갱신 시도";
    private static final String LOG_INVALID_TOKEN = "유효하지 않은 액세스 토큰, 리프레시 토큰으로 갱신 시도";
    private static final String LOG_NO_REFRESH_TOKEN = "리프레시 토큰이 없음 - 쿠키 클리어 및 재로그인 필요";
    private static final String LOG_LOGIN_ID_EXTRACTION_FAILED = "loginId 추출 실패 - 쿠키 클리어";
    private static final String LOG_INVALID_REFRESH_TOKEN = "유효하지 않은 리프레시 토큰 - 쿠키 클리어: {}";
    private static final String LOG_MEMBER_NOT_FOUND = "존재하지 않는 회원 - 쿠키 클리어: {}";
    private static final String LOG_TOKEN_REFRESH_SUCCESS = "토큰 자동 갱신 성공: {}";
    private static final String LOG_TOKEN_REFRESH_FAILED = "토큰 갱신 처리 실패: {}";
    private static final String LOG_JWT_AUTH_ERROR = "JWT 인증 처리 중 오류 발생: {}";
    private static final String LOG_MEMBER_ID_EXTRACTION_FAILED = "토큰에서 memberId를 추출할 수 없습니다.";
    private static final String LOG_SET_AUTH_FAILED = "인증 정보 설정 실패: {}";

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable FilterChain filterChain)
            throws ServletException, IOException {

        // 테스트 환경에서 의존성이 없는 경우 필터 스킵
        if (tokenProvider == null || cookieUtil == null || memberRepository == null) {
            if (filterChain != null) {
                filterChain.doFilter(request, response);
            }
            return;
        }

        if (request != null && response != null) {
            try {
                processAuthentication(request, response);
            } catch (Exception e) {
                log.error(LOG_JWT_AUTH_ERROR, e.getMessage(), e);
                clearAuthenticationAndCookies(response);
            }
        }

        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 인증 프로세스를 처리합니다.
     */
    private void processAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = cookieUtil.getAccessTokenFromCookies(request);

        if (accessToken != null) {
            handleAccessToken(request, response, accessToken);
        } else {
            // 액세스 토큰이 없는 경우 바로 리프레시 토큰 확인
            handleTokenRefresh(request, response, null);
        }
    }

    /**
     * 액세스 토큰을 검증하고 처리합니다.
     */
    private void handleAccessToken(HttpServletRequest request, HttpServletResponse response, String accessToken) {
        TokenProvider.TokenValidationResult validationResult = tokenProvider.validateTokenWithResult(accessToken);

        switch (validationResult) {
            case VALID:
                // 유효한 액세스 토큰 - 인증 처리
                setAuthentication(accessToken);
                break;
            case EXPIRED:
                // 만료된 액세스 토큰 - 리프레시 토큰으로 갱신 시도
                log.info(LOG_TOKEN_EXPIRED);
                handleTokenRefresh(request, response, accessToken);
                break;
            case INVALID:
                // 유효하지 않은 액세스 토큰 - 리프레시 토큰 확인
                log.warn(LOG_INVALID_TOKEN);
                handleTokenRefresh(request, response, null);
                break;
        }
    }

    /**
     * JWT 토큰에서 사용자 정보를 추출하여 Spring Security 인증 객체를 설정합니다.
     * @param token JWT 액세스 토큰
     */
    private void setAuthentication(String token) {
        try {
            Long memberId = tokenProvider.getMemberIdFromToken(token);
            String loginId = tokenProvider.getLoginIdFromToken(token);
            String role = tokenProvider.getRoleFromToken(token);

            if (memberId == null) {
                log.warn(LOG_MEMBER_ID_EXTRACTION_FAILED);
                return;
            }

            // Spring Security 권한 형식으로 변환
            String authority = buildAuthority(role);
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));

            // memberId를 principal로 하는 인증 객체 생성
            // getName()은 memberId를 반환 (PollController 호환)
            // getDetails()는 loginId를 반환 (MemberController 호환)
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, authorities) {
                    @Override
                    public String getName() {
                        return String.valueOf(memberId);
                    }

                    @Override
                    public Object getDetails() {
                        return loginId;
                    }
                };

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn(LOG_SET_AUTH_FAILED, e.getMessage());
        }
    }

    /**
     * 권한 문자열을 생성합니다.
     */
    private String buildAuthority(String role) {
        return ROLE_PREFIX + (role != null ? role : DEFAULT_ROLE);
    }

    /**
     * 리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다.
     * RTR(Refresh Token Rotation) 패턴을 적용하여 새로운 토큰 쌍을 생성합니다.
     */
    private void handleTokenRefresh(HttpServletRequest request, HttpServletResponse response, String expiredAccessToken) {
        try {
            String refreshToken = cookieUtil.getRefreshTokenFromCookies(request);
            if (refreshToken == null) {
                log.info(LOG_NO_REFRESH_TOKEN);
                clearAuthenticationAndCookies(response);
                return;
            }

            String loginId = extractLoginId(expiredAccessToken, refreshToken);
            if (loginId == null) {
                log.warn(LOG_LOGIN_ID_EXTRACTION_FAILED);
                clearAuthenticationAndCookies(response);
                return;
            }

            if (!tokenProvider.validateRefreshToken(loginId, refreshToken)) {
                log.info(LOG_INVALID_REFRESH_TOKEN, loginId);
                clearAuthenticationAndCookies(response);
                return;
            }

            MemberAdapter member = findMemberByLoginId(loginId);
            if (member == null) {
                log.warn(LOG_MEMBER_NOT_FOUND, loginId);
                clearAuthenticationAndCookies(response);
                return;
            }

            refreshTokensAndSetAuthentication(response, loginId, member);
            log.info(LOG_TOKEN_REFRESH_SUCCESS, loginId);

        } catch (Exception e) {
            log.error(LOG_TOKEN_REFRESH_FAILED, e.getMessage(), e);
            clearAuthenticationAndCookies(response);
        }
    }

    /**
     * loginId를 추출합니다 (만료된 토큰 또는 리프레시 토큰에서).
     */
    private String extractLoginId(String expiredAccessToken, String refreshToken) {
        String loginId = null;
        if (expiredAccessToken != null) {
            loginId = tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken);
        }
        if (loginId == null) {
            loginId = tokenProvider.findUsernameByRefreshToken(refreshToken);
        }
        return loginId;
    }

    /**
     * loginId로 회원 정보를 조회합니다 (Member 또는 OAuth2Member).
     */
    private MemberAdapter findMemberByLoginId(String loginId) {
        MemberAdapter member = memberRepository.findByLoginId(loginId).orElse(null);

        if (member == null && oauth2MemberRepository != null) {
            member = oauth2MemberRepository.findByLoginId(loginId).orElse(null);
        }

        return member;
    }

    /**
     * RTR 패턴으로 토큰을 갱신하고 인증을 설정합니다.
     */
    private void refreshTokensAndSetAuthentication(HttpServletResponse response, String loginId, MemberAdapter member) {
        // RTR(Refresh Token Rotation) 패턴: 기존 모든 토큰 삭제
        tokenProvider.deleteAllTokens(loginId);

        // 새로운 액세스 토큰과 리프레시 토큰 생성
        String newAccessToken = tokenProvider.generateAccessToken(member);
        String newRefreshToken = tokenProvider.generateRefreshToken(member);

        // 새로운 토큰들을 쿠키에 설정
        cookieUtil.setTokenCookies(response, newAccessToken, newRefreshToken);

        // 새로운 액세스 토큰으로 인증 설정
        setAuthentication(newAccessToken);
    }

    /**
     * 인증 정보와 쿠키를 모두 클리어합니다.
     */
    private void clearAuthenticationAndCookies(HttpServletResponse response) {
        // Spring Security 인증 정보 클리어
        SecurityContextHolder.clearContext();

        // 쿠키 클리어
        cookieUtil.clearTokenCookies(response);
    }

}