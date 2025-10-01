package com.ai.lawyer.global.oauth;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getEmail();
    String getName();
    String getGender();
    String getBirthYear(); // ageRange 대신 birthYear 사용
}
