package com.ai.lawyer.global.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/redis-test")
@RequiredArgsConstructor
@Slf4j
public class RedisTestController {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/set")
    public ResponseEntity<String> setValue(@RequestParam String key, @RequestParam String value) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
            log.info("Redis set 성공: key={}, value={}", key, value);
            return ResponseEntity.ok("저장 성공");
        } catch (Exception e) {
            log.error("Redis set 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("저장 실패: " + e.getMessage());
        }
    }

    @GetMapping("/get")
    public ResponseEntity<String> getValue(@RequestParam String key) {
        try {
            String value = (String) redisTemplate.opsForValue().get(key);
            log.info("Redis get 성공: key={}, value={}", key, value);
            return ResponseEntity.ok("값: " + value);
        } catch (Exception e) {
            log.error("Redis get 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("조회 실패: " + e.getMessage());
        }
    }

    @GetMapping("/keys")
    public ResponseEntity<Set<String>> getAllKeys(@RequestParam(defaultValue = "*") String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            log.info("Redis keys 조회 성공: pattern={}, count={}", pattern, keys.size());
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            log.error("Redis keys 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteKey(@RequestParam String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("Redis delete 결과: key={}, deleted={}", key, deleted);
            return ResponseEntity.ok("삭제 결과: " + deleted);
        } catch (Exception e) {
            log.error("Redis delete 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("삭제 실패: " + e.getMessage());
        }
    }

    @GetMapping("/connection-test")
    public ResponseEntity<String> testConnection() {
        try {
            Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().ping();
            log.info("Redis 연결 테스트 성공");
            return ResponseEntity.ok("Redis 연결 성공");
        } catch (Exception e) {
            log.error("Redis 연결 테스트 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Redis 연결 실패: " + e.getMessage());
        }
    }

    @GetMapping("/tokens/{loginId}")
    public ResponseEntity<String> getTokenInfo(@PathVariable String loginId) {
        try {
            StringBuilder info = new StringBuilder();
            String tokenKey = "tokens:" + loginId;

            // hash 토큰 정보 조회
            String accessToken = (String) redisTemplate.opsForHash().get(tokenKey, "accessToken");
            String accessTokenExpiryStr = (String) redisTemplate.opsForHash().get(tokenKey, "accessTokenExpiry");
            Long accessTokenExpiry = accessTokenExpiryStr != null ? Long.parseLong(accessTokenExpiryStr) : null;

            String refreshToken = (String) redisTemplate.opsForHash().get(tokenKey, "refreshToken");
            String refreshTokenExpiryStr = (String) redisTemplate.opsForHash().get(tokenKey, "refreshTokenExpiry");
            Long refreshTokenExpiry = refreshTokenExpiryStr != null ? Long.parseLong(refreshTokenExpiryStr) : null;

            // Hash TTL 조회
            Long ttl = redisTemplate.getExpire(tokenKey);

            info.append("=== 토큰 Hash 정보 ===\n");
            info.append("Login ID: ").append(loginId).append("\n");
            info.append("Redis Key: ").append(tokenKey).append("\n");
            info.append("TTL: ").append(ttl).append("초").append("\n\n");

            info.append("[Access Token]\n");
            info.append("Token: ").append(accessToken != null ? accessToken.substring(0, Math.min(50, accessToken.length())) + "..." : "없음").append("\n");
            info.append("Expiry: ").append(accessTokenExpiry != null ? new java.util.Date(accessTokenExpiry) : "없음").append("\n\n");

            info.append("[Refresh Token]\n");
            info.append("Token: ").append(refreshToken != null ? refreshToken : "없음").append("\n");
            info.append("Expiry: ").append(refreshTokenExpiry != null ? new java.util.Date(refreshTokenExpiry) : "없음").append("\n");

            log.info("토큰 Hash 정보 조회 성공: loginId={}", loginId);
            return ResponseEntity.ok(info.toString());
        } catch (Exception e) {
            log.error("토큰 Hash 정보 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("토큰 정보 조회 실패: " + e.getMessage());
        }
    }
}