package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.entity.Member;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenProvider {

    // 임시로 토큰과 사용자 정보를 매핑하는 메모리 저장소 (추후 레디스로 대체)
    private final Map<String, String> tokenToLoginIdMap = new ConcurrentHashMap<>();

    public String generateAccessToken(Member member) {
        // TODO: JWT 의존성 추가 후 실제 JWT 토큰 생성로직으로 변경
        // 현재는 임시로 UUID 사용 (추후 레디스에서 매핑 관리)
        String token = "access_" + UUID.randomUUID();
        tokenToLoginIdMap.put(token, member.getLoginId());
        return token;
    }

    public String generateRefreshToken(Member member) {
        // TODO: JWT 의존성 추가 후 실제 JWT 토큰 생성로직으로 변경
        // 현재는 임시로 UUID 사용 (추후 레디스에서 매핑 관리)
        String token = "refresh_" + UUID.randomUUID();
        tokenToLoginIdMap.put(token, member.getLoginId());
        return token;
    }

    public boolean validateToken(String token) {
        // TODO: JWT 의존성 추가 후 실제 토큰 검증 로직으로 변경
        return token != null && !token.isEmpty() && tokenToLoginIdMap.containsKey(token);
    }

    public String getUsernameFromToken(String token) {
        // TODO: JWT 의존성 추가 후 실제 토큰에서 사용자 정보 추출 로직으로 변경
        return tokenToLoginIdMap.get(token);
    }
}