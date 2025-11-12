package com.ai.lawyer.global.oauth;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = extractMapFromAttributes(attributes, "kakao_account");
        this.profile = extractMapFromAttributes(kakaoAccount, "profile");
    }

    private Map<String, Object> extractMapFromAttributes(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object obj = source.get(key);
        if (obj instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                return map;
            } catch (ClassCastException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "KAKAO";
    }

    @Override
    public String getEmail() {
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getName() {
        return profile != null ? (String) profile.get("nickname") : null;
    }

    @Override
    public String getGender() {
        // 카카오 성별: "male", "female"
        if (kakaoAccount != null && kakaoAccount.containsKey("gender")) {
            String gender = (String) kakaoAccount.get("gender");
            if ("male".equalsIgnoreCase(gender)) return "MALE";
            if ("female".equalsIgnoreCase(gender)) return "FEMALE";
        }
        return null;
    }

    @Override
    public String getBirthYear() {
        // 카카오 출생년도: "1990", "1985" 등
        if (kakaoAccount != null && kakaoAccount.containsKey("birthyear")) {
            return (String) kakaoAccount.get("birthyear");
        }
        return null;
    }
}
