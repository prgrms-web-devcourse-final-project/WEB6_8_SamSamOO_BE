package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable FilterChain filterChain)
            throws ServletException, IOException {

        if (request != null && response != null) {
            try {
                // 1. 쿠키에서 액세스 토큰 확인
                String accessToken = cookieUtil.getAccessTokenFromCookies(request);

                if (accessToken != null) {
                    // 액세스 토큰이 있는 경우 검증
                    TokenProvider.TokenValidationResult validationResult = tokenProvider.validateTokenWithResult(accessToken);

                    if (validationResult == TokenProvider.TokenValidationResult.VALID) {
                        // 유효한 액세스 토큰 - 인증 처리
                        setAuthentication(accessToken);
                        log.debug("유효한 액세스 토큰으로 인증 완료");
                    } else if (validationResult == TokenProvider.TokenValidationResult.EXPIRED) {
                        // 만료된 액세스 토큰 - 리프레시 토큰으로 갱신 시도
                        log.info("액세스 토큰 만료, 리프레시 토큰으로 갱신 시도");
                        handleTokenRefresh(request, response, accessToken);
                    } else {
                        // 유효하지 않은 액세스 토큰 - 리프레시 토큰 확인
                        log.warn("유효하지 않은 액세스 토큰, 리프레시 토큰으로 갱신 시도");
                        handleTokenRefresh(request, response, null);
                    }
                } else {
                    // 4. 액세스 토큰이 없는 경우 바로 리프레시 토큰 확인
                    log.debug("액세스 토큰이 없음, 리프레시 토큰 확인");
                    handleTokenRefresh(request, response, null);
                }
            } catch (Exception e) {
                log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage(), e);
                clearAuthenticationAndCookies(response);
            }
        }

        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * JWT 토큰에서 사용자 정보를 추출하여 Spring Security 인증 객체를 설정합니다.
     * @param token JWT 액세스 토큰
     */
    private void setAuthentication(String token) {
        try {
            Long memberId = tokenProvider.getMemberIdFromToken(token);
            String role = tokenProvider.getRoleFromToken(token);

            if (memberId == null) {
                log.warn("토큰에서 memberId를 추출할 수 없습니다.");
                return;
            }

            // Spring Security 권한 형식으로 변환
            String authority = "ROLE_" + (role != null ? role : "USER");
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));

            // memberId를 principal로 하는 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("인증 정보 설정 실패: {}", e.getMessage());
        }
    }

    /**
     * 리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다.
     * RTR(Refresh Token Rotation) 패턴을 적용하여 새로운 토큰 쌍을 생성합니다.
     */
    private void handleTokenRefresh(HttpServletRequest request, HttpServletResponse response, String expiredAccessToken) {
        try {
            // 리프레시 토큰 확인
            String refreshToken = cookieUtil.getRefreshTokenFromCookies(request);
            if (refreshToken == null) {
                // 리프레시 토큰이 없을 경우 쿠키 클리어
                log.info("리프레시 토큰이 없음 - 쿠키 클리어 및 재로그인 필요");
                clearAuthenticationAndCookies(response);
                return;
            }

            // loginId 추출 시도 (만료된 토큰이 있으면 그것에서, 없으면 리프레시 토큰으로 찾기)
            String loginId = null;
            if (expiredAccessToken != null) {
                loginId = tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken);
            }

            // 만료된 토큰에서 추출 실패 시 리프레시 토큰으로 사용자 찾기
            if (loginId == null) {
                loginId = tokenProvider.findUsernameByRefreshToken(refreshToken);
            }

            if (loginId == null) {
                log.warn("loginId 추출 실패 - 쿠키 클리어");
                clearAuthenticationAndCookies(response);
                return;
            }

            // 3. 리프레시 토큰이 Redis의 저장값과 동일한지 검증
            if (!tokenProvider.validateRefreshToken(loginId, refreshToken)) {
                log.info("유효하지 않은 리프레시 토큰 - 쿠키 클리어: {}", loginId);
                clearAuthenticationAndCookies(response);
                return;
            }

            // 회원 정보 조회
            Member member = memberRepository.findByLoginId(loginId).orElse(null);
            if (member == null) {
                log.warn("존재하지 않는 회원 - 쿠키 클리어: {}", loginId);
                clearAuthenticationAndCookies(response);
                return;
            }

            // RTR(Refresh Token Rotation) 패턴: 기존 모든 토큰 삭제
            tokenProvider.deleteAllTokens(loginId);

            // 새로운 액세스 토큰과 리프레시 토큰 생성
            String newAccessToken = tokenProvider.generateAccessToken(member);
            String newRefreshToken = tokenProvider.generateRefreshToken(member);

            // 새로운 토큰들을 쿠키에 설정
            cookieUtil.setTokenCookies(response, newAccessToken, newRefreshToken);

            // 새로운 액세스 토큰으로 인증 설정
            setAuthentication(newAccessToken);

            log.info("토큰 자동 갱신 성공: {}", loginId);

        } catch (Exception e) {
            log.error("토큰 갱신 처리 실패: {}", e.getMessage(), e);
            clearAuthenticationAndCookies(response);
        }
    }

    /**
     * 인증 정보와 쿠키를 모두 클리어합니다.
     */
    private void clearAuthenticationAndCookies(HttpServletResponse response) {
        // Spring Security 인증 정보 클리어
        SecurityContextHolder.clearContext();

        // 쿠키 클리어
        cookieUtil.clearTokenCookies(response);

        log.debug("인증 정보 및 쿠키 클리어 완료");
    }

    /**
     * JWT 인증이 필요하지 않은 경로들을 필터링에서 제외합니다.
     * @param request HTTP 요청
     * @return true인 경우 필터 제외
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/signup") ||
               path.equals("/api/auth/login") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/actuator/health") ||
               path.startsWith("/h2-console");
    }
}