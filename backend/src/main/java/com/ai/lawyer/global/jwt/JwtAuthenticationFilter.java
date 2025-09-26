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
            // 1. Authorization 헤더에서 Bearer 토큰 추출 시도 (우선순위 1)
            String accessToken = extractTokenFromAuthorizationHeader(request);
            boolean fromHeader = accessToken != null;

            // 2. Authorization 헤더에 없으면 쿠키에서 토큰 추출 (우선순위 2)
            if (accessToken == null) {
                accessToken = cookieUtil.getAccessTokenFromCookies(request);
            }

            // JWT 액세스 토큰 검증 및 인증 처리
            if (accessToken != null) {
                TokenProvider.TokenValidationResult validationResult = tokenProvider.validateTokenWithResult(accessToken);

                if (validationResult == TokenProvider.TokenValidationResult.VALID) {
                    // 유효한 토큰인 경우 인증 처리
                    setAuthentication(accessToken);
                } else if (validationResult == TokenProvider.TokenValidationResult.EXPIRED && !fromHeader) {
                    // 만료된 토큰이고 쿠키에서 왔을 경우에만 자동 갱신 시도
                    // (Authorization 헤더 토큰은 클라이언트가 직접 관리해야 함)
                    tryAutoRefreshToken(request, response, accessToken);
                }
                // INVALID인 경우 아무 처리 하지 않음 (인증되지 않은 상태로 진행)
            }
        }

        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출합니다.
     * @param request HTTP 요청
     * @return Bearer 토큰 값 또는 null
     */
    private String extractTokenFromAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // "Bearer " 제거
        }
        return null;
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
     * 만료된 액세스 토큰으로 자동 갱신을 시도합니다.
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param expiredAccessToken 만료된 액세스 토큰
     */
    private void tryAutoRefreshToken(HttpServletRequest request, HttpServletResponse response, String expiredAccessToken) {
        try {
            // 1. 만료된 토큰에서 loginId 추출
            String loginId = tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken);
            if (loginId == null) {
                log.warn("만료된 토큰에서 loginId 추출 실패");
                return;
            }

            // 2. 쿠키에서 리프레시 토큰 추출
            String refreshToken = cookieUtil.getRefreshTokenFromCookies(request);
            if (refreshToken == null) {
                log.info("리프레시 토큰이 없어 자동 갱신 불가: {}", loginId);
                return;
            }

            // 3. 리프레시 토큰 유효성 검증
            if (!tokenProvider.validateRefreshToken(loginId, refreshToken)) {
                log.info("유효하지 않은 리프레시 토큰으로 자동 갱신 불가: {}", loginId);
                return;
            }

            // 4. 회원 정보 조회
            Member member = memberRepository.findByLoginId(loginId).orElse(null);
            if (member == null) {
                log.warn("존재하지 않는 회원으로 자동 갱신 불가: {}", loginId);
                return;
            }

            // 5. RTR(Refresh Token Rotation) 패턴: 기존 리프레시 토큰 삭제
            tokenProvider.deleteRefreshToken(loginId);

            // 6. 새로운 액세스 토큰과 리프레시 토큰 생성
            String newAccessToken = tokenProvider.generateAccessToken(member);
            String newRefreshToken = tokenProvider.generateRefreshToken(member);

            // 7. 새로운 토큰들을 쿠키에 설정
            cookieUtil.setTokenCookies(response, newAccessToken, newRefreshToken);

            // 8. 새로운 액세스 토큰으로 인증 설정
            setAuthentication(newAccessToken);

            log.info("액세스 토큰 자동 갱신 성공: {}", loginId);

        } catch (Exception e) {
            log.warn("액세스 토큰 자동 갱신 실패: {}", e.getMessage());
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