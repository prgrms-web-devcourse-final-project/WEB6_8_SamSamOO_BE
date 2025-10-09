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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${custom.oauth2.failure-url}")
    private String failureUrl;

    private final OAuth2TestPageUtil oauth2TestPageUtil;

    public OAuth2FailureHandler(OAuth2TestPageUtil oauth2TestPageUtil) {
        this.oauth2TestPageUtil = oauth2TestPageUtil;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 로그인 실패: {}", exception.getMessage());

        // mode 파라미터 확인 (기본값: frontend)
        String mode = request.getParameter("mode");

        if ("backend".equals(mode)) {
            // 백엔드 테스트 모드: HTML 에러 페이지 반환 (팝업 자동 닫기 포함)
            log.info("OAuth2 로그인 실패 (백엔드 테스트 모드)");
            response.setContentType("text/html;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            String errorMessage = exception.getMessage() != null ? exception.getMessage() : "알 수 없는 오류";

            String htmlContent = oauth2TestPageUtil.getFailurePageHtml(errorMessage);
            response.getWriter().write(htmlContent);
        } else {
            // 프론트엔드 모드: 리다이렉트
            String errorMessage = exception.getMessage() != null ? exception.getMessage() : "알 수 없는 오류";
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