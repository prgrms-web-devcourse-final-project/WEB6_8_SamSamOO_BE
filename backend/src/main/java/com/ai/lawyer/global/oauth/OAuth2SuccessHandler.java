package com.ai.lawyer.global.oauth;

import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;
    private final OAuth2TestPageUtil oauth2TestPageUtil;

    @Value("${custom.oauth2.redirect-url}")
    private String frontendRedirectUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private static final int HEALTH_CHECK_TIMEOUT = 2000; // 2초

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        com.ai.lawyer.domain.member.entity.MemberAdapter member = principalDetails.getMember();

        log.info("OAuth2 로그인 성공: memberId={}, email={}",
                member.getMemberId(), member.getLoginId());

        // JWT 토큰 생성 (Redis 저장 포함)
        String accessToken = tokenProvider.generateAccessToken(member);
        String refreshToken = tokenProvider.generateRefreshToken(member);

        // 쿠키에 토큰 설정
        cookieUtil.setTokenCookies(response, accessToken, refreshToken);

        log.info("JWT 토큰 생성 완료 및 쿠키 설정 완료");

        // mode 파라미터 확인
        String mode = request.getParameter("mode");

        if ("backend".equals(mode)) {
            // 백엔드 테스트 모드: JSON 응답 반환
            log.info("OAuth2 로그인 완료 (백엔드 테스트 모드)");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(String.format(
                "{\"success\":true,\"message\":\"OAuth2 로그인 성공\",\"memberId\":%d,\"loginId\":\"%s\",\"accessToken\":\"%s\",\"refreshToken\":\"%s\"}",
                member.getMemberId(), member.getLoginId(), accessToken, refreshToken
            ));
        } else {
            // 프론트엔드 모드: 프론트엔드로 리다이렉트 또는 백엔드 성공 페이지 표시
            if (isDevelopmentEnvironment() && !isFrontendAvailable()) {
                // 프론트엔드 서버가 없으면 백엔드 성공 페이지 HTML 직접 반환
                log.warn("프론트엔드 서버({})가 응답하지 않습니다. 백엔드 성공 페이지를 반환합니다.", frontendRedirectUrl);
                response.setContentType("text/html;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_OK);

                String htmlContent = oauth2TestPageUtil.getSuccessPageHtml(
                    "OAuth2 로그인 성공",
                    "로그인에 성공했습니다!",
                    String.format("회원 ID: %d<br>이메일: %s", member.getMemberId(), member.getLoginId())
                );
                response.getWriter().write(htmlContent);
            } else {
                // 프론트엔드 서버로 리다이렉트
                log.info("OAuth2 로그인 완료, 프론트엔드로 리다이렉트: {}", frontendRedirectUrl);
                getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);
            }
        }
    }

    /**
     * 개발 환경인지 확인
     */
    private boolean isDevelopmentEnvironment() {
        return "dev".equals(activeProfile) || "local".equals(activeProfile);
    }

    /**
     * 프론트엔드 서버가 동작 중인지 확인
     */
    private boolean isFrontendAvailable() {
        try {
            URI uri = URI.create(frontendRedirectUrl);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HEALTH_CHECK_TIMEOUT);
            connection.setReadTimeout(HEALTH_CHECK_TIMEOUT);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 200-299 또는 404도 서버가 살아있는 것으로 간주
            boolean isAvailable = (responseCode >= 200 && responseCode < 300) || responseCode == 404;
            log.debug("프론트엔드 헬스체크: {} - 응답코드 {}", frontendRedirectUrl, responseCode);
            return isAvailable;
        } catch (Exception e) {
            log.debug("프론트엔드 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }
}
