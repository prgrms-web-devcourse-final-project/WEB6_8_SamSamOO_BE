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
import java.nio.charset.StandardCharsets;
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
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessToken().getExpirationSeconds() * 1000);

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("loginid", member.getLoginId())
                .claim("memberId", member.getMemberId())
                .claim("role", member.getRole().name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(Member member) {
        String refreshToken = UUID.randomUUID().toString();

        // Redis에 리프레시 토큰 저장 (만료시간: 7일)
        String redisKey = REFRESH_TOKEN_PREFIX + member.getLoginId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME));

        return refreshToken;
    }

    /**
     * 토큰의 상태를 확인합니다.
     * @param token JWT 토큰
     * @return TokenValidationResult (유효, 만료, 오류)
     */
    public TokenValidationResult validateTokenWithResult(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
            return TokenValidationResult.EXPIRED;
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException | SecurityException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return TokenValidationResult.INVALID;
        }
    }

    public enum TokenValidationResult {
        VALID,      // 유효한 토큰
        EXPIRED,    // 만료된 토큰
        INVALID     // 잘못된 토큰
    }

    public Long getMemberIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("memberId", Long.class);
        } catch (Exception e) {
            log.warn("토큰에서 회원 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.warn("토큰에서 역할 정보 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public String getLoginIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("loginid", String.class); // loginid claim에서 추출
        } catch (Exception e) {
            log.warn("토큰에서 로그인 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 만료된 토큰에서도 loginId를 추출합니다.
     * @param token JWT 토큰 (만료되어도 괜찮음)
     * @return loginId 또는 null
     */
    public String getLoginIdFromExpiredToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("loginid", String.class);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이지만 claim은 추출 가능
            return e.getClaims().get("loginid", String.class);
        } catch (Exception e) {
            log.warn("만료된 토큰에서 로그인 ID 추출 실패: {}", e.getMessage());
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

    /**
     * 리프레시 토큰으로 사용자명을 찾습니다.
     * Redis에서 모든 리프레시 토큰 키를 순회하며 일치하는 토큰을 찾습니다.
     * @param refreshToken 찾을 리프레시 토큰
     * @return 사용자명 또는 null
     */
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