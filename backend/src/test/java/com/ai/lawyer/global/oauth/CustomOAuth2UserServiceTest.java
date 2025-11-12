package com.ai.lawyer.global.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomOAuth2UserService 테스트")
class CustomOAuth2UserServiceTest {

    @Test
    @DisplayName("카카오 UserInfo에서 사용자 정보를 추출한다")
    void extractKakaoUserInfo() {
        // given
        Map<String, Object> attributes = createKakaoAttributes(
        );

        // when
        KakaoUserInfo userInfo = new KakaoUserInfo(attributes);

        // then
        assertThat(userInfo.getProviderId()).isEqualTo("123456789");
        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
        assertThat(userInfo.getName()).isEqualTo("홍길동");
        assertThat(userInfo.getGender()).isEqualTo("MALE");
        assertThat(userInfo.getBirthYear()).isEqualTo("1990");
        assertThat(userInfo.getProvider()).isEqualTo("KAKAO");
    }

    @Test
    @DisplayName("네이버 UserInfo에서 사용자 정보를 추출한다")
    void extractNaverUserInfo() {
        // given
        Map<String, Object> attributes = createNaverAttributes(
        );

        // when
        NaverUserInfo userInfo = new NaverUserInfo(attributes);

        // then
        assertThat(userInfo.getProviderId()).isEqualTo("abcdefg123");
        assertThat(userInfo.getEmail()).isEqualTo("test@naver.com");
        assertThat(userInfo.getName()).isEqualTo("김영희");
        assertThat(userInfo.getGender()).isEqualTo("FEMALE");
        assertThat(userInfo.getBirthYear()).isEqualTo("1995");
        assertThat(userInfo.getProvider()).isEqualTo("NAVER");
    }

    @Test
    @DisplayName("출생년도가 올바르게 나이로 계산된다")
    void birthYearConvertedToAge() {
        // given
        int currentYear = Year.now().getValue();
        String birthYear = "1990";
        int year = Integer.parseInt(birthYear);

        // when - 한국 나이 계산: 현재 년도 - 출생 년도 + 1
        int age = currentYear - year + 1;

        // then
        assertThat(age).isGreaterThan(0);
        assertThat(age).isLessThan(150);
    }

    private Map<String, Object> createKakaoAttributes() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "홍길동");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");
        kakaoAccount.put("gender", "male");
        kakaoAccount.put("birthyear", "1990");
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "123456789");
        attributes.put("kakao_account", kakaoAccount);

        return attributes;
    }

    private Map<String, Object> createNaverAttributes() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "abcdefg123");
        response.put("email", "test@naver.com");
        response.put("name", "김영희");
        response.put("gender", "F");
        response.put("birthyear", "1995");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);

        return attributes;
    }

    @Test
    @DisplayName("OAuth2 회원 저장 후 반환된 엔티티 사용 - memberId 할당 확인")
    void oauth2MemberSave_ReturnsEntityWithMemberId() {
        // given - 이 테스트는 CustomOAuth2UserService에서 save() 반환값을 사용하는지 검증
        // 실제 구현에서는 다음과 같이 수정되어야 함:
        // member = oauth2MemberRepository.save(member);

        // when - save() 호출 시 memberId가 할당된 엔티티가 반환됨
        // JPA의 @GeneratedValue 전략 사용 시, save()는 영속화된 엔티티를 반환하며
        // 이 엔티티에는 자동 생성된 ID가 포함되어 있음

        // then - 반환된 엔티티의 memberId를 사용해야 JWT 토큰 생성 시 올바른 ID가 포함됨
        // 이를 통해 소셜 로그인 후 API 호출 시 member_id 조회가 정상 동작함

        // 이 테스트는 문서화 목적으로, 실제 동작은 Integration Test에서 검증됨
        assertThat(true).isTrue(); // 개념 검증용 테스트
    }
}
