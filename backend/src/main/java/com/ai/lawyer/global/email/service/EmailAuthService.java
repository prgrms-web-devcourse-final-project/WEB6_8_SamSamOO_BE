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

    /**
     * 인증번호 생성 후 Redis에 저장
     */
    public String generateAndSaveAuthCode(String loginId) {
        String code = String.format("%06d", new Random().nextInt(999999)); // 6자리 랜덤 숫자
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
            redisTemplate.delete(key); // 성공 시 삭제
            return true;
        }
        return false;
    }

    private String buildKey(String loginId) {
        return "email:auth:" + loginId;
    }

}
