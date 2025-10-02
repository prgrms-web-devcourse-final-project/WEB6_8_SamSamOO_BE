package com.ai.lawyer.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    // 쿠키 이름 상수
    private static final String ACCESS_TOKEN_NAME = "accessToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";

    // 쿠키 만료 시간 상수 (초 단위)
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final int ACCESS_TOKEN_EXPIRE_TIME = 5 * MINUTES_PER_HOUR; // 5분
    private static final int REFRESH_TOKEN_EXPIRE_TIME = 7 * HOURS_PER_DAY * MINUTES_PER_HOUR * 60; // 7일

    // 쿠키 보안 설정 상수
    private static final boolean HTTP_ONLY = true;
    private static final boolean SECURE_IN_PRODUCTION = false; // 운영환경에서는 true로 변경 (HTTPS)
    private static final String COOKIE_PATH = "/";
    private static final int COOKIE_EXPIRE_IMMEDIATELY = 0;

    public void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie accessCookie = createCookie(ACCESS_TOKEN_NAME, accessToken, ACCESS_TOKEN_EXPIRE_TIME);
        response.addCookie(accessCookie);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = createCookie(REFRESH_TOKEN_NAME, refreshToken, REFRESH_TOKEN_EXPIRE_TIME);
        response.addCookie(refreshCookie);
    }

    public void clearTokenCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_NAME);
        clearCookie(response, REFRESH_TOKEN_NAME);
    }

    /**
     * 쿠키를 생성합니다.
     */
    private Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE_IN_PRODUCTION);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    /**
     * 쿠키를 삭제합니다 (MaxAge를 0으로 설정).
     */
    private void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = createCookie(cookieName, null, COOKIE_EXPIRE_IMMEDIATELY);
        response.addCookie(cookie);
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