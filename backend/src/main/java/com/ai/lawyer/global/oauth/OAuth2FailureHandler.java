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

    @Value("${custom.oauth2.failure-url:http://localhost:8080/api/auth/oauth2/callback/failure}")
    private String failureUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 로그인 실패: {}", exception.getMessage());

        // 에러 메시지를 URL-safe하게 인코딩
        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "알 수 없는 오류";
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);

        String targetUrl = UriComponentsBuilder.fromUriString(failureUrl)
                .queryParam("error", encodedError)
                .build(true)  // true로 설정하여 이미 인코딩된 값을 사용
                .toUriString();

        log.info("OAuth2 로그인 실패, 백엔드 콜백으로 리다이렉트: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}