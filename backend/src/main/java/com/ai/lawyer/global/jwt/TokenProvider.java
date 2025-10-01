package com.ai.lawyer.global.jwt;

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

    private static final String TOKEN_PREFIX = "tokens:";
    private static final String ACCESS_TOKEN_FIELD = "accessToken";
    private static final String ACCESS_TOKEN_EXPIRY_FIELD = "accessTokenExpiry";
    private static final String REFRESH_TOKEN_FIELD = "refreshToken";
    private static final String REFRESH_TOKEN_EXPIRY_FIELD = "refreshTokenExpiry";
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60; // 7일

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(com.ai.lawyer.domain.member.entity.MemberAdapter member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessToken().getExpirationSeconds() * 1000);

        String accessToken = Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("loginid", member.getLoginId())
                .claim("memberId", member.getMemberId())
                .claim("role", member.getRole().name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Redis Hash에 액세스 토큰 정보 저장
        try {
            String loginId = member.getLoginId();
            String tokenKey = TOKEN_PREFIX + loginId;

            // Hash에 액세스 토큰과 만료시점 저장
            redisTemplate.opsForHash().put(tokenKey, ACCESS_TOKEN_FIELD, accessToken);
            redisTemplate.opsForHash().put(tokenKey, ACCESS_TOKEN_EXPIRY_FIELD, String.valueOf(expiry.getTime()));

            // 전체 Hash에 TTL 설정 (리프레시 토큰 만료시간으로 설정)
            redisTemplate.expire(tokenKey, Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME));

            log.info("=== Access token Hash 저장 성공: key={}, expiry={} ===", tokenKey, expiry);
        } catch (Exception e) {
            log.error("=== Access token Hash 저장 실패: {} ===", e.getMessage(), e);
        }

        return accessToken;
    }

    public String generateRefreshToken(com.ai.lawyer.domain.member.entity.MemberAdapter member) {
        String refreshToken = UUID.randomUUID().toString();
        Date now = new Date();
        Date refreshExpiry = new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME * 1000);

        // Redis Hash에 리프레시 토큰 정보 저장
        try {
            String loginId = member.getLoginId();
            String tokenKey = TOKEN_PREFIX + loginId;

            // Hash에 리프레시 토큰과 만료시점 저장
            redisTemplate.opsForHash().put(tokenKey, REFRESH_TOKEN_FIELD, refreshToken);
            redisTemplate.opsForHash().put(tokenKey, REFRESH_TOKEN_EXPIRY_FIELD, String.valueOf(refreshExpiry.getTime()));

            // 전체 Hash에 TTL 설정 (리프레시 토큰 만료시간으로 설정)
            redisTemplate.expire(tokenKey, Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME));

            log.info("=== Refresh token Hash 저장 성공: key={}, value={}, expiry={} ===", tokenKey, refreshToken, refreshExpiry);

            // 저장 확인
            String storedToken = (String) redisTemplate.opsForHash().get(tokenKey, REFRESH_TOKEN_FIELD);
            String storedExpiryStr = (String) redisTemplate.opsForHash().get(tokenKey, REFRESH_TOKEN_EXPIRY_FIELD);
            if (storedExpiryStr != null) {
                long storedExpiry = Long.parseLong(storedExpiryStr);
                log.info("=== Hash 저장 확인: storedToken={}, storedExpiry={} ===", storedToken, new Date(storedExpiry));
            }
        } catch (Exception e) {
            log.error("=== Refresh token Hash 저장 실패: {} ===", e.getMessage(), e);
        }

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
        String tokenKey = TOKEN_PREFIX + loginId;
        String storedToken = (String) redisTemplate.opsForHash().get(tokenKey, REFRESH_TOKEN_FIELD);
        return refreshToken.equals(storedToken);
    }

    public void deleteAllTokens(String loginId) {
        String tokenKey = TOKEN_PREFIX + loginId;
        redisTemplate.delete(tokenKey);
        log.info("=== 모든 토큰 Hash 삭제 완료: loginId={} ===", loginId);
    }

    /**
     * 리프레시 토큰으로 사용자명을 찾습니다.
     * Redis에서 모든 토큰 Hash를 순회하며 일치하는 리프레시 토큰을 찾습니다.
     * @param refreshToken 찾을 리프레시 토큰
     * @return 사용자명 또는 null
     */
    public String findUsernameByRefreshToken(String refreshToken) {
        String pattern = TOKEN_PREFIX + "*";
        var keys = redisTemplate.keys(pattern);
        for (String key : keys) {
            String storedToken = (String) redisTemplate.opsForHash().get(key, REFRESH_TOKEN_FIELD);
            if (refreshToken.equals(storedToken)) {
                return key.substring(TOKEN_PREFIX.length());
            }
        }
        return null;
    }
}