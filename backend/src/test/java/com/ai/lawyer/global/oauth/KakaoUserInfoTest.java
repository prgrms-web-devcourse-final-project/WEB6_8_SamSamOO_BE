package com.ai.lawyer.global.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("카카오 사용자 정보 파싱 테스트")
class KakaoUserInfoTest {

    @Test
    @DisplayName("카카오 사용자 정보를 정상적으로 파싱한다")
    void parseKakaoUserInfo() {
        // given
        Map<String, Object> attributes = createKakaoAttributes(
                "홍길동",
                "male",
                "1990"
        );

        // when
        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        // then
        assertThat(userInfo.getProviderId()).isEqualTo("123456789");
        assertThat(userInfo.getProvider()).isEqualTo("KAKAO");
        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
        assertThat(userInfo.getName()).isEqualTo("홍길동");
        assertThat(userInfo.getGender()).isEqualTo("MALE");
        assertThat(userInfo.getBirthYear()).isEqualTo("1990");
    }

    @Test
    @DisplayName("여성 성별을 정상적으로 파싱한다")
    void parseFemalGender() {
        // given
        Map<String, Object> attributes = createKakaoAttributes(
                "김영희",
                "female",
                "1995"
        );

        // when
        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        // then
        assertThat(userInfo.getGender()).isEqualTo("FEMALE");
    }

    @Test
    @DisplayName("이메일이 없으면 null을 반환한다")
    void returnNullWhenNoEmail() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "123456789");
        attributes.put("kakao_account", new HashMap<>());

        // when
        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        // then
        assertThat(userInfo.getEmail()).isNull();
    }

    @Test
    @DisplayName("출생년도가 없으면 null을 반환한다")
    void returnNullWhenNoBirthYear() {
        // given
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "123456789");
        attributes.put("kakao_account", kakaoAccount);

        // when
        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        // then
        assertThat(userInfo.getBirthYear()).isNull();
    }

    private Map<String, Object> createKakaoAttributes(
            String nickname,
            String gender,
            String birthYear
    ) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", nickname);

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");
        kakaoAccount.put("gender", gender);
        kakaoAccount.put("birthyear", birthYear);
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "123456789");
        attributes.put("kakao_account", kakaoAccount);

        return attributes;
    }
}
