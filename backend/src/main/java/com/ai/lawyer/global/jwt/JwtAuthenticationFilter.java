package com.ai.lawyer.global.jwt;

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

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable FilterChain filterChain)
            throws ServletException, IOException {

        if (request != null) {
            String accessToken = cookieUtil.getAccessTokenFromCookies(request);

            // JWT 액세스 토큰 검증 및 인증 처리
            if (accessToken != null && tokenProvider.validateToken(accessToken)) {
                setAuthentication(accessToken);
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
     * JWT 인증이 필요하지 않은 경로들을 필터링에서 제외합니다.
     * @param request HTTP 요청
     * @return true인 경우 필터 제외
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/signup") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/refresh") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/swagger-") ||
               path.startsWith("/v3/api-docs");
    }
}