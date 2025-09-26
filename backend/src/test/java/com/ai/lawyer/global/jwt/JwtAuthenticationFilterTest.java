package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private Member testMember;
    private String validAccessToken;
    private String expiredAccessToken;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

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
    @DisplayName("유효한 Authorization 헤더 토큰으로 인증 성공")
    void doFilterInternal_ValidHeaderToken_Success() throws Exception {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + validAccessToken);
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

        given(request.getHeader("Authorization")).willReturn(null);
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);

        // 자동 리프레시 관련
        given(tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken)).willReturn("test@example.com");
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(refreshToken);
        given(tokenProvider.validateRefreshToken("test@example.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("test@example.com")).willReturn(Optional.of(testMember));
        given(tokenProvider.generateAccessToken(testMember)).willReturn(newAccessToken);
        given(tokenProvider.generateRefreshToken(testMember)).willReturn(newRefreshToken);

        // 새 토큰으로 인증 설정
        given(tokenProvider.getMemberIdFromToken(newAccessToken)).willReturn(1L);
        given(tokenProvider.getRoleFromToken(newAccessToken)).willReturn("USER");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider).deleteRefreshToken("test@example.com");
        verify(cookieUtil).setTokenCookies(response, newAccessToken, newRefreshToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("만료된 Authorization 헤더 토큰은 자동 리프레시하지 않음")
    void doFilterInternal_ExpiredHeaderToken_NoAutoRefresh() throws Exception {
        // given
        given(request.getHeader("Authorization")).willReturn("Bearer " + expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider, never()).getLoginIdFromExpiredToken(anyString());
        verify(cookieUtil, never()).getRefreshTokenFromCookies(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 자동 갱신 실패")
    void doFilterInternal_NoRefreshToken_AutoRefreshFail() throws Exception {
        // given
        given(request.getHeader("Authorization")).willReturn(null);
        given(cookieUtil.getAccessTokenFromCookies(request)).willReturn(expiredAccessToken);
        given(tokenProvider.validateTokenWithResult(expiredAccessToken))
                .willReturn(TokenProvider.TokenValidationResult.EXPIRED);
        given(tokenProvider.getLoginIdFromExpiredToken(expiredAccessToken)).willReturn("test@example.com");
        given(cookieUtil.getRefreshTokenFromCookies(request)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(tokenProvider, never()).validateRefreshToken(anyString(), anyString());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("잘못된 토큰으로 인증 실패")
    void doFilterInternal_InvalidToken_AuthFail() throws Exception {
        // given
        String invalidToken = "invalidToken";
        given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
        given(tokenProvider.validateTokenWithResult(invalidToken))
                .willReturn(TokenProvider.TokenValidationResult.INVALID);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}