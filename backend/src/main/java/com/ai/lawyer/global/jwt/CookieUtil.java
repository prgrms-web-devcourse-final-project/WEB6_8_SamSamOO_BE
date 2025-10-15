package com.ai.lawyer.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class CookieUtil {

    // 쿠키 이름 상수
    private static final String ACCESS_TOKEN_NAME = "accessToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";

    // 쿠키 만료 시간 상수 (초 단위)
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final int ACCESS_TOKEN_EXPIRE_TIME = 60 * 60; // 5분 (300초)
    private static final int REFRESH_TOKEN_EXPIRE_TIME = 7 * HOURS_PER_DAY * MINUTES_PER_HOUR * 60; // 7일

    // 쿠키 보안 설정 상수
    private static final boolean HTTP_ONLY = true;
    private static final String COOKIE_PATH = "/";
    private static final int COOKIE_EXPIRE_IMMEDIATELY = 0;

    @Value("${custom.cookie.domain:}")
    private String cookieDomain;

    @Value("${custom.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${custom.cookie.same-site:Lax}")
    private String cookieSameSite;

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
        log.info("=== 쿠키 생성 중: name={}, domain='{}', secure={}, sameSite={}",
                name, cookieDomain, cookieSecure, cookieSameSite);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(HTTP_ONLY)
                .secure(cookieSecure)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(maxAge))
                .sameSite(cookieSameSite);

        // 도메인이 설정되어 있으면 추가
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            log.info("쿠키 도메인 설정: {}", cookieDomain);
            builder.domain(cookieDomain);
        } else {
            log.info("쿠키 도메인 설정 안 함 (빈 값 또는 null)");
        }

        ResponseCookie cookie = builder.build();
        log.info("생성된 쿠키: {}", cookie);
        return cookie;
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