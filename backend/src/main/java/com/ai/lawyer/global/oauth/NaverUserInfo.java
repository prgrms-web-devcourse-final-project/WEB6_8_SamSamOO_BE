package com.ai.lawyer.global.oauth;

import lombok.Getter;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {
    @Getter
    private final Map<String, Object> attributes;
    private final Map<String, Object> response;

    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.response = extractResponseMap(attributes);
    }

    private Map<String, Object> extractResponseMap(Map<String, Object> attributes) {
        Object responseObj = attributes.get("response");
        if (responseObj instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) responseObj;
                return responseMap;
            } catch (ClassCastException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String getProviderId() {
        return response != null ? (String) response.get("id") : null;
    }

    @Override
    public String getProvider() {
        return "NAVER";
    }

    @Override
    public String getEmail() {
        return response != null ? (String) response.get("email") : null;
    }

    @Override
    public String getName() {
        return response != null ? (String) response.get("name") : null;
    }

    @Override
    public String getGender() {
        // 네이버 성별: "M", "F", "U"
        if (response != null && response.containsKey("gender")) {
            String gender = (String) response.get("gender");
            if ("M".equalsIgnoreCase(gender)) return "MALE";
            if ("F".equalsIgnoreCase(gender)) return "FEMALE";
        }
        return null;
    }

    @Override
    public String getBirthYear() {
        // 네이버 생년: "1990"
        if (response != null && response.containsKey("birthyear")) {
            return (String) response.get("birthyear");
        }
        return null;
    }

}
