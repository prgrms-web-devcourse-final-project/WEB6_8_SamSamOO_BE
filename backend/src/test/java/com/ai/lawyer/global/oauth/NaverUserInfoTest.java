package com.ai.lawyer.global.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("네이버 사용자 정보 파싱 테스트")
class NaverUserInfoTest {

    @Test
    @DisplayName("네이버 사용자 정보를 정상적으로 파싱한다")
    void parseNaverUserInfo() {
        // given
        Map<String, Object> attributes = createNaverAttributes(
                "홍길동",
                "M",
                "1990"
        );

        // when
        NaverUserInfo userInfo = new NaverUserInfo(attributes);

        // then
        assertThat(userInfo.getProviderId()).isEqualTo("abcdefg123");
        assertThat(userInfo.getProvider()).isEqualTo("NAVER");
        assertThat(userInfo.getEmail()).isEqualTo("test@naver.com");
        assertThat(userInfo.getName()).isEqualTo("홍길동");
        assertThat(userInfo.getGender()).isEqualTo("MALE");
        assertThat(userInfo.getBirthYear()).isEqualTo("1990");
    }

    @Test
    @DisplayName("여성 성별을 정상적으로 파싱한다")
    void parseFemaleGender() {
        // given
        Map<String, Object> attributes = createNaverAttributes(
                "김영희",
                "F",
                "1995"
        );

        // when
        NaverUserInfo userInfo = new NaverUserInfo(attributes);

        // then
        assertThat(userInfo.getGender()).isEqualTo("FEMALE");
    }

    @Test
    @DisplayName("알 수 없는 성별은 null을 반환한다")
    void returnNullForUnknownGender() {
        // given
        Map<String, Object> attributes = createNaverAttributes(
                "홍길동",
                "U",
                "1990"
        );

        // when
        NaverUserInfo userInfo = new NaverUserInfo(attributes);

        // then
        assertThat(userInfo.getGender()).isNull();
    }

    @Test
    @DisplayName("response가 없으면 null을 반환한다")
    void returnNullWhenNoResponse() {
        // given
        Map<String, Object> attributes = new HashMap<>();

        // when
        NaverUserInfo userInfo = new NaverUserInfo(attributes);

        // then
        assertThat(userInfo.getEmail()).isNull();
        assertThat(userInfo.getName()).isNull();
        assertThat(userInfo.getProviderId()).isNull();
    }

    private Map<String, Object> createNaverAttributes(
            String name,
            String gender,
            String birthYear
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "abcdefg123");
        response.put("email", "test@naver.com");
        response.put("name", name);
        response.put("gender", gender);
        response.put("birthyear", birthYear);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);

        return attributes;
    }
}
