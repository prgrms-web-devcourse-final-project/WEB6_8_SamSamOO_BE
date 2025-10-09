package com.ai.lawyer.global.oauth;

import com.ai.lawyer.domain.member.entity.OAuth2Member;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2LogoutService {

    private final OAuth2MemberRepository oauth2MemberRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Setter
    @Getter
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${custom.frontend.url}")
    private String frontendUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private static final String BACKEND_LOGOUT_REDIRECT = "http://localhost:8080/api/auth/oauth2/test-page";
    private static final int HEALTH_CHECK_TIMEOUT = 2000; // 2초

    /**
     * OAuth2 제공자 로그아웃을 백엔드에서 직접 처리합니다.
     * @param loginId 회원 loginId
     * @return 로그아웃 성공 여부
     */
    public boolean logoutFromOAuth2Provider(String loginId) {
        if (loginId == null || loginId.isEmpty()) {
            return false;
        }

        Optional<OAuth2Member> oauth2Member = oauth2MemberRepository.findByLoginId(loginId);

        if (oauth2Member.isEmpty()) {
            log.info("일반 회원 로그아웃: loginId={}", loginId);
            return false;
        }

        OAuth2Member member = oauth2Member.get();
        OAuth2Member.Provider provider = member.getProvider();
        String providerId = member.getProviderId();

        log.info("OAuth2 회원 로그아웃 시도: loginId={}, provider={}, providerId={}", loginId, provider, providerId);

        try {
            return switch (provider) {
                case KAKAO -> logoutFromKakao(loginId);
                case NAVER -> unlinkFromNaver(loginId);
            };
        } catch (Exception e) {
            log.error("OAuth2 로그아웃 실패: loginId={}, provider={}, error={}", loginId, provider, e.getMessage());
            return false;
        }
    }

    /**
     * OAuth2 제공자의 로그아웃 URL을 반환합니다. (클라이언트 리다이렉트용)
     * @param loginId 회원 loginId
     * @return OAuth2 제공자 로그아웃 URL (OAuth2 회원이 아니면 null)
     */
    public String getOAuth2LogoutUrl(String loginId) {
        if (loginId == null || loginId.isEmpty()) {
            return null;
        }

        Optional<OAuth2Member> oauth2Member = oauth2MemberRepository.findByLoginId(loginId);

        if (oauth2Member.isEmpty()) {
            log.info("일반 회원 로그아웃: loginId={}", loginId);
            return null;
        }

        OAuth2Member member = oauth2Member.get();
        OAuth2Member.Provider provider = member.getProvider();

        log.info("OAuth2 회원 로그아웃 URL 생성: loginId={}, provider={}", loginId, provider);

        return switch (provider) {
            case KAKAO -> buildKakaoLogoutUrl();
            case NAVER -> buildNaverLogoutUrl();
        };
    }

    /**
     * 카카오 로그아웃
     * 참고: OAuth2 provider의 access token을 저장하지 않으므로
     * 클라이언트 측에서 처리하거나 세션만 종료합니다.
     */
    private boolean logoutFromKakao(String loginId) {
        log.info("카카오 로그아웃: loginId={} (로컬 세션만 종료)", loginId);
        // OAuth2 provider access token을 저장하지 않으므로
        // 로컬 세션 종료만 수행합니다.
        // 실제 카카오 로그아웃은 클라이언트에서 처리해야 합니다.
        return true;
    }

    /**
     * 네이버 로그아웃
     * 참고: 네이버는 공식 로그아웃 API가 없으며, OAuth2 provider access token을 저장하지 않으므로
     * 로컬 세션 종료만 수행합니다.
     */
    private boolean unlinkFromNaver(String loginId) {
        log.info("네이버 로그아웃: loginId={} (로컬 세션만 종료)", loginId);
        // OAuth2 provider access token을 저장하지 않으므로
        // 로컬 세션 종료만 수행합니다.
        return true;
    }

    /**
     * 카카오 로그아웃 URL 생성 (클라이언트 리다이렉트용)
     * 개발 환경에서 프론트엔드 헬스체크 후 폴백
     */
    private String buildKakaoLogoutUrl() {
        String logoutRedirectUri;

        // 개발 환경에서 프론트엔드 헬스체크
        if (isDevelopmentEnvironment() && !isFrontendAvailable()) {
            log.warn("프론트엔드 서버({})가 응답하지 않습니다. 백엔드 로그아웃 페이지로 폴백합니다.", frontendUrl);
            logoutRedirectUri = BACKEND_LOGOUT_REDIRECT;
        } else {
            logoutRedirectUri = frontendUrl + "/login";
        }

        log.info("카카오 로그아웃 리다이렉트 URI: {}", logoutRedirectUri);

        return String.format(
                "https://kauth.kakao.com/oauth/logout?client_id=%s&logout_redirect_uri=%s",
                kakaoClientId,
                logoutRedirectUri
        );
    }

    /**
     * 개발 환경인지 확인
     */
    private boolean isDevelopmentEnvironment() {
        return "dev".equals(activeProfile) || "local".equals(activeProfile);
    }

    /**
     * 프론트엔드 서버가 동작 중인지 확인
     */
    private boolean isFrontendAvailable() {
        try {
            java.net.URI uri = java.net.URI.create(frontendUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HEALTH_CHECK_TIMEOUT);
            connection.setReadTimeout(HEALTH_CHECK_TIMEOUT);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 200-299 또는 404도 서버가 살아있는 것으로 간주
            boolean isAvailable = (responseCode >= 200 && responseCode < 300) || responseCode == 404;
            log.debug("프론트엔드 헬스체크: {} - 응답코드 {}", frontendUrl, responseCode);
            return isAvailable;
        } catch (Exception e) {
            log.debug("프론트엔드 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 네이버 로그아웃 URL 생성 (클라이언트 리다이렉트용)
     * 참고: 네이버는 공식 로그아웃 API가 없으므로 null 반환
     */
    private String buildNaverLogoutUrl() {
        // 네이버는 공식 로그아웃 API를 제공하지 않습니다.
        // 프론트엔드에서 쿠키/세션 삭제로 처리
        return null;
    }

    /**
     * OAuth2 제공자에서 회원 연동 해제 (회원 탈퇴)
     * @param loginId 회원 loginId
     * @return 연동 해제 성공 여부
     */
    public boolean unlinkFromOAuth2Provider(String loginId) {
        if (loginId == null || loginId.isEmpty()) {
            return false;
        }

        Optional<OAuth2Member> oauth2Member = oauth2MemberRepository.findByLoginId(loginId);

        if (oauth2Member.isEmpty()) {
            log.info("일반 회원 연동 해제: loginId={}", loginId);
            return false;
        }

        OAuth2Member member = oauth2Member.get();
        OAuth2Member.Provider provider = member.getProvider();

        log.info("OAuth2 회원 연동 해제 시도: loginId={}, provider={}", loginId, provider);

        try {
            return switch (provider) {
                case KAKAO -> unlinkFromKakao(loginId);
                case NAVER -> unlinkFromNaverApp(loginId);
            };
        } catch (Exception e) {
            log.error("OAuth2 연동 해제 실패: loginId={}, provider={}, error={}", loginId, provider, e.getMessage());
            return false;
        }
    }

    /**
     * 카카오 연동 해제 (회원 탈퇴)
     * Redis에 저장된 OAuth2 provider access token을 사용하여 실제 카카오 연동 해제를 수행합니다.
     */
    private boolean unlinkFromKakao(String loginId) {
        log.info("카카오 연동 해제 시도: loginId={}", loginId);

        // Redis에서 OAuth2 provider access token 조회
        String accessToken = getOAuth2ProviderAccessToken(loginId);

        if (accessToken == null) {
            log.warn("카카오 연동 해제 실패: OAuth2 provider access token이 없습니다. loginId={}", loginId);
            return false;
        }

        try {
            // 카카오 연동 해제 API 호출
            String url = "https://kapi.kakao.com/v1/user/unlink";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("카카오 연동 해제 성공: loginId={}", loginId);
                deleteOAuth2ProviderAccessToken(loginId);
                return true;
            } else {
                log.warn("카카오 연동 해제 실패: loginId={}, status={}", loginId, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("카카오 연동 해제 API 호출 실패: loginId={}, error={}", loginId, e.getMessage());
            // API 호출 실패 시에도 로컬 처리는 계속 진행
            deleteOAuth2ProviderAccessToken(loginId);
            return false;
        }
    }

    /**
     * 네이버 연동 해제 (회원 탈퇴)
     * Redis에 저장된 OAuth2 provider access token을 사용하여 실제 네이버 연동 해제를 수행합니다.
     */
    private boolean unlinkFromNaverApp(String loginId) {
        log.info("네이버 연동 해제 시도: loginId={}", loginId);

        // Redis에서 OAuth2 provider access token 조회
        String accessToken = getOAuth2ProviderAccessToken(loginId);

        if (accessToken == null) {
            log.warn("네이버 연동 해제 실패: OAuth2 provider access token이 없습니다. loginId={}", loginId);
            return false;
        }

        try {
            // 네이버 연동 해제 API 호출
            String url = String.format(
                    "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=%s&client_secret=%s&access_token=%s&service_provider=NAVER",
                    naverClientId,
                    naverClientSecret,
                    accessToken
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("네이버 연동 해제 성공: loginId={}", loginId);
                deleteOAuth2ProviderAccessToken(loginId);
                return true;
            } else {
                log.warn("네이버 연동 해제 실패: loginId={}, status={}", loginId, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("네이버 연동 해제 API 호출 실패: loginId={}, error={}", loginId, e.getMessage());
            // API 호출 실패 시에도 로컬 처리는 계속 진행
            deleteOAuth2ProviderAccessToken(loginId);
            return false;
        }
    }

    /**
     * Redis에서 OAuth2 provider access token을 조회합니다.
     * @param loginId 회원 loginId
     * @return OAuth2 provider access token 또는 null
     */
    private String getOAuth2ProviderAccessToken(String loginId) {
        try {
            String key = "oauth2_provider_token:" + loginId;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("OAuth2 provider access token 조회 실패: loginId={}, error={}", loginId, e.getMessage());
            return null;
        }
    }

    /**
     * Redis에서 OAuth2 provider access token을 삭제합니다.
     * @param loginId 회원 loginId
     */
    private void deleteOAuth2ProviderAccessToken(String loginId) {
        try {
            String key = "oauth2_provider_token:" + loginId;
            redisTemplate.delete(key);
            log.info("OAuth2 provider access token 삭제 완료: loginId={}", loginId);
        } catch (Exception e) {
            log.error("OAuth2 provider access token 삭제 실패: loginId={}, error={}", loginId, e.getMessage());
        }
    }

}
