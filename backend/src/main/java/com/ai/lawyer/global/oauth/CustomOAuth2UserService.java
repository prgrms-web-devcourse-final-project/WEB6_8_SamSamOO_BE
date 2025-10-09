package com.ai.lawyer.global.oauth;

import com.ai.lawyer.domain.member.entity.OAuth2Member;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2MemberRepository oauth2MemberRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String accessToken = userRequest.getAccessToken().getTokenValue();

        log.info("OAuth2 로그인 시도: provider={}, accessToken={}",
                registrationId, accessToken.substring(0, Math.min(10, accessToken.length())) + "...");

        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if (userInfo.getEmail() == null) {
            throw new OAuth2AuthenticationException("이메일을 가져올 수 없습니다.");
        }

        // 이메일로 기존 OAuth2 회원 조회
        OAuth2Member member = oauth2MemberRepository.findByLoginId(userInfo.getEmail())
                .orElse(null);

        if (member == null) {
            // 신규 OAuth2 회원 생성
            log.info("신규 OAuth2 사용자: email={}, provider={}", userInfo.getEmail(), registrationId);
            member = createOAuth2Member(userInfo);
        } else {
            // 기존 OAuth2 회원 로그인
            log.info("기존 OAuth2 사용자 로그인: email={}, provider={}", userInfo.getEmail(), registrationId);
        }

        oauth2MemberRepository.save(member);

        // OAuth2 provider의 access token을 Redis에 저장 (연동 해제용)
        saveOAuth2ProviderAccessToken(userInfo.getEmail(), accessToken);

        // Note: JWT 토큰은 OAuth2SuccessHandler에서 생성되어 Redis에 저장됩니다.

        return new PrincipalDetails(member, oAuth2User.getAttributes());
    }

    /**
     * OAuth2 provider의 access token을 Redis에 저장합니다.
     * 이 토큰은 소셜 연동 해제(회원 탈퇴) 시 사용됩니다.
     * @param loginId 회원 loginId (email)
     * @param accessToken OAuth2 provider access token
     */
    private void saveOAuth2ProviderAccessToken(String loginId, String accessToken) {
        try {
            String key = "oauth2_provider_token:" + loginId;
            // 7일 TTL 설정 (refresh token과 동일한 기간)
            redisTemplate.opsForValue().set(key, accessToken, java.time.Duration.ofDays(7));
            log.info("OAuth2 provider access token 저장 완료: loginId={}", loginId);
        } catch (Exception e) {
            log.error("OAuth2 provider access token 저장 실패: loginId={}, error={}", loginId, e.getMessage());
        }
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equalsIgnoreCase(registrationId)) {
            return new KakaoUserInfo(attributes);
        } else if ("naver".equalsIgnoreCase(registrationId)) {
            return new NaverUserInfo(attributes);
        }
        throw new OAuth2AuthenticationException("지원하지 않는 로그인 방식입니다: " + registrationId);
    }

    private OAuth2Member createOAuth2Member(OAuth2UserInfo userInfo) {
        // 출생년도를 나이로 계산
        Integer age = calculateAgeFromBirthYear(userInfo.getBirthYear());

        com.ai.lawyer.domain.member.entity.Member.Gender gender = null;
        if (userInfo.getGender() != null) {
            try {
                gender = com.ai.lawyer.domain.member.entity.Member.Gender.valueOf(userInfo.getGender());
            } catch (IllegalArgumentException e) {
                log.warn("성별 파싱 실패: {}", userInfo.getGender());
            }
        }

        String email = userInfo.getEmail();

        return OAuth2Member.builder()
                .loginId(email)  // loginId와 email을 동일하게 설정
                .email(email)    // email 컬럼에도 저장
                .name(userInfo.getName() != null ? userInfo.getName() : "정보없음")
                .age(age != null ? age : 20) // 기본값
                .gender(gender != null ? gender : com.ai.lawyer.domain.member.entity.Member.Gender.OTHER) // 기본값
                .provider(OAuth2Member.Provider.valueOf(userInfo.getProvider()))
                .providerId(userInfo.getProviderId())
                .role(com.ai.lawyer.domain.member.entity.Member.Role.USER)
                .build();
    }

    /**
     * 출생년도를 현재 나이로 계산
     * @param birthYear 출생년도 (예: "1990")
     * @return 현재 나이, 파싱 실패 시 null
     */
    private Integer calculateAgeFromBirthYear(String birthYear) {
        if (birthYear == null || birthYear.trim().isEmpty()) {
            return null;
        }

        try {
            int year = Integer.parseInt(birthYear.trim());
            int currentYear = java.time.Year.now().getValue();
            int age = currentYear - year + 1; // 한국 나이 계산 (만 나이 + 1)

            // 유효성 검사
            if (age < 0 || age > 150) {
                log.warn("비정상적인 나이 계산됨: birthYear={}, age={}", birthYear, age);
                return null;
            }

            return age;
        } catch (NumberFormatException e) {
            log.warn("출생년도 파싱 실패: {}", birthYear);
            return null;
        }
    }
}
