package com.ai.lawyer.global.jwt;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.global.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenProvider 테스트")
class TokenProviderTest {

    private static final Logger log = LoggerFactory.getLogger(TokenProviderTest.class);

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtProperties.AccessToken accessTokenProperties;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private TokenProvider tokenProvider;

    private Member member;
    private String secretKey;

    private static final String TOKEN_PREFIX = "tokens:";
    private static final String ACCESS_TOKEN_FIELD = "accessToken";
    private static final String ACCESS_TOKEN_EXPIRY_FIELD = "accessTokenExpiry";
    private static final String REFRESH_TOKEN_FIELD = "refreshToken";
    private static final String REFRESH_TOKEN_EXPIRY_FIELD = "refreshTokenExpiry";
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60; // 7일

    @BeforeEach
    void setUp() {
        log.info("=== 테스트 초기화 시작 ===");

        // Member 생성
        member = Member.builder()
                .memberId(1L)
                .loginId("test@example.com")
                .password("encodedPassword")
                .age(25)
                .gender(Member.Gender.MALE)
                .name("테스트")
                .role(Member.Role.USER)
                .build();

        // JWT 설정 (최소 256비트 = 32바이트 이상의 키 필요)
        secretKey = "testSecretKeyForJWTTokenGenerationAndValidation1234567890";

        // Mock 설정 - lenient() 사용하여 모든 테스트에서 사용되지 않아도 경고하지 않음
        lenient().when(jwtProperties.getSecretKey()).thenReturn(secretKey);
        lenient().when(jwtProperties.getAccessToken()).thenReturn(accessTokenProperties);
        long accessTokenExpirationSeconds = 3600L;
        lenient().when(accessTokenProperties.getExpirationSeconds()).thenReturn(accessTokenExpirationSeconds);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        log.info("테스트 데이터 초기화 완료: memberId={}, loginId={}", member.getMemberId(), member.getLoginId());
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공 - JWT 구조, claims, 만료시간 검증")
    void generateAccessToken_Success() {
        // given
        log.info("=== 액세스 토큰 생성 테스트 시작 ===");
        String tokenKey = TOKEN_PREFIX + member.getLoginId();

        willDoNothing().given(hashOperations).put(eq(tokenKey), eq(ACCESS_TOKEN_FIELD), anyString());
        willDoNothing().given(hashOperations).put(eq(tokenKey), eq(ACCESS_TOKEN_EXPIRY_FIELD), anyString());
        given(redisTemplate.expire(eq(tokenKey), any(Duration.class))).willReturn(true);
        log.info("Redis Mock 설정 완료");

        // when
        log.info("액세스 토큰 생성 호출 중...");
        String token = tokenProvider.generateAccessToken(member);
        log.info("액세스 토큰 생성 완료: {}", token);

        // then
        log.info("토큰 검증 시작");
        assertThat(token).as("토큰이 null이 아님").isNotNull();
        assertThat(token.split("\\.")).as("JWT는 3개 부분으로 구성됨 (header.payload.signature)").hasSize(3);

        // JWT 파싱하여 claims 검증
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.get("loginid", String.class)).as("loginId claim 일치").isEqualTo("test@example.com");
        assertThat(claims.get("memberId", Long.class)).as("memberId claim 일치").isEqualTo(1L);
        assertThat(claims.get("role", String.class)).as("role claim 일치").isEqualTo("USER");
        assertThat(claims.getIssuedAt()).as("발급 시간 존재").isNotNull();
        assertThat(claims.getExpiration()).as("만료 시간 존재").isNotNull();
        assertThat(claims.getExpiration()).as("만료 시간이 발급 시간 이후").isAfter(claims.getIssuedAt());
        log.info("JWT claims 검증 완료");

        // Redis 저장 검증
        log.info("Redis 저장 검증 시작");
        verify(hashOperations).put(eq(tokenKey), eq(ACCESS_TOKEN_FIELD), eq(token));
        verify(hashOperations).put(eq(tokenKey), eq(ACCESS_TOKEN_EXPIRY_FIELD), anyString());
        verify(redisTemplate).expire(eq(tokenKey), eq(Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME)));
        log.info("Redis 저장 호출 확인 완료");
        log.info("=== 액세스 토큰 생성 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공 - UUID 형식 및 Redis 저장 검증")
    void generateRefreshToken_Success() {
        // given
        log.info("=== 리프레시 토큰 생성 테스트 시작 ===");
        String tokenKey = TOKEN_PREFIX + member.getLoginId();

        willDoNothing().given(hashOperations).put(eq(tokenKey), eq(REFRESH_TOKEN_FIELD), anyString());
        willDoNothing().given(hashOperations).put(eq(tokenKey), eq(REFRESH_TOKEN_EXPIRY_FIELD), anyString());
        given(hashOperations.get(eq(tokenKey), eq(REFRESH_TOKEN_FIELD))).willAnswer(invocation -> {
            // 저장 확인용 mock - 생성된 토큰 반환
            return null; // 실제로는 저장된 토큰이 반환되지만, 생성 로직에서는 null 체크 없이 진행
        });
        given(hashOperations.get(eq(tokenKey), eq(REFRESH_TOKEN_EXPIRY_FIELD))).willReturn(String.valueOf(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME * 1000));
        given(redisTemplate.expire(eq(tokenKey), any(Duration.class))).willReturn(true);
        log.info("Redis Mock 설정 완료");

        // when
        log.info("리프레시 토큰 생성 호출 중...");
        String refreshToken = tokenProvider.generateRefreshToken(member);
        log.info("리프레시 토큰 생성 완료: {}", refreshToken);

        // then
        log.info("토큰 검증 시작");
        assertThat(refreshToken).as("토큰이 null이 아님").isNotNull();
        assertThat(refreshToken).as("토큰이 비어있지 않음").isNotEmpty();

        // UUID 형식 검증 (8-4-4-4-12)
        String uuidPattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        assertThat(refreshToken).as("UUID 형식 일치").matches(uuidPattern);
        log.info("UUID 형식 검증 완료");

        // Redis 저장 검증
        log.info("Redis 저장 검증 시작");
        verify(hashOperations).put(eq(tokenKey), eq(REFRESH_TOKEN_FIELD), eq(refreshToken));
        verify(hashOperations).put(eq(tokenKey), eq(REFRESH_TOKEN_EXPIRY_FIELD), anyString());
        verify(redisTemplate).expire(eq(tokenKey), eq(Duration.ofSeconds(REFRESH_TOKEN_EXPIRE_TIME)));
        verify(hashOperations).get(eq(tokenKey), eq(REFRESH_TOKEN_FIELD));
        verify(hashOperations).get(eq(tokenKey), eq(REFRESH_TOKEN_EXPIRY_FIELD));
        log.info("Redis 저장 및 저장 확인 호출 검증 완료");
        log.info("=== 리프레시 토큰 생성 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 검증 성공 - 유효한 토큰")
    void validateTokenWithResult_Valid() {
        // given
        log.info("=== 토큰 검증(유효) 테스트 시작 ===");
        willDoNothing().given(hashOperations).put(anyString(), anyString(), anyString());
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);

        String token = tokenProvider.generateAccessToken(member);
        log.info("유효한 토큰 생성 완료");

        // when
        log.info("토큰 검증 호출 중...");
        TokenProvider.TokenValidationResult result = tokenProvider.validateTokenWithResult(token);
        log.info("토큰 검증 완료: {}", result);

        // then
        assertThat(result).as("토큰이 VALID 상태").isEqualTo(TokenProvider.TokenValidationResult.VALID);
        log.info("=== 토큰 검증(유효) 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 검증 실패 - 만료된 토큰")
    void validateTokenWithResult_Expired() {
        // given
        log.info("=== 토큰 검증(만료) 테스트 시작 ===");

        // 만료된 토큰 생성 (expirationSeconds를 -1로 설정)
        lenient().when(accessTokenProperties.getExpirationSeconds()).thenReturn(-1L);

        willDoNothing().given(hashOperations).put(anyString(), anyString(), anyString());
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);

        String expiredToken = tokenProvider.generateAccessToken(member);
        log.info("만료된 토큰 생성 완료");

        // when
        log.info("토큰 검증 호출 중...");
        TokenProvider.TokenValidationResult result = tokenProvider.validateTokenWithResult(expiredToken);
        log.info("토큰 검증 완료: {}", result);

        // then
        assertThat(result).as("토큰이 EXPIRED 상태").isEqualTo(TokenProvider.TokenValidationResult.EXPIRED);
        log.info("=== 토큰 검증(만료) 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 검증 실패 - 잘못된 토큰 형식")
    void validateTokenWithResult_Invalid_MalformedToken() {
        // given
        log.info("=== 토큰 검증(잘못된 형식) 테스트 시작 ===");
        String invalidToken = "invalid.token.format";
        log.info("잘못된 형식의 토큰: {}", invalidToken);

        // when
        log.info("토큰 검증 호출 중...");
        TokenProvider.TokenValidationResult result = tokenProvider.validateTokenWithResult(invalidToken);
        log.info("토큰 검증 완료: {}", result);

        // then
        assertThat(result).as("토큰이 INVALID 상태").isEqualTo(TokenProvider.TokenValidationResult.INVALID);
        log.info("=== 토큰 검증(잘못된 형식) 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 검증 실패 - null 토큰")
    void validateTokenWithResult_Invalid_NullToken() {
        // given
        log.info("=== 토큰 검증(null) 테스트 시작 ===");
        String nullToken = null;

        // when
        log.info("토큰 검증 호출 중...");
        TokenProvider.TokenValidationResult result = tokenProvider.validateTokenWithResult(nullToken);
        log.info("토큰 검증 완료: {}", result);

        // then
        assertThat(result).as("null 토큰은 INVALID 상태").isEqualTo(TokenProvider.TokenValidationResult.INVALID);
        log.info("=== 토큰 검증(null) 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰에서 loginId 추출 성공")
    void getLoginIdFromToken_Success() {
        // given
        log.info("=== 토큰에서 loginId 추출 테스트 시작 ===");
        willDoNothing().given(hashOperations).put(anyString(), anyString(), anyString());
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);

        String token = tokenProvider.generateAccessToken(member);
        log.info("토큰 생성 완료");

        // when
        log.info("loginId 추출 호출 중...");
        String loginId = tokenProvider.getLoginIdFromToken(token);
        log.info("loginId 추출 완료: {}", loginId);

        // then
        assertThat(loginId).as("loginId가 null이 아님").isNotNull();
        assertThat(loginId).as("loginId 일치").isEqualTo("test@example.com");
        log.info("=== 토큰에서 loginId 추출 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰에서 loginId 추출 실패 - 유효하지 않은 토큰")
    void getLoginIdFromToken_Fail_InvalidToken() {
        // given
        log.info("=== 토큰에서 loginId 추출 실패 테스트 시작 ===");
        String invalidToken = "invalid.token.format";

        // when
        log.info("loginId 추출 호출 중...");
        String loginId = tokenProvider.getLoginIdFromToken(invalidToken);
        log.info("loginId 추출 결과: {}", loginId);

        // then
        assertThat(loginId).as("유효하지 않은 토큰에서는 null 반환").isNull();
        log.info("=== 토큰에서 loginId 추출 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰에서 memberId 추출 성공")
    void getMemberIdFromToken_Success() {
        // given
        log.info("=== 토큰에서 memberId 추출 테스트 시작 ===");
        willDoNothing().given(hashOperations).put(anyString(), anyString(), anyString());
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);

        String token = tokenProvider.generateAccessToken(member);
        log.info("토큰 생성 완료");

        // when
        log.info("memberId 추출 호출 중...");
        Long memberId = tokenProvider.getMemberIdFromToken(token);
        log.info("memberId 추출 완료: {}", memberId);

        // then
        assertThat(memberId).as("memberId가 null이 아님").isNotNull();
        assertThat(memberId).as("memberId 일치").isEqualTo(1L);
        log.info("=== 토큰에서 memberId 추출 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰에서 memberId 추출 실패 - 유효하지 않은 토큰")
    void getMemberIdFromToken_Fail_InvalidToken() {
        // given
        log.info("=== 토큰에서 memberId 추출 실패 테스트 시작 ===");
        String invalidToken = "invalid.token.format";

        // when
        log.info("memberId 추출 호출 중...");
        Long memberId = tokenProvider.getMemberIdFromToken(invalidToken);
        log.info("memberId 추출 결과: {}", memberId);

        // then
        assertThat(memberId).as("유효하지 않은 토큰에서는 null 반환").isNull();
        log.info("=== 토큰에서 memberId 추출 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰에서 role 추출 성공")
    void getRoleFromToken_Success() {
        // given
        log.info("=== 토큰에서 role 추출 테스트 시작 ===");
        willDoNothing().given(hashOperations).put(anyString(), anyString(), anyString());
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);

        String token = tokenProvider.generateAccessToken(member);
        log.info("토큰 생성 완료");

        // when
        log.info("role 추출 호출 중...");
        String role = tokenProvider.getRoleFromToken(token);
        log.info("role 추출 완료: {}", role);

        // then
        assertThat(role).as("role이 null이 아님").isNotNull();
        assertThat(role).as("role 일치").isEqualTo("USER");
        log.info("=== 토큰에서 role 추출 테스트 완료 ===");
    }

    @Test
    @DisplayName("만료된 토큰에서 loginId 추출 성공")
    void getLoginIdFromExpiredToken_Success() {
        // given
        log.info("=== 만료된 토큰에서 loginId 추출 테스트 시작 ===");

        // 만료된 토큰 생성
        lenient().when(accessTokenProperties.getExpirationSeconds()).thenReturn(-1L);
        willDoNothing().given(hashOperations).put(anyString(), anyString(), anyString());
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);

        String expiredToken = tokenProvider.generateAccessToken(member);
        log.info("만료된 토큰 생성 완료");

        // when
        log.info("만료된 토큰에서 loginId 추출 호출 중...");
        String loginId = tokenProvider.getLoginIdFromExpiredToken(expiredToken);
        log.info("loginId 추출 완료: {}", loginId);

        // then
        assertThat(loginId).as("만료된 토큰에서도 loginId 추출 가능").isNotNull();
        assertThat(loginId).as("loginId 일치").isEqualTo("test@example.com");
        log.info("=== 만료된 토큰에서 loginId 추출 테스트 완료 ===");
    }

    @Test
    @DisplayName("만료된 토큰에서 loginId 추출 실패 - 잘못된 토큰")
    void getLoginIdFromExpiredToken_Fail_InvalidToken() {
        // given
        log.info("=== 만료된 토큰에서 loginId 추출 실패 테스트 시작 ===");
        String invalidToken = "invalid.token.format";

        // when
        log.info("loginId 추출 호출 중...");
        String loginId = tokenProvider.getLoginIdFromExpiredToken(invalidToken);
        log.info("loginId 추출 결과: {}", loginId);

        // then
        assertThat(loginId).as("잘못된 토큰에서는 null 반환").isNull();
        log.info("=== 만료된 토큰에서 loginId 추출 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰 검증 성공 - Redis에서 일치하는 토큰 확인")
    void validateRefreshToken_Success() {
        // given
        log.info("=== 리프레시 토큰 검증 성공 테스트 시작 ===");
        String loginId = "test@example.com";
        String refreshToken = "test-refresh-token";
        String tokenKey = TOKEN_PREFIX + loginId;

        given(hashOperations.get(tokenKey, REFRESH_TOKEN_FIELD)).willReturn(refreshToken);
        log.info("Redis Mock 설정 완료: 저장된 토큰={}", refreshToken);

        // when
        log.info("리프레시 토큰 검증 호출 중...");
        boolean isValid = tokenProvider.validateRefreshToken(loginId, refreshToken);
        log.info("검증 결과: {}", isValid);

        // then
        assertThat(isValid).as("리프레시 토큰 검증 성공").isTrue();
        verify(hashOperations).get(tokenKey, REFRESH_TOKEN_FIELD);
        log.info("=== 리프레시 토큰 검증 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰 검증 실패 - Redis에 저장된 토큰과 불일치")
    void validateRefreshToken_Fail_TokenMismatch() {
        // given
        log.info("=== 리프레시 토큰 검증 실패(불일치) 테스트 시작 ===");
        String loginId = "test@example.com";
        String refreshToken = "test-refresh-token";
        String storedToken = "different-refresh-token";
        String tokenKey = TOKEN_PREFIX + loginId;

        given(hashOperations.get(tokenKey, REFRESH_TOKEN_FIELD)).willReturn(storedToken);
        log.info("Redis Mock 설정 완료: 저장된 토큰={}, 입력 토큰={}", storedToken, refreshToken);

        // when
        log.info("리프레시 토큰 검증 호출 중...");
        boolean isValid = tokenProvider.validateRefreshToken(loginId, refreshToken);
        log.info("검증 결과: {}", isValid);

        // then
        assertThat(isValid).as("리프레시 토큰 불일치로 검증 실패").isFalse();
        verify(hashOperations).get(tokenKey, REFRESH_TOKEN_FIELD);
        log.info("=== 리프레시 토큰 검증 실패(불일치) 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰 검증 실패 - Redis에 토큰이 없음")
    void validateRefreshToken_Fail_NoTokenInRedis() {
        // given
        log.info("=== 리프레시 토큰 검증 실패(없음) 테스트 시작 ===");
        String loginId = "test@example.com";
        String refreshToken = "test-refresh-token";
        String tokenKey = TOKEN_PREFIX + loginId;

        given(hashOperations.get(tokenKey, REFRESH_TOKEN_FIELD)).willReturn(null);
        log.info("Redis Mock 설정 완료: 저장된 토큰 없음");

        // when
        log.info("리프레시 토큰 검증 호출 중...");
        boolean isValid = tokenProvider.validateRefreshToken(loginId, refreshToken);
        log.info("검증 결과: {}", isValid);

        // then
        assertThat(isValid).as("Redis에 토큰이 없어 검증 실패").isFalse();
        verify(hashOperations).get(tokenKey, REFRESH_TOKEN_FIELD);
        log.info("=== 리프레시 토큰 검증 실패(없음) 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰으로 사용자명 찾기 성공")
    void findUsernameByRefreshToken_Success() {
        // given
        log.info("=== 리프레시 토큰으로 사용자명 찾기 성공 테스트 시작 ===");
        String refreshToken = "test-refresh-token";
        String loginId = "test@example.com";
        String tokenKey = TOKEN_PREFIX + loginId;

        Set<String> keys = new HashSet<>();
        keys.add(tokenKey);

        given(redisTemplate.keys(TOKEN_PREFIX + "*")).willReturn(keys);
        given(hashOperations.get(tokenKey, REFRESH_TOKEN_FIELD)).willReturn(refreshToken);
        log.info("Redis Mock 설정 완료: 저장된 토큰 키={}", tokenKey);

        // when
        log.info("사용자명 찾기 호출 중...");
        String foundLoginId = tokenProvider.findUsernameByRefreshToken(refreshToken);
        log.info("찾은 사용자명: {}", foundLoginId);

        // then
        assertThat(foundLoginId).as("사용자명을 찾음").isNotNull();
        assertThat(foundLoginId).as("사용자명 일치").isEqualTo(loginId);
        verify(redisTemplate).keys(TOKEN_PREFIX + "*");
        verify(hashOperations).get(tokenKey, REFRESH_TOKEN_FIELD);
        log.info("=== 리프레시 토큰으로 사용자명 찾기 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰으로 사용자명 찾기 실패 - 일치하는 토큰 없음")
    void findUsernameByRefreshToken_Fail_NoMatch() {
        // given
        log.info("=== 리프레시 토큰으로 사용자명 찾기 실패 테스트 시작 ===");
        String refreshToken = "non-existent-token";
        String loginId = "test@example.com";
        String tokenKey = TOKEN_PREFIX + loginId;

        Set<String> keys = new HashSet<>();
        keys.add(tokenKey);

        given(redisTemplate.keys(TOKEN_PREFIX + "*")).willReturn(keys);
        given(hashOperations.get(tokenKey, REFRESH_TOKEN_FIELD)).willReturn("different-token");
        log.info("Redis Mock 설정 완료: 일치하는 토큰 없음");

        // when
        log.info("사용자명 찾기 호출 중...");
        String foundLoginId = tokenProvider.findUsernameByRefreshToken(refreshToken);
        log.info("찾은 사용자명: {}", foundLoginId);

        // then
        assertThat(foundLoginId).as("일치하는 토큰이 없어 null 반환").isNull();
        verify(redisTemplate).keys(TOKEN_PREFIX + "*");
        verify(hashOperations).get(tokenKey, REFRESH_TOKEN_FIELD);
        log.info("=== 리프레시 토큰으로 사용자명 찾기 실패 테스트 완료 ===");
    }

    @Test
    @DisplayName("리프레시 토큰으로 사용자명 찾기 실패 - Redis에 키가 없음")
    void findUsernameByRefreshToken_Fail_NoKeysInRedis() {
        // given
        log.info("=== 리프레시 토큰으로 사용자명 찾기 실패(키 없음) 테스트 시작 ===");
        String refreshToken = "test-refresh-token";

        Set<String> keys = new HashSet<>();

        given(redisTemplate.keys(TOKEN_PREFIX + "*")).willReturn(keys);
        log.info("Redis Mock 설정 완료: 저장된 키 없음");

        // when
        log.info("사용자명 찾기 호출 중...");
        String foundLoginId = tokenProvider.findUsernameByRefreshToken(refreshToken);
        log.info("찾은 사용자명: {}", foundLoginId);

        // then
        assertThat(foundLoginId).as("Redis에 키가 없어 null 반환").isNull();
        verify(redisTemplate).keys(TOKEN_PREFIX + "*");
        verify(hashOperations, never()).get(anyString(), anyString());
        log.info("=== 리프레시 토큰으로 사용자명 찾기 실패(키 없음) 테스트 완료 ===");
    }

    @Test
    @DisplayName("모든 토큰 삭제 성공 - Redis에서 사용자의 모든 토큰 삭제")
    void deleteAllTokens_Success() {
        // given
        log.info("=== 모든 토큰 삭제 테스트 시작 ===");
        String loginId = "test@example.com";
        String tokenKey = TOKEN_PREFIX + loginId;

        given(redisTemplate.delete(tokenKey)).willReturn(true);
        log.info("Redis Mock 설정 완료");

        // when
        log.info("토큰 삭제 호출 중...");
        tokenProvider.deleteAllTokens(loginId);
        log.info("토큰 삭제 완료");

        // then
        log.info("Redis 삭제 호출 검증");
        verify(redisTemplate).delete(tokenKey);
        log.info("=== 모든 토큰 삭제 테스트 완료 ===");
    }

    @Test
    @DisplayName("여러 사용자의 토큰 생성 및 검증 - 멀티 유저 시나리오")
    void multipleUsers_TokenGeneration() {
        // given
        log.info("=== 멀티 유저 토큰 생성 테스트 시작 ===");
        Member user1 = Member.builder()
                .memberId(1L)
                .loginId("user1@example.com")
                .role(Member.Role.USER)
                .build();

        Member user2 = Member.builder()
                .memberId(2L)
                .loginId("user2@example.com")
                .role(Member.Role.ADMIN)
                .build();

        willDoNothing().given(hashOperations).put(anyString(), anyString(), anyString());
        given(redisTemplate.expire(anyString(), any(Duration.class))).willReturn(true);

        // when
        log.info("두 사용자의 토큰 생성 중...");
        String token1 = tokenProvider.generateAccessToken(user1);
        String token2 = tokenProvider.generateAccessToken(user2);
        log.info("토큰 생성 완료");

        // then
        log.info("토큰 검증 시작");
        assertThat(token1).as("user1 토큰 생성됨").isNotNull();
        assertThat(token2).as("user2 토큰 생성됨").isNotNull();
        assertThat(token1).as("두 토큰은 다름").isNotEqualTo(token2);

        String loginId1 = tokenProvider.getLoginIdFromToken(token1);
        String loginId2 = tokenProvider.getLoginIdFromToken(token2);
        String role1 = tokenProvider.getRoleFromToken(token1);
        String role2 = tokenProvider.getRoleFromToken(token2);

        assertThat(loginId1).as("user1 loginId 일치").isEqualTo("user1@example.com");
        assertThat(loginId2).as("user2 loginId 일치").isEqualTo("user2@example.com");
        assertThat(role1).as("user1 role 일치").isEqualTo("USER");
        assertThat(role2).as("user2 role 일치").isEqualTo("ADMIN");
        log.info("=== 멀티 유저 토큰 생성 테스트 완료 ===");
    }
}