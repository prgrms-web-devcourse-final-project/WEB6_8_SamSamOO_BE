package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.global.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenProvider {

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60; // 7일

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
    }

    public String generateAccessToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessToken().getExpirationSeconds() * 1000);

        return Jwts.builder()
                .setSubject(member.getLoginId())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("memberId", member.getMemberId())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Member member) {
        String refreshToken = UUID.randomUUID().toString();

        // Redis에 리프레시 토큰 저장
        String redisKey = REFRESH_TOKEN_PREFIX + member.getLoginId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME));

        return refreshToken;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 토큰: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT 서명이 잘못되었습니다: {}", e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.warn("토큰에서 사용자 정보 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateRefreshToken(String loginId, String refreshToken) {
        String redisKey = REFRESH_TOKEN_PREFIX + loginId;
        String storedToken = (String) redisTemplate.opsForValue().get(redisKey);
        return refreshToken.equals(storedToken);
    }

    public void deleteRefreshToken(String loginId) {
        String redisKey = REFRESH_TOKEN_PREFIX + loginId;
        redisTemplate.delete(redisKey);
    }

    public String findUsernameByRefreshToken(String refreshToken) {
        String pattern = REFRESH_TOKEN_PREFIX + "*";
        var keys = redisTemplate.keys(pattern);
        for (String key : keys) {
            String storedToken = (String) redisTemplate.opsForValue().get(key);
            if (refreshToken.equals(storedToken)) {
                return key.substring(REFRESH_TOKEN_PREFIX.length());
            }
        }
        return null;
    }
}