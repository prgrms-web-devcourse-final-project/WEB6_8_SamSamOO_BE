package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
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
    @Getter
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
    private static final String LOG_TOKEN_EXPIRED = "액세스 토큰 만료 - 401 반환";
    private static final String LOG_INVALID_TOKEN = "유효하지 않은 액세스 토큰 - 401 반환";
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

        // OAuth2 관련 경로는 JWT 필터 스킵
        if (request != null && shouldSkipFilter(request)) {
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
                SecurityContextHolder.clearContext();
            }
        }

        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * JWT 필터를 스킵해야 하는 경로인지 확인합니다.
     */
    private boolean shouldSkipFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/")
            || path.startsWith("/login/oauth2/")
            || path.startsWith("/api/auth/oauth2/");
    }

    /**
     * 인증 프로세스를 처리합니다.
     */
    private void processAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = cookieUtil.getAccessTokenFromCookies(request);

        if (accessToken != null) {
            handleAccessToken(response, accessToken);
        }
        // 액세스 토큰이 없는 경우 인증 처리하지 않음 (공개 API 허용)
    }

    /**
     * 액세스 토큰을 검증하고 처리합니다.
     */
    private void handleAccessToken(HttpServletResponse response, String accessToken) throws IOException {
        TokenProvider.TokenValidationResult validationResult = tokenProvider.validateTokenWithResult(accessToken);

        switch (validationResult) {
            case VALID:
                // 유효한 액세스 토큰 - 인증 처리
                setAuthentication(accessToken);
                break;
            case EXPIRED:
                // 만료된 액세스 토큰 - 401 반환
                log.info(LOG_TOKEN_EXPIRED);
                sendUnauthorizedError(response);
                break;
            case INVALID:
                // 유효하지 않은 액세스 토큰 - 401 반환
                log.warn(LOG_INVALID_TOKEN);
                sendUnauthorizedError(response);
                break;
        }
    }

    /**
     * 401 Unauthorized 응답을 반환합니다.
     */
    private void sendUnauthorizedError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"토큰이 만료되었거나 유효하지 않습니다. /api/auth/refresh를 호출하여 토큰을 재발급 받으세요.\"}");
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

}