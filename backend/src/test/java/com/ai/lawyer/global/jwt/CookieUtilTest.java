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
import static org.mockito.ArgumentMatchers.eq;
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
        log.info("검증: 2개의 Set-Cookie 헤더가 추가되었는지 확인");
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        var setCookieHeaders = headerCaptor.getAllValues();
        assertThat(setCookieHeaders).hasSize(2);

        // 액세스 토큰 쿠키 검증
        String accessCookieHeader = setCookieHeaders.getFirst();
        assertThat(accessCookieHeader).contains(ACCESS_TOKEN_NAME + "=" + ACCESS_TOKEN);
        assertThat(accessCookieHeader).contains("HttpOnly");
        assertThat(accessCookieHeader).contains("Path=/");
        assertThat(accessCookieHeader).contains("Max-Age=300"); // 5분 = 300초
        assertThat(accessCookieHeader).contains("SameSite=Lax");
        log.info("액세스 토큰 쿠키 검증 완료: {}", accessCookieHeader);

        // 리프레시 토큰 쿠키 검증
        String refreshCookieHeader = setCookieHeaders.get(1);
        assertThat(refreshCookieHeader).contains(REFRESH_TOKEN_NAME + "=" + REFRESH_TOKEN);
        assertThat(refreshCookieHeader).contains("HttpOnly");
        assertThat(refreshCookieHeader).contains("Path=/");
        assertThat(refreshCookieHeader).contains("Max-Age=604800"); // 7일 = 604800초
        assertThat(refreshCookieHeader).contains("SameSite=Lax");
        log.info("리프레시 토큰 쿠키 검증 완료: {}", refreshCookieHeader);

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
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        String cookieHeader = headerCaptor.getValue();
        assertThat(cookieHeader).contains(ACCESS_TOKEN_NAME + "=" + ACCESS_TOKEN);
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("Max-Age=300");
        assertThat(cookieHeader).contains("SameSite=Lax");
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
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        String cookieHeader = headerCaptor.getValue();
        assertThat(cookieHeader).contains(REFRESH_TOKEN_NAME + "=" + REFRESH_TOKEN);
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("Max-Age=604800");
        assertThat(cookieHeader).contains("SameSite=Lax");
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
        log.info("검증: 2개의 Set-Cookie 헤더가 삭제용으로 추가되었는지 확인");
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        var setCookieHeaders = headerCaptor.getAllValues();
        assertThat(setCookieHeaders).hasSize(2);

        // 액세스 토큰 클리어 검증
        String accessClearHeader = setCookieHeaders.getFirst();
        assertThat(accessClearHeader).contains(ACCESS_TOKEN_NAME + "=");
        assertThat(accessClearHeader).contains("Max-Age=0");
        assertThat(accessClearHeader).contains("HttpOnly");
        assertThat(accessClearHeader).contains("Path=/");
        log.info("액세스 토큰 쿠키 클리어 검증 완료: {}", accessClearHeader);

        // 리프레시 토큰 클리어 검증
        String refreshClearHeader = setCookieHeaders.get(1);
        assertThat(refreshClearHeader).contains(REFRESH_TOKEN_NAME + "=");
        assertThat(refreshClearHeader).contains("Max-Age=0");
        assertThat(refreshClearHeader).contains("HttpOnly");
        assertThat(refreshClearHeader).contains("Path=/");
        log.info("리프레시 토큰 쿠키 클리어 검증 완료: {}", refreshClearHeader);

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
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        headerCaptor.getAllValues().forEach(header -> {
            assertThat(header).contains("HttpOnly");
            log.info("Set-Cookie 헤더: HttpOnly 포함 확인 - {}", header);
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
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        headerCaptor.getAllValues().forEach(header -> {
            assertThat(header).contains("Path=/");
            log.info("Set-Cookie 헤더: Path=/ 포함 확인 - {}", header);
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
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq("Set-Cookie"), headerCaptor.capture());

        var setCookieHeaders = headerCaptor.getAllValues();

        String accessHeader = setCookieHeaders.getFirst();
        assertThat(accessHeader).contains("Max-Age=300");
        log.info("액세스 토큰 만료 시간: 300초 (5분)");

        String refreshHeader = setCookieHeaders.get(1);
        assertThat(refreshHeader).contains("Max-Age=604800");
        log.info("리프레시 토큰 만료 시간: 604800초 (7일)");

        log.info("=== 토큰 만료 시간 테스트 완료 ===");
    }
}
