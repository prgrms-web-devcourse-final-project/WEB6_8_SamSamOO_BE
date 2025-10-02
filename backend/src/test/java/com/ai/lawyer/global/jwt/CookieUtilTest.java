package com.ai.lawyer.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookieUtil 테스트")
class CookieUtilTest {

    private static final Logger log = LoggerFactory.getLogger(CookieUtilTest.class);

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CookieUtil cookieUtil;

    private static final String ACCESS_TOKEN = "testAccessToken";
    private static final String REFRESH_TOKEN = "testRefreshToken";
    private static final String ACCESS_TOKEN_NAME = "accessToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";

    @BeforeEach
    void setUp() {
        log.info("=== 테스트 초기화 ===");
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰을 쿠키에 설정")
    void setTokenCookies_Success() {
        // given
        log.info("=== 토큰 쿠키 설정 테스트 시작 ===");
        log.info("액세스 토큰: {}, 리프레시 토큰: {}", ACCESS_TOKEN, REFRESH_TOKEN);

        // when
        log.info("쿠키 설정 호출 중...");
        cookieUtil.setTokenCookies(response, ACCESS_TOKEN, REFRESH_TOKEN);
        log.info("쿠키 설정 완료");

        // then
        log.info("검증: 2개의 쿠키(액세스, 리프레시)가 추가되었는지 확인");
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        var cookies = cookieCaptor.getAllValues();
        assertThat(cookies).hasSize(2);

        // 액세스 토큰 쿠키 검증
        Cookie accessCookie = cookies.getFirst();
        assertThat(accessCookie.getName()).isEqualTo(ACCESS_TOKEN_NAME);
        assertThat(accessCookie.getValue()).isEqualTo(ACCESS_TOKEN);
        assertThat(accessCookie.isHttpOnly()).isTrue();
        assertThat(accessCookie.getPath()).isEqualTo("/");
        assertThat(accessCookie.getMaxAge()).isEqualTo(5 * 60); // 5분
        log.info("액세스 토큰 쿠키 검증 완료: name={}, maxAge={}", accessCookie.getName(), accessCookie.getMaxAge());

        // 리프레시 토큰 쿠키 검증
        Cookie refreshCookie = cookies.get(1);
        assertThat(refreshCookie.getName()).isEqualTo(REFRESH_TOKEN_NAME);
        assertThat(refreshCookie.getValue()).isEqualTo(REFRESH_TOKEN);
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getPath()).isEqualTo("/");
        assertThat(refreshCookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60); // 7일
        log.info("리프레시 토큰 쿠키 검증 완료: name={}, maxAge={}", refreshCookie.getName(), refreshCookie.getMaxAge());

        log.info("=== 토큰 쿠키 설정 테스트 완료 ===");
    }

