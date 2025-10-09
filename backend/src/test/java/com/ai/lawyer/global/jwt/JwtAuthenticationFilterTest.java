package com.ai.lawyer.global.jwt;

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

    private String validAccessToken;
    private String expiredAccessToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        // JwtAuthenticationFilter 생성
        jwtAuthenticationFilter = new JwtAuthenticationFilter();
        jwtAuthenticationFilter.setTokenProvider(tokenProvider);
        jwtAuthenticationFilter.setCookieUtil(cookieUtil);
        jwtAuthenticationFilter.setMemberRepository(memberRepository);
        jwtAuthenticationFilter.setOauth2MemberRepository(oauth2MemberRepository);

        validAccessToken = "validAccessToken";
        expiredAccessToken = "expiredAccessToken";
    }

    @Test
    @DisplayName("유효한 쿠키 토큰으로 인증 성공")
    void doFilterInternal_ValidCookieToken_Success() throws Exception {
        // given
        given(request.getRequestURI()).willReturn("/api/polls/1");
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(validAccessToken);
        given(tokenProvider.validateTokenWithResult(validAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.VALID);
        given(tokenProvider.getMemberIdFromToken(validAccessToken)).willReturn(1L);
        given(tokenProvider.getRoleFromToken(validAccessToken)).willReturn("USER");
        given(response.getWriter()).willReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("만료된 쿠키 토큰으로 401 에러 반환")
    void doFilterInternal_ExpiredCookieToken_Returns401() throws Exception {
        // given
        given(request.getRequestURI()).willReturn("/api/polls/1");
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);
        given(response.getWriter()).willReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        verify(tokenProvider, never()).deleteAllTokens(anyString());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 쿠키 토큰으로 401 에러 반환")
    void doFilterInternal_InvalidCookieToken_Returns401() throws Exception {
        // given
        String invalidToken = "invalidToken";
        given(request.getRequestURI()).willReturn("/api/polls/1");
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(invalidToken);
        given(tokenProvider.validateTokenWithResult(invalidToken))
                .willReturn(TokenProvider.TokenValidationResult.INVALID);
        given(response.getWriter()).willReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(tokenProvider, never()).deleteAllTokens(anyString());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("액세스 토큰이 없으면 인증 없이 통과")
    void doFilterInternal_NoAccessToken_PassWithoutAuth() throws Exception {
        // given
        given(request.getRequestURI()).willReturn("/api/polls/1");
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(null);
        given(response.getWriter()).willReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider, never()).validateTokenWithResult(anyString());
        verify(tokenProvider, never()).deleteAllTokens(anyString());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

}
