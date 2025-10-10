package com.ai.lawyer.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    // 쿠키 이름 상수
    private static final String ACCESS_TOKEN_NAME = "accessToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";

    // 쿠키 만료 시간 상수 (초 단위)
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final int ACCESS_TOKEN_EXPIRE_TIME = 5 * 60; // 5분 (300초)
    private static final int REFRESH_TOKEN_EXPIRE_TIME = 7 * HOURS_PER_DAY * MINUTES_PER_HOUR * 60; // 7일

    // 쿠키 보안 설정 상수
    private static final boolean HTTP_ONLY = true;
    private static final boolean SECURE_IN_PRODUCTION = false; // 개발환경에서는 false (HTTP), 운영환경에서는 true로 변경 (HTTPS)
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax"; // Lax: 같은 사이트 요청에서 쿠키 전송 허용
    private static final int COOKIE_EXPIRE_IMMEDIATELY = 0;

    public void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = createResponseCookie(ACCESS_TOKEN_NAME, accessToken, ACCESS_TOKEN_EXPIRE_TIME);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = createResponseCookie(REFRESH_TOKEN_NAME, refreshToken, REFRESH_TOKEN_EXPIRE_TIME);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearTokenCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_NAME);
        clearCookie(response, REFRESH_TOKEN_NAME);
    }

    /**
     * ResponseCookie를 생성합니다 (SameSite 지원).
     */
    private ResponseCookie createResponseCookie(String name, String value, int maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE_IN_PRODUCTION)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(maxAge))
                .sameSite(SAME_SITE)
                .build();
    }

    /**
     * 쿠키를 삭제합니다 (MaxAge를 0으로 설정).
     */
    private void clearCookie(HttpServletResponse response, String cookieName) {
        ResponseCookie cookie = createResponseCookie(cookieName, "", COOKIE_EXPIRE_IMMEDIATELY);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getAccessTokenFromCookies(HttpServletRequest request) {
        return getTokenFromCookies(request, ACCESS_TOKEN_NAME);
    }

    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        return getTokenFromCookies(request, REFRESH_TOKEN_NAME);
    }

    private String getTokenFromCookies(HttpServletRequest request, String tokenName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (tokenName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}