    @Test
    @DisplayName("액세스 토큰 단독 쿠키 설정")
    void setAccessTokenCookie_Success() {
        // given
        log.info("=== 액세스 토큰 단독 쿠키 설정 테스트 시작 ===");

        // when
        cookieUtil.setAccessTokenCookie(response, ACCESS_TOKEN);

        // then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo(ACCESS_TOKEN_NAME);
        assertThat(cookie.getValue()).isEqualTo(ACCESS_TOKEN);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(5 * 60);
        log.info("=== 액세스 토큰 단독 쿠키 설정 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰 단독 쿠키 설정")
    void setRefreshTokenCookie_Success() {
        // given
        log.info("=== 리프레시 토큰 단독 쿠키 설정 테스트 시작 ===");

        // when
        cookieUtil.setRefreshTokenCookie(response, REFRESH_TOKEN);

        // then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo(REFRESH_TOKEN_NAME);
        assertThat(cookie.getValue()).isEqualTo(REFRESH_TOKEN);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
        log.info("=== 리프레시 토큰 단독 쿠키 설정 테스트 완료 ===");
    }

    @Test
    @DisplayName("요청에서 액세스 토큰 쿠키 읽기 성공")
    void getAccessTokenFromCookies_Success() {
        // given
        log.info("=== 액세스 토큰 쿠키 읽기 테스트 시작 ===");
        Cookie accessCookie = new Cookie(ACCESS_TOKEN_NAME, ACCESS_TOKEN);
        Cookie[] cookies = {accessCookie};
        given(request.getCookies()).willReturn(cookies);

        // when
        String token = cookieUtil.getAccessTokenFromCookies(request);

        // then
        assertThat(token).isEqualTo(ACCESS_TOKEN);
        log.info("읽은 액세스 토큰: {}", token);
        log.info("=== 액세스 토큰 쿠키 읽기 테스트 완료 ===");
    }

    @Test
    @DisplayName("요청에서 리프레시 토큰 쿠키 읽기 성공")
    void getRefreshTokenFromCookies_Success() {
        // given
        log.info("=== 리프레시 토큰 쿠키 읽기 테스트 시작 ===");
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_NAME, REFRESH_TOKEN);
        Cookie[] cookies = {refreshCookie};
        given(request.getCookies()).willReturn(cookies);

        // when
        String token = cookieUtil.getRefreshTokenFromCookies(request);

        // then
        assertThat(token).isEqualTo(REFRESH_TOKEN);
        log.info("읽은 리프레시 토큰: {}", token);
        log.info("=== 리프레시 토큰 쿠키 읽기 테스트 완료 ===");
    }

    @Test
    @DisplayName("여러 쿠키 중에서 액세스 토큰 찾기")
    void getAccessTokenFromCookies_MultipleCookies() {
        // given
        log.info("=== 여러 쿠키 중 액세스 토큰 찾기 테스트 시작 ===");
        Cookie[] cookies = {
            new Cookie("otherCookie1", "value1"),
            new Cookie(ACCESS_TOKEN_NAME, ACCESS_TOKEN),
            new Cookie("otherCookie2", "value2")
        };
        given(request.getCookies()).willReturn(cookies);

        // when
        String token = cookieUtil.getAccessTokenFromCookies(request);

        // then
        assertThat(token).isEqualTo(ACCESS_TOKEN);
        log.info("여러 쿠키 중에서 액세스 토큰 찾기 성공");
        log.info("=== 여러 쿠키 중 액세스 토큰 찾기 테스트 완료 ===");
    }

    @Test
    @DisplayName("쿠키가 없을 때 null 반환")
    void getAccessTokenFromCookies_NoCookies() {
        // given
        log.info("=== 쿠키 없음 테스트 시작 ===");
        given(request.getCookies()).willReturn(null);

        // when
        String token = cookieUtil.getAccessTokenFromCookies(request);

        // then
        assertThat(token).isNull();
        log.info("쿠키 없을 때 null 반환 확인");
        log.info("=== 쿠키 없음 테스트 완료 ===");
    }

