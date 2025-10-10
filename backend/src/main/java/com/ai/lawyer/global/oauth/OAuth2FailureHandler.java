package com.ai.lawyer.global.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${custom.oauth2.failure-url}")
    private String failureUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private final OAuth2TestPageUtil oauth2TestPageUtil;

    private static final int HEALTH_CHECK_TIMEOUT = 2000; // 2초

    public OAuth2FailureHandler(OAuth2TestPageUtil oauth2TestPageUtil) {
        this.oauth2TestPageUtil = oauth2TestPageUtil;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 로그인 실패: {}", exception.getMessage());

        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "알 수 없는 오류";

        // mode 파라미터 확인
        String mode = request.getParameter("mode");

        if ("backend".equals(mode)) {
            // 백엔드 테스트 모드: HTML 에러 페이지 반환
            log.info("OAuth2 로그인 실패 (백엔드 테스트 모드)");
            response.setContentType("text/html;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            String htmlContent = oauth2TestPageUtil.getFailurePageHtml(errorMessage);
            response.getWriter().write(htmlContent);
        } else {
            // 프론트엔드 모드: 프론트엔드로 리다이렉트 또는 백엔드 실패 페이지 표시
            if (isDevelopmentEnvironment() && !isFrontendAvailable()) {
                // 프론트엔드 서버가 없으면 백엔드 실패 페이지 HTML 직접 반환
                log.warn("프론트엔드 서버({})가 응답하지 않습니다. 백엔드 실패 페이지를 반환합니다.", failureUrl);
                response.setContentType("text/html;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_OK);

                String htmlContent = oauth2TestPageUtil.getFailurePageHtml(errorMessage);
                response.getWriter().write(htmlContent);
            } else {
                // 프론트엔드 서버로 리다이렉트
                String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
                String targetUrl = UriComponentsBuilder.fromUriString(failureUrl)
                        .queryParam("error", encodedError)
                        .build(true)
                        .toUriString();
                log.info("OAuth2 로그인 실패, 프론트엔드 실패 페이지로 리다이렉트: {}", targetUrl);

                getRedirectStrategy().sendRedirect(request, response, targetUrl);
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
            URI uri = URI.create(failureUrl);
            // 쿼리 파라미터를 제거하고 베이스 URL만 추출
            String baseUrl = uri.getScheme() + "://" + uri.getHost() +
                           (uri.getPort() != -1 ? ":" + uri.getPort() : "");

            HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HEALTH_CHECK_TIMEOUT);
            connection.setReadTimeout(HEALTH_CHECK_TIMEOUT);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 200-299 또는 404도 서버가 살아있는 것으로 간주
            boolean isAvailable = (responseCode >= 200 && responseCode < 300) || responseCode == 404;
            log.debug("프론트엔드 헬스체크: {} - 응답코드 {}", baseUrl, responseCode);
            return isAvailable;
        } catch (Exception e) {
            log.debug("프론트엔드 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }
}