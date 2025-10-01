package com.ai.lawyer.global.oauth;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final CookieUtil cookieUtil;

    @Value("${custom.oauth2.redirect-url:http://localhost:3000/oauth/callback}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        com.ai.lawyer.domain.member.entity.MemberAdapter member = principalDetails.getMember();

        log.info("OAuth2 로그인 성공: memberId={}, email={}",
                member.getMemberId(), member.getLoginId());

        // JWT 토큰 생성 (Redis 저장 포함)
        String accessToken = tokenProvider.generateAccessToken(member);
        String refreshToken = tokenProvider.generateRefreshToken(member);

        // 쿠키에 토큰 설정
        cookieUtil.setTokenCookies(response, accessToken, refreshToken);

        // 프론트엔드로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("success", "true")
                .build()
                .toUriString();

        log.info("OAuth2 로그인 완료, 리다이렉트: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
