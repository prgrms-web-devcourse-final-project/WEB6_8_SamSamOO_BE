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
            // 토큰에서 사용자 정보 추출
            Long memberId = tokenProvider.getMemberIdFromToken(token);
            String role = tokenProvider.getRoleFromToken(token);

            if (memberId == null) {
                log.warn("토큰에서 memberId를 추출할 수 없습니다.");
                return;
            }

            // 권한 설정 (토큰에서 추출한 role 사용)
            String authority = "ROLE_" + (role != null ? role : "USER");
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));

            // memberId를 principal로 사용하는 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("인증 정보 설정 실패: {}", e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 인증이 필요없는 경로들 (구체적으로 명시)
        return path.equals("/api/auth/signup") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/refresh") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/swagger-") ||
               path.startsWith("/v3/api-docs");
    }
}