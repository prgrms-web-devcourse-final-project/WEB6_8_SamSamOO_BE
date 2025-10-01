package com.ai.lawyer.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String ACCESS_TOKEN_NAME = "accessToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";
    private static final int ACCESS_TOKEN_EXPIRE_TIME = 10; // 5분
    private static final int REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60; // 7일

    public void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);
    }

    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_NAME, accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // 운영환경에서는 true로 변경 (HTTPS)
        accessCookie.setPath("/");
        accessCookie.setMaxAge(ACCESS_TOKEN_EXPIRE_TIME);
        response.addCookie(accessCookie);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_NAME, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // 운영환경에서는 true로 변경 (HTTPS)
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(REFRESH_TOKEN_EXPIRE_TIME);
        response.addCookie(refreshCookie);
    }

    public void clearTokenCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_NAME);
        clearCookie(response, REFRESH_TOKEN_NAME);
    }

    private void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
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