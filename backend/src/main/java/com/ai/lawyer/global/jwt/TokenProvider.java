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

    // Redis Key 관련 상수
    private static final String TOKEN_PREFIX = "tokens:";
    private static final String ACCESS_TOKEN_FIELD = "accessToken";
    private static final String ACCESS_TOKEN_EXPIRY_FIELD = "accessTokenExpiry";
    private static final String REFRESH_TOKEN_FIELD = "refreshToken";
    private static final String REFRESH_TOKEN_EXPIRY_FIELD = "refreshTokenExpiry";

    // 토큰 만료 시간 상수
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60; // 7일 (초 단위)
    private static final long MILLIS_PER_SECOND = 1000L;

    // JWT Claim 상수
    private static final String CLAIM_LOGIN_ID = "loginId";
    private static final String CLAIM_MEMBER_ID = "memberId";
    private static final String CLAIM_ROLE = "role";

    // 로그 메시지 상수
    private static final String LOG_ACCESS_TOKEN_SAVED = "=== Access token Hash 저장 성공: key={}, expiry={} ===";
    private static final String LOG_ACCESS_TOKEN_SAVE_FAILED = "=== Access token Hash 저장 실패: {} ===";
    private static final String LOG_REFRESH_TOKEN_SAVED = "=== Refresh token Hash 저장 성공: key={}, value={}, expiry={} ===";
    private static final String LOG_REFRESH_TOKEN_SAVE_FAILED = "=== Refresh token Hash 저장 실패: {} ===";
    private static final String LOG_HASH_VERIFIED = "=== Hash 저장 확인: storedToken={}, storedExpiry={} ===";
    private static final String LOG_ALL_TOKENS_DELETED = "=== 모든 토큰 Hash 삭제 완료: loginId={} ===";
    private static final String LOG_EXPIRED_JWT = "만료된 JWT 토큰: {}";
    private static final String LOG_INVALID_JWT = "유효하지 않은 JWT 토큰: {}";
    private static final String LOG_MEMBER_ID_EXTRACTION_FAILED = "토큰에서 회원 ID 추출 실패: {}";
    private static final String LOG_ROLE_EXTRACTION_FAILED = "토큰에서 역할 정보 추출 실패: {}";
    private static final String LOG_LOGIN_ID_EXTRACTION_FAILED = "토큰에서 로그인 ID 추출 실패: {}";
    private static final String LOG_EXPIRED_TOKEN_LOGIN_ID_FAILED = "만료된 토큰에서 로그인 ID 추출 실패: {}";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(com.ai.lawyer.domain.member.entity.MemberAdapter member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessToken().getExpirationSeconds() * MILLIS_PER_SECOND);

        String accessToken = Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim(CLAIM_LOGIN_ID, member.getLoginId())
                .claim(CLAIM_MEMBER_ID, member.getMemberId())
                .claim(CLAIM_ROLE, member.getRole().name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Redis Hash에 액세스 토큰 정보 저장
        saveAccessTokenToRedis(member.getLoginId(), accessToken, expiry);

        return accessToken;
    }

    /**
     * Redis에 액세스 토큰을 저장합니다.
     */
    private void saveAccessTokenToRedis(String loginId, String accessToken, Date expiry) {
        try {
            String tokenKey = buildTokenKey(loginId);
            redisTemplate.opsForHash().put(tokenKey, ACCESS_TOKEN_FIELD, accessToken);
            redisTemplate.opsForHash().put(tokenKey, ACCESS_TOKEN_EXPIRY_FIELD, String.valueOf(expiry.getTime()));
            redisTemplate.expire(tokenKey, Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME));
            log.info(LOG_ACCESS_TOKEN_SAVED, tokenKey, expiry);
        } catch (Exception e) {
            log.error(LOG_ACCESS_TOKEN_SAVE_FAILED, e.getMessage(), e);
        }
    }

    public String generateRefreshToken(com.ai.lawyer.domain.member.entity.MemberAdapter member) {
        String refreshToken = UUID.randomUUID().toString();
        Date refreshExpiry = new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME * MILLIS_PER_SECOND);

        // Redis Hash에 리프레시 토큰 정보 저장
        saveRefreshTokenToRedis(member.getLoginId(), refreshToken, refreshExpiry);

        return refreshToken;
    }

    /**
     * Redis에 리프레시 토큰을 저장하고 검증합니다.
     */
    private void saveRefreshTokenToRedis(String loginId, String refreshToken, Date expiry) {
        try {
            String tokenKey = buildTokenKey(loginId);
            redisTemplate.opsForHash().put(tokenKey, REFRESH_TOKEN_FIELD, refreshToken);
            redisTemplate.opsForHash().put(tokenKey, REFRESH_TOKEN_EXPIRY_FIELD, String.valueOf(expiry.getTime()));
            redisTemplate.expire(tokenKey, Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME));
            log.info(LOG_REFRESH_TOKEN_SAVED, tokenKey, refreshToken, expiry);

            // 저장 확인
            verifyTokenStorageInRedis(tokenKey);
        } catch (Exception e) {
            log.error(LOG_REFRESH_TOKEN_SAVE_FAILED, e.getMessage(), e);
        }
    }

    /**
     * Redis에 저장된 토큰을 검증합니다.
     */
    private void verifyTokenStorageInRedis(String tokenKey) {
        String storedToken = (String) redisTemplate.opsForHash().get(tokenKey, REFRESH_TOKEN_FIELD);
        String storedExpiryStr = (String) redisTemplate.opsForHash().get(tokenKey, REFRESH_TOKEN_EXPIRY_FIELD);
        if (storedExpiryStr != null) {
            long storedExpiry = Long.parseLong(storedExpiryStr);
            log.info(LOG_HASH_VERIFIED, storedToken, new Date(storedExpiry));
        }
    }

    /**
     * 토큰 키를 생성합니다.
     */
    private String buildTokenKey(String loginId) {
        return TOKEN_PREFIX + loginId;
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
            log.warn(LOG_EXPIRED_JWT, e.getMessage());
            return TokenValidationResult.EXPIRED;
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException | SecurityException e) {
            log.warn(LOG_INVALID_JWT, e.getMessage());
            return TokenValidationResult.INVALID;
        }
    }

    public enum TokenValidationResult {
        VALID,      // 유효한 토큰
        EXPIRED,    // 만료된 토큰
        INVALID     // 잘못된 토큰
    }

    public Long getMemberIdFromToken(String token) {
        return getClaimFromToken(token, CLAIM_MEMBER_ID, Long.class, LOG_MEMBER_ID_EXTRACTION_FAILED);
    }

    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, CLAIM_ROLE, String.class, LOG_ROLE_EXTRACTION_FAILED);
    }

    public String getLoginIdFromToken(String token) {
        return getClaimFromToken(token, CLAIM_LOGIN_ID, String.class, LOG_LOGIN_ID_EXTRACTION_FAILED);
    }

    /**
     * 토큰에서 특정 Claim을 추출하는 공통 메서드
     */
    private <T> T getClaimFromToken(String token, String claimKey, Class<T> claimType, String errorLogMessage) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(claimKey, claimType);
        } catch (Exception e) {
            log.warn(errorLogMessage, e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰을 파싱하여 Claims를 반환합니다.
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 만료된 토큰에서도 loginId를 추출합니다.
     * @param token JWT 토큰 (만료되어도 괜찮음)
     * @return loginId 또는 null
     */
    public String getLoginIdFromExpiredToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(CLAIM_LOGIN_ID, String.class);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이지만 claim은 추출 가능
            return e.getClaims().get(CLAIM_LOGIN_ID, String.class);
        } catch (Exception e) {
            log.warn(LOG_EXPIRED_TOKEN_LOGIN_ID_FAILED, e.getMessage());
            return null;
        }
    }

    public boolean validateRefreshToken(String loginId, String refreshToken) {
        String tokenKey = buildTokenKey(loginId);
        String storedToken = (String) redisTemplate.opsForHash().get(tokenKey, REFRESH_TOKEN_FIELD);
        return refreshToken.equals(storedToken);
    }

    public void deleteAllTokens(String loginId) {
        String tokenKey = buildTokenKey(loginId);
        redisTemplate.delete(tokenKey);
        log.info(LOG_ALL_TOKENS_DELETED, loginId);
    }

    /**
     * 리프레시 토큰으로 사용자명을 찾습니다.
     * Redis 모든 토큰 Hash를 순회하며 일치하는 리프레시 토큰을 찾습니다.
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