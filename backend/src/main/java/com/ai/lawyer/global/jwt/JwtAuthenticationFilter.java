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

            // 액세스 토큰이 있는 경우
            if (accessToken != null && tokenProvider.validateToken(accessToken)) {
                // 유효한 토큰 - 인증 정보 설정
                setAuthentication(accessToken);
            }
        }

        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    private void setAuthentication(String token) {
        try {
            // TODO: 실제 JWT 구현 시 토큰에서 사용자 정보 추출
            // 현재는 임시로 토큰에서 사용자명 추출
            String username = tokenProvider.getUsernameFromToken(token);

            // 간단한 권한 설정 (실제로는 토큰에서 권한 정보도 추출)
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("인증 정보 설정 실패: {}", e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 인증이 필요없는 경로들
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/swagger-") ||
               path.startsWith("/v3/api-docs");
    }
}