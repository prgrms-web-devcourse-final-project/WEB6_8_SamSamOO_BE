package com.ai.lawyer.global.email.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long EXPIRATION_MINUTES = 5; // 인증번호 유효시간 (5분)
    private static final Random RANDOM = new Random(); // Random 인스턴스 재사용

    /**
     * 인증번호 생성 후 Redis에 저장
     */
    public String generateAndSaveAuthCode(String loginId) {
        String code = String.format("%06d", RANDOM.nextInt(999999)); // 6자리 랜덤 숫자
        redisTemplate.opsForValue().set(buildKey(loginId), code, EXPIRATION_MINUTES, TimeUnit.MINUTES);
        return code;
    }

    /**
     * 인증번호 검증
     */
    public boolean verifyAuthCode(String loginId, String inputCode) {
        String key = buildKey(loginId);
        String savedCode = (String) redisTemplate.opsForValue().get(key);
        if (savedCode != null && savedCode.equals(inputCode)) {
            // 성공 시 삭제하지 않고 인증 성공 표시로 업데이트
            String successKey = buildSuccessKey(loginId);
            redisTemplate.opsForValue().set(successKey, "true", EXPIRATION_MINUTES, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }

    /**
     * 비밀번호 검증 성공 표시 (로그인 사용자용)
     */
    public void markPasswordVerified(String loginId) {
        String successKey = buildSuccessKey(loginId);
        redisTemplate.opsForValue().set(successKey, "true", EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 이메일 인증 성공 여부 확인
     */
    public boolean isEmailVerified(String loginId) {
        String successKey = buildSuccessKey(loginId);
        String isVerified = (String) redisTemplate.opsForValue().get(successKey);
        return "true".equals(isVerified);
    }

    /**
     * 비밀번호 재설정 완료 후 인증 데이터 삭제
     */
    public void clearAuthData(String loginId) {
        String key = buildKey(loginId);
        String successKey = buildSuccessKey(loginId);
        redisTemplate.delete(key);
        redisTemplate.delete(successKey);
    }

    private String buildKey(String loginId) {
        return "email:auth:" + loginId;
    }

    private String buildSuccessKey(String loginId) {
        return "email:auth:success:" + loginId;
    }

}
