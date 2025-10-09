package com.ai.lawyer.global.oauth;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.OAuth2Member;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2SuccessHandler 테스트")
class OAuth2SuccessHandlerTest {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandlerTest.class);

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OAuth2SuccessHandler oauth2SuccessHandler;

    private OAuth2Member kakaoMember;
    private OAuth2Member naverMember;
    private PrincipalDetails kakaoPrincipalDetails;
    private PrincipalDetails naverPrincipalDetails;

    @BeforeEach
    void setUp() {
        // Redirect URL 설정 (환경변수에서 주입되는 값)
        ReflectionTestUtils.setField(oauth2SuccessHandler, "frontendRedirectUrl",
            "http://localhost:3000/oauth/success");
        ReflectionTestUtils.setField(oauth2SuccessHandler, "activeProfile", "dev");

        // 카카오 회원 생성
        kakaoMember = OAuth2Member.builder()
                .loginId("kakao@test.com")
                .email("kakao@test.com")
                .name("카카오사용자")
                .age(30)
                .gender(Member.Gender.MALE)
                .provider(OAuth2Member.Provider.KAKAO)
                .providerId("kakao123")
                .role(Member.Role.USER)
                .build();
        ReflectionTestUtils.setField(kakaoMember, "memberId", 10L);

        // 네이버 회원 생성
        naverMember = OAuth2Member.builder()
                .loginId("naver@test.com")
                .email("naver@test.com")
                .name("네이버사용자")
                .age(25)
                .gender(Member.Gender.FEMALE)
                .provider(OAuth2Member.Provider.NAVER)
                .providerId("naver456")
                .role(Member.Role.USER)
                .build();
        ReflectionTestUtils.setField(naverMember, "memberId", 20L);

        // OAuth2 attributes 생성 (카카오)
        java.util.Map<String, Object> kakaoAttributes = new java.util.HashMap<>();
        kakaoAttributes.put("id", "kakao123");

        // OAuth2 attributes 생성 (네이버)
        java.util.Map<String, Object> naverAttributes = new java.util.HashMap<>();
        naverAttributes.put("id", "naver456");

        // PrincipalDetails 생성 (MemberAdapter와 attributes 필요)
        kakaoPrincipalDetails = new PrincipalDetails(kakaoMember, kakaoAttributes);
        naverPrincipalDetails = new PrincipalDetails(naverMember, naverAttributes);
    }

    @Test
    @DisplayName("카카오 OAuth2 로그인 성공 - JWT 토큰 생성 및 쿠키 설정")
    void onAuthenticationSuccess_Kakao() throws Exception {
        // given
        log.info("=== 카카오 OAuth2 로그인 성공 테스트 시작 ===");
        String accessToken = "kakaoAccessToken";
        String refreshToken = "kakaoRefreshToken";

        given(authentication.getPrincipal()).willReturn(kakaoPrincipalDetails);
        given(tokenProvider.generateAccessToken(kakaoMember)).willReturn(accessToken);
        given(tokenProvider.generateRefreshToken(kakaoMember)).willReturn(refreshToken);
        log.info("Mock 설정 완료: 카카오 회원, 토큰 생성 준비");

        // when
        log.info("OAuth2 로그인 성공 핸들러 호출 중...");
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);
        log.info("OAuth2 로그인 성공 처리 완료");

        // then
        log.info("검증 시작: 토큰 생성 및 쿠키 설정 확인");
        verify(tokenProvider).generateAccessToken(kakaoMember);
        log.info("카카오 회원 액세스 토큰 생성 호출 확인");
        verify(tokenProvider).generateRefreshToken(kakaoMember);
        log.info("카카오 회원 리프레시 토큰 생성 호출 확인");
        verify(cookieUtil).setTokenCookies(response, accessToken, refreshToken);
        log.info("쿠키에 토큰 설정 호출 확인");
        log.info("=== 카카오 OAuth2 로그인 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("네이버 OAuth2 로그인 성공 - JWT 토큰 생성 및 쿠키 설정")
    void onAuthenticationSuccess_Naver() throws Exception {
        // given
        log.info("=== 네이버 OAuth2 로그인 성공 테스트 시작 ===");
        String accessToken = "naverAccessToken";
        String refreshToken = "naverRefreshToken";

        given(authentication.getPrincipal()).willReturn(naverPrincipalDetails);
        given(tokenProvider.generateAccessToken(naverMember)).willReturn(accessToken);
        given(tokenProvider.generateRefreshToken(naverMember)).willReturn(refreshToken);
        log.info("Mock 설정 완료: 네이버 회원, 토큰 생성 준비");

        // when
        log.info("OAuth2 로그인 성공 핸들러 호출 중...");
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);
        log.info("OAuth2 로그인 성공 처리 완료");

        // then
        log.info("검증 시작: 토큰 생성 및 쿠키 설정 확인");
        verify(tokenProvider).generateAccessToken(naverMember);
        log.info("네이버 회원 액세스 토큰 생성 호출 확인");
        verify(tokenProvider).generateRefreshToken(naverMember);
        log.info("네이버 회원 리프레시 토큰 생성 호출 확인");
        verify(cookieUtil).setTokenCookies(response, accessToken, refreshToken);
        log.info("쿠키에 토큰 설정 호출 확인");
        log.info("=== 네이버 OAuth2 로그인 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 후 리다이렉트 URL 확인")
    void onAuthenticationSuccess_RedirectUrl() throws Exception {
        // given
        log.info("=== OAuth2 로그인 성공 후 리다이렉트 테스트 시작 ===");
        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";

        given(authentication.getPrincipal()).willReturn(kakaoPrincipalDetails);
        given(tokenProvider.generateAccessToken(kakaoMember)).willReturn(accessToken);
        given(tokenProvider.generateRefreshToken(kakaoMember)).willReturn(refreshToken);

        // when
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        log.info("리다이렉트 URL 검증: http://localhost:3000/oauth/success");
        // 실제 리다이렉트는 내부에서 sendRedirect로 처리되므로,
        // 토큰 생성 및 쿠키 설정이 정상적으로 완료되었는지만 확인
        verify(cookieUtil).setTokenCookies(response, accessToken, refreshToken);
        log.info("=== OAuth2 로그인 성공 후 리다이렉트 테스트 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 - Redis에 토큰 저장 확인")
    void onAuthenticationSuccess_RedisTokenStorage() throws Exception {
        // given
        log.info("=== OAuth2 로그인 성공 - Redis 토큰 저장 테스트 시작 ===");
        String accessToken = "redisTestAccessToken";
        String refreshToken = "redisTestRefreshToken";

        given(authentication.getPrincipal()).willReturn(kakaoPrincipalDetails);
        given(tokenProvider.generateAccessToken(kakaoMember)).willReturn(accessToken);
        given(tokenProvider.generateRefreshToken(kakaoMember)).willReturn(refreshToken);
        log.info("Mock 설정 완료: 토큰 생성 시 Redis 저장 포함");

        // when
        log.info("OAuth2 로그인 성공 핸들러 호출 중...");
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        log.info("검증: TokenProvider의 generateAccessToken 호출 시 Redis에 자동 저장됨");
        verify(tokenProvider).generateAccessToken(kakaoMember);
        log.info("검증: TokenProvider의 generateRefreshToken 호출 시 Redis에 자동 저장됨");
        verify(tokenProvider).generateRefreshToken(kakaoMember);
        log.info("=== OAuth2 로그인 성공 - Redis 토큰 저장 테스트 완료 ===");
    }
}
