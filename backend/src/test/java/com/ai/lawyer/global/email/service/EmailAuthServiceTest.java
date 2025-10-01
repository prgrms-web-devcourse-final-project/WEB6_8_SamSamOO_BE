package com.ai.lawyer.global.email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailAuthService 테스트")
class EmailAuthServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private EmailAuthService emailAuthService;

    private String loginId;
    private String authKey;
    private String successKey;

    @BeforeEach
    void setUp() {
        loginId = "test@example.com";
        authKey = "email:auth:" + loginId;
        successKey = "email:auth:success:" + loginId;
    }

    @Test
    @DisplayName("인증번호 생성 및 저장 성공")
    void generateAndSaveAuthCode_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        doNothing().when(valueOperations).set(eq(authKey), anyString(), eq(5L), eq(TimeUnit.MINUTES));

        // when
        String authCode = emailAuthService.generateAndSaveAuthCode(loginId);

        // then
        assertThat(authCode).isNotNull();
        assertThat(authCode).hasSize(6);
        assertThat(authCode).matches("\\d{6}"); // 6자리 숫자 패턴 확인

        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(authKey), eq(authCode), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("인증번호 검증 성공 - 올바른 인증번호")
    void verifyAuthCode_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String inputCode = "123456";
        given(valueOperations.get(authKey)).willReturn(inputCode);
        doNothing().when(valueOperations).set(eq(successKey), eq("true"), eq(5L), eq(TimeUnit.MINUTES));

        // when
        boolean result = emailAuthService.verifyAuthCode(loginId, inputCode);

        // then
        assertThat(result).isTrue();

        verify(redisTemplate, times(2)).opsForValue(); // get과 set 호출
        verify(valueOperations).get(authKey);
        verify(valueOperations).set(successKey, "true", 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("인증번호 검증 실패 - 잘못된 인증번호")
    void verifyAuthCode_Fail_InvalidCode() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String savedCode = "123456";
        String inputCode = "999999";
        given(valueOperations.get(authKey)).willReturn(savedCode);

        // when
        boolean result = emailAuthService.verifyAuthCode(loginId, inputCode);

        // then
        assertThat(result).isFalse();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(authKey);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("인증번호 검증 실패 - 저장된 인증번호 없음")
    void verifyAuthCode_Fail_NoSavedCode() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String inputCode = "123456";
        given(valueOperations.get(authKey)).willReturn(null);

        // when
        boolean result = emailAuthService.verifyAuthCode(loginId, inputCode);

        // then
        assertThat(result).isFalse();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(authKey);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("이메일 인증 성공 여부 확인 - 인증됨")
    void isEmailVerified_True() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(successKey)).willReturn("true");

        // when
        boolean result = emailAuthService.isEmailVerified(loginId);

        // then
        assertThat(result).isTrue();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(successKey);
    }

    @Test
    @DisplayName("이메일 인증 성공 여부 확인 - 인증되지 않음")
    void isEmailVerified_False() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(successKey)).willReturn(null);

        // when
        boolean result = emailAuthService.isEmailVerified(loginId);

        // then
        assertThat(result).isFalse();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(successKey);
    }

    @Test
    @DisplayName("이메일 인증 성공 여부 확인 - false 값")
    void isEmailVerified_FalseValue() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(successKey)).willReturn("false");

        // when
        boolean result = emailAuthService.isEmailVerified(loginId);

        // then
        assertThat(result).isFalse();

        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(successKey);
    }

    @Test
    @DisplayName("인증 데이터 삭제 성공")
    void clearAuthData_Success() {
        // given
        given(redisTemplate.delete(authKey)).willReturn(true);
        given(redisTemplate.delete(successKey)).willReturn(true);

        // when
        emailAuthService.clearAuthData(loginId);

        // then
        verify(redisTemplate).delete(authKey);
        verify(redisTemplate).delete(successKey);
    }

    @Test
    @DisplayName("여러 사용자 인증번호 생성 - 각각 고유한 키 사용")
    void generateAuthCode_MultipleUsers() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String loginId1 = "user1@example.com";
        String loginId2 = "user2@example.com";
        String authKey1 = "email:auth:" + loginId1;
        String authKey2 = "email:auth:" + loginId2;

        doNothing().when(valueOperations).set(eq(authKey1), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        doNothing().when(valueOperations).set(eq(authKey2), anyString(), eq(5L), eq(TimeUnit.MINUTES));

        // when
        String code1 = emailAuthService.generateAndSaveAuthCode(loginId1);
        String code2 = emailAuthService.generateAndSaveAuthCode(loginId2);

        // then
        assertThat(code1).isNotNull().hasSize(6);
        assertThat(code2).isNotNull().hasSize(6);
        assertThat(code1).isNotEqualTo(code2); // 대부분의 경우 다른 코드가 생성됨

        verify(valueOperations).set(eq(authKey1), eq(code1), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq(authKey2), eq(code2), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("인증 성공 후 성공 상태 확인 플로우")
    void authenticationFlow_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String inputCode = "123456";

        // 1. 인증번호 검증
        given(valueOperations.get(authKey)).willReturn(inputCode);
        doNothing().when(valueOperations).set(eq(successKey), eq("true"), eq(5L), eq(TimeUnit.MINUTES));

        // 2. 성공 상태 확인
        given(valueOperations.get(successKey)).willReturn("true");

        // when
        boolean verifyResult = emailAuthService.verifyAuthCode(loginId, inputCode);
        boolean isVerified = emailAuthService.isEmailVerified(loginId);

        // then
        assertThat(verifyResult).isTrue();
        assertThat(isVerified).isTrue();

        verify(valueOperations).get(authKey);
        verify(valueOperations).set(successKey, "true", 5L, TimeUnit.MINUTES);
        verify(valueOperations).get(successKey);
    }

    @Test
    @DisplayName("비밀번호 검증 성공 표시 저장")
    void markPasswordVerified_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        doNothing().when(valueOperations).set(eq(successKey), eq("true"), eq(5L), eq(TimeUnit.MINUTES));

        // when
        emailAuthService.markPasswordVerified(loginId);

        // then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(successKey, "true", 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("비밀번호 검증 성공 후 성공 상태 확인 플로우")
    void passwordVerificationFlow_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // 1. 비밀번호 검증 성공 표시
        doNothing().when(valueOperations).set(eq(successKey), eq("true"), eq(5L), eq(TimeUnit.MINUTES));

        // 2. 성공 상태 확인
        given(valueOperations.get(successKey)).willReturn("true");

        // when
        emailAuthService.markPasswordVerified(loginId);
        boolean isVerified = emailAuthService.isEmailVerified(loginId);

        // then
        assertThat(isVerified).isTrue();

        verify(valueOperations).set(successKey, "true", 5L, TimeUnit.MINUTES);
        verify(valueOperations).get(successKey);
    }
}