    @Test
    @DisplayName("찾는 쿠키가 없을 때 null 반환")
    void getAccessTokenFromCookies_TokenNotFound() {
        // given
        log.info("=== 토큰 쿠키 없음 테스트 시작 ===");
        Cookie[] cookies = {
            new Cookie("otherCookie1", "value1"),
            new Cookie("otherCookie2", "value2")
        };
        given(request.getCookies()).willReturn(cookies);

        // when
        String token = cookieUtil.getAccessTokenFromCookies(request);

        // then
        assertThat(token).isNull();
        log.info("액세스 토큰 쿠키 없을 때 null 반환 확인");
        log.info("=== 토큰 쿠키 없음 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 쿠키 클리어 - MaxAge 0으로 설정")
    void clearTokenCookies_Success() {
        // given
        log.info("=== 토큰 쿠키 클리어 테스트 시작 ===");

        // when
        log.info("쿠키 클리어 호출 중...");
        cookieUtil.clearTokenCookies(response);
        log.info("쿠키 클리어 완료");

        // then
        log.info("검증: 2개의 쿠키(액세스, 리프레시)가 삭제용으로 추가되었는지 확인");
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        var cookies = cookieCaptor.getAllValues();
        assertThat(cookies).hasSize(2);

        // 액세스 토큰 클리어 검증
        Cookie accessClearCookie = cookies.getFirst();
        assertThat(accessClearCookie.getName()).isEqualTo(ACCESS_TOKEN_NAME);
        assertThat(accessClearCookie.getValue()).isNull();
        assertThat(accessClearCookie.getMaxAge()).isEqualTo(0);
        assertThat(accessClearCookie.isHttpOnly()).isTrue();
        assertThat(accessClearCookie.getPath()).isEqualTo("/");
        log.info("액세스 토큰 쿠키 클리어 검증 완료: maxAge={}", accessClearCookie.getMaxAge());

        // 리프레시 토큰 클리어 검증
        Cookie refreshClearCookie = cookies.get(1);
        assertThat(refreshClearCookie.getName()).isEqualTo(REFRESH_TOKEN_NAME);
        assertThat(refreshClearCookie.getValue()).isNull();
        assertThat(refreshClearCookie.getMaxAge()).isEqualTo(0);
        assertThat(refreshClearCookie.isHttpOnly()).isTrue();
        assertThat(refreshClearCookie.getPath()).isEqualTo("/");
        log.info("리프레시 토큰 쿠키 클리어 검증 완료: maxAge={}", refreshClearCookie.getMaxAge());

        log.info("=== 토큰 쿠키 클리어 테스트 완료 ===");
    }

    @Test
    @DisplayName("HttpOnly 속성 확인 - XSS 공격 방어")
    void cookieHttpOnlyAttribute_Security() {
        // given
        log.info("=== HttpOnly 속성 보안 테스트 시작 ===");
        log.info("HttpOnly 속성: JavaScript에서 쿠키 접근 차단, XSS 공격 방어");

        // when
        cookieUtil.setTokenCookies(response, ACCESS_TOKEN, REFRESH_TOKEN);

        // then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        cookieCaptor.getAllValues().forEach(cookie -> {
            assertThat(cookie.isHttpOnly()).isTrue();
            log.info("쿠키 {}: HttpOnly=true (보안 설정 확인)", cookie.getName());
        });

        log.info("=== HttpOnly 속성 보안 테스트 완료 ===");
    }

    @Test
    @DisplayName("Path 속성 확인 - 모든 경로에서 쿠키 접근 가능")
    void cookiePathAttribute_Accessibility() {
        // given
        log.info("=== Path 속성 테스트 시작 ===");
        log.info("Path=/: 모든 경로에서 쿠키 접근 가능");

        // when
        cookieUtil.setTokenCookies(response, ACCESS_TOKEN, REFRESH_TOKEN);

        // then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        cookieCaptor.getAllValues().forEach(cookie -> {
            assertThat(cookie.getPath()).isEqualTo("/");
            log.info("쿠키 {}: Path=/ (모든 경로 접근 가능)", cookie.getName());
        });

        log.info("=== Path 속성 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 만료 시간 확인 - 액세스 5분, 리프레시 7일")
    void cookieMaxAgeAttribute_ExpiryTime() {
        // given
        log.info("=== 토큰 만료 시간 테스트 시작 ===");
        log.info("액세스 토큰 만료: 5분 (300초)");
        log.info("리프레시 토큰 만료: 7일 (604800초)");

        // when
        cookieUtil.setTokenCookies(response, ACCESS_TOKEN, REFRESH_TOKEN);

        // then
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());

        var cookies = cookieCaptor.getAllValues();

        Cookie accessCookie = cookies.getFirst();
        assertThat(accessCookie.getMaxAge()).isEqualTo(5 * 60);
        log.info("액세스 토큰 만료 시간: {}초 (5분)", accessCookie.getMaxAge());

        Cookie refreshCookie = cookies.get(1);
        assertThat(refreshCookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
        log.info("리프레시 토큰 만료 시간: {}초 (7일)", refreshCookie.getMaxAge());

        log.info("=== 토큰 만료 시간 테스트 완료 ===");
    }
}
