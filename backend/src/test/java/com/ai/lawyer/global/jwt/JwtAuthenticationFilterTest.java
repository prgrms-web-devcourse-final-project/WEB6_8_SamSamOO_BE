package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.OAuth2Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuth2MemberRepository oauth2MemberRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Member testMember;
    private String validAccessToken;
    private String expiredAccessToken;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        // JwtAuthenticationFilter 생성
        jwtAuthenticationFilter = new JwtAuthenticationFilter();
        jwtAuthenticationFilter.setTokenProvider(tokenProvider);
        jwtAuthenticationFilter.setCookieUtil(cookieUtil);
        jwtAuthenticationFilter.setMemberRepository(memberRepository);
        jwtAuthenticationFilter.setOauth2MemberRepository(oauth2MemberRepository);

        testMember = Member.builder()
                .loginId("test@example.com")
                .password("encodedPassword")
                .name("Test User")
                .age(25)
                .gender(Member.Gender.MALE)
                .role(Member.Role.USER)
                .build();

        validAccessToken = "validAccessToken";
        expiredAccessToken = "expiredAccessToken";
        refreshToken = "refreshToken";
    }

    @Test
    @DisplayName("유효한 쿠키 토큰으로 인증 성공")
    void doFilterInternal_ValidCookieToken_Success() throws Exception {
        // given
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(validAccessToken);
        given(tokenProvider.validateTokenWithResult(validAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.VALID);
        given(tokenProvider.getMemberIdFromToken(validAccessToken)).willReturn(1L);
        given(tokenProvider.getRoleFromToken(validAccessToken)).willReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("만료된 쿠키 토큰으로 자동 리프레시 성공")
    void doFilterInternal_ExpiredCookieToken_AutoRefreshSuccess() throws Exception {
        // given
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);

        // 자동 리프레시 관련
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(refreshToken);
        given(tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken)).willReturn("test@example.com");
        given(tokenProvider.validateRefreshToken("test@example.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("test@example.com")).willReturn(Optional.of(testMember));
        given(oauth2MemberRepository.findByLoginId("test@example.com")).willReturn(Optional.empty());
        given(tokenProvider.generateAccessToken(testMember)).willReturn(newAccessToken);
        given(tokenProvider.generateRefreshToken(testMember)).willReturn(newRefreshToken);

        // 새 토큰으로 인증 설정
        given(tokenProvider.getMemberIdFromToken(newAccessToken)).willReturn(1L);
        given(tokenProvider.getRoleFromToken(newAccessToken)).willReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider).deleteAllTokens("test@example.com");
        verify(cookieUtil).setTokenCookies(response, newAccessToken, newRefreshToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 쿠키 클리어")
    void doFilterInternal_NoRefreshToken_ClearCookies() throws Exception {
        // given
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(cookieUtil).clearTokenCookies(response);
        verify(tokenProvider, never()).validateRefreshToken(anyString(), anyString());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("액세스 토큰이 없으면 리프레시 토큰 확인")
    void doFilterInternal_NoAccessToken_CheckRefreshToken() throws Exception {
        // given
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(null);
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(refreshToken);
        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn("test@example.com");
        given(tokenProvider.validateRefreshToken("test@example.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("test@example.com")).willReturn(Optional.of(testMember));
        given(oauth2MemberRepository.findByLoginId("test@example.com")).willReturn(Optional.empty());
        given(tokenProvider.generateAccessToken(testMember)).willReturn(newAccessToken);
        given(tokenProvider.generateRefreshToken(testMember)).willReturn(newRefreshToken);

        // 새 토큰으로 인증 설정
        given(tokenProvider.getMemberIdFromToken(newAccessToken)).willReturn(1L);
        given(tokenProvider.getRoleFromToken(newAccessToken)).willReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider).deleteAllTokens("test@example.com");
        verify(cookieUtil).setTokenCookies(response, newAccessToken, newRefreshToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 쿠키 토큰으로 리프레시 시도")
    void doFilterInternal_InvalidCookieToken_TryRefresh() throws Exception {
        // given
        String invalidToken = "invalidToken";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(invalidToken);
        given(tokenProvider.validateTokenWithResult(invalidToken))
                .willReturn(TokenProvider.TokenValidationResult.INVALID);
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(refreshToken);
        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn("test@example.com");
        given(tokenProvider.validateRefreshToken("test@example.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("test@example.com")).willReturn(Optional.of(testMember));
        given(oauth2MemberRepository.findByLoginId("test@example.com")).willReturn(Optional.empty());
        given(tokenProvider.generateAccessToken(testMember)).willReturn(newAccessToken);
        given(tokenProvider.generateRefreshToken(testMember)).willReturn(newRefreshToken);

        // 새 토큰으로 인증 설정
        given(tokenProvider.getMemberIdFromToken(newAccessToken)).willReturn(1L);
        given(tokenProvider.getRoleFromToken(newAccessToken)).willReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider).deleteAllTokens("test@example.com");
        verify(cookieUtil).setTokenCookies(response, newAccessToken, newRefreshToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("모든 토큰이 없으면 쿠키 클리어")
    void doFilterInternal_NoTokens_ClearCookies() throws Exception {
        // given
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(null);
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(cookieUtil).clearTokenCookies(response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("OAuth2 회원 - 만료된 토큰으로 자동 리프레시 성공 (카카오)")
    void doFilterInternal_OAuth2Member_KakaoRefresh() throws Exception {
        // given
        OAuth2Member kakaoMember = OAuth2Member.builder()
                .loginId("kakao@test.com")
                .email("kakao@test.com")
                .name("카카오사용자")
                .age(30)
                .gender(Member.Gender.MALE)
                .provider(OAuth2Member.Provider.KAKAO)
                .providerId("kakao123")
                .role(Member.Role.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(kakaoMember, "memberId", 10L);

        String newAccessToken = "newOAuth2AccessToken";
        String newRefreshToken = "newOAuth2RefreshToken";

        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(refreshToken);
        given(tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken)).willReturn("kakao@test.com");
        given(tokenProvider.validateRefreshToken("kakao@test.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("kakao@test.com")).willReturn(Optional.empty());
        given(oauth2MemberRepository.findByLoginId("kakao@test.com")).willReturn(Optional.of(kakaoMember));
        given(tokenProvider.generateAccessToken(kakaoMember)).willReturn(newAccessToken);
        given(tokenProvider.generateRefreshToken(kakaoMember)).willReturn(newRefreshToken);
        given(tokenProvider.getMemberIdFromToken(newAccessToken)).willReturn(10L);
        given(tokenProvider.getRoleFromToken(newAccessToken)).willReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(memberRepository).findByLoginId("kakao@test.com");
        verify(oauth2MemberRepository).findByLoginId("kakao@test.com");
        verify(tokenProvider).deleteAllTokens("kakao@test.com");
        verify(tokenProvider).generateAccessToken(kakaoMember);
        verify(tokenProvider).generateRefreshToken(kakaoMember);
        verify(cookieUtil).setTokenCookies(response, newAccessToken, newRefreshToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(10L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("OAuth2 회원 - 액세스 토큰 없이 리프레시 토큰으로만 갱신 성공 (네이버)")
    void doFilterInternal_OAuth2Member_NaverRefreshOnly() throws Exception {
        // given
        OAuth2Member naverMember = OAuth2Member.builder()
                .loginId("naver@test.com")
                .email("naver@test.com")
                .name("네이버사용자")
                .age(25)
                .gender(Member.Gender.FEMALE)
                .provider(OAuth2Member.Provider.NAVER)
                .providerId("naver456")
                .role(Member.Role.USER)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(naverMember, "memberId", 20L);

        String newAccessToken = "newNaverAccessToken";
        String newRefreshToken = "newNaverRefreshToken";

        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(null);
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(refreshToken);
        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn("naver@test.com");
        given(tokenProvider.validateRefreshToken("naver@test.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("naver@test.com")).willReturn(Optional.empty());
        given(oauth2MemberRepository.findByLoginId("naver@test.com")).willReturn(Optional.of(naverMember));
        given(tokenProvider.generateAccessToken(naverMember)).willReturn(newAccessToken);
        given(tokenProvider.generateRefreshToken(naverMember)).willReturn(newRefreshToken);
        given(tokenProvider.getMemberIdFromToken(newAccessToken)).willReturn(20L);
        given(tokenProvider.getRoleFromToken(newAccessToken)).willReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(memberRepository).findByLoginId("naver@test.com");
        verify(oauth2MemberRepository).findByLoginId("naver@test.com");
        verify(tokenProvider).deleteAllTokens("naver@test.com");
        verify(cookieUtil).setTokenCookies(response, newAccessToken, newRefreshToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(20L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("OAuth2 회원 - 유효하지 않은 리프레시 토큰으로 쿠키 클리어")
    void doFilterInternal_OAuth2Member_InvalidRefreshToken() throws Exception {
        // given
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(refreshToken);
        given(tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken)).willReturn("kakao@test.com");
        given(tokenProvider.validateRefreshToken("kakao@test.com", refreshToken)).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider).validateRefreshToken("kakao@test.com", refreshToken);
        verify(cookieUtil).clearTokenCookies(response);
        verify(memberRepository, never()).findByLoginId(anyString());
        verify(oauth2MemberRepository, never()).findByLoginId(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}