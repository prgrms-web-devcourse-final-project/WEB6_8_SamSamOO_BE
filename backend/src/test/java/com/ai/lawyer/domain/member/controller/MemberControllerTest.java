package com.ai.lawyer.domain.member.controller;

import com.ai.lawyer.domain.member.dto.*;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.service.MemberService;
import com.ai.lawyer.domain.member.exception.MemberAuthenticationException;
import com.ai.lawyer.domain.member.exception.MemberExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberController 테스트")
class MemberControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private MemberService memberService;

    @Mock
    private HttpServletResponse response;

    @Mock
    private com.ai.lawyer.global.oauth.OAuth2LogoutService oauth2LogoutService;

    @InjectMocks
    private MemberController memberController;

    private MemberSignupRequest signupRequest;
    private MemberLoginRequest loginRequest;
    private MemberResponse memberResponse;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new MemberExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        signupRequest = MemberSignupRequest.builder()
                .loginId("test@example.com")
                .password("password123")
                .age(25)
                .gender(Member.Gender.MALE)
                .name("테스트")
                .build();

        loginRequest = MemberLoginRequest.builder()
                .loginId("test@example.com")
                .password("password123")
                .build();

        memberResponse = MemberResponse.builder()
                .memberId(1L)
                .loginId("test@example.com")
                .age(25)
                .gender(Member.Gender.MALE)
                .name("테스트")
                .role(Member.Role.USER)
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                1L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ) {
            @Override
            public String getName() {
                return "test@example.com";
            }
        };
        ((UsernamePasswordAuthenticationToken) authentication).setDetails("test@example.com");
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() throws Exception {
        // given
        given(memberService.signup(any(MemberSignupRequest.class), any(HttpServletResponse.class))).willReturn(memberResponse);

        // when and then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.loginId").value("test@example.com"))
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(memberService).signup(any(MemberSignupRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_DuplicateEmail() throws Exception {
        given(memberService.signup(any(MemberSignupRequest.class), any(HttpServletResponse.class)))
                .willThrow(new IllegalArgumentException("이미 존재하는 이메일입니다."));

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService).signup(any(MemberSignupRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검증 실패")
    void signup_Fail_ValidationError() throws Exception {
        // given
        MemberSignupRequest invalidRequest = MemberSignupRequest.builder()
                .loginId("invalid-email") // 잘못된 이메일 형식
                .password("123") // 너무 짧은 비밀번호
                .age(10) // 최소 나이 미만
                .gender(Member.Gender.MALE)
                .name("")  // 빈 이름
                .build();

        // when and then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService, never()).signup(any(MemberSignupRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // given
        given(memberService.login(any(MemberLoginRequest.class), any()))
                .willReturn(memberResponse);

        // when and then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.loginId").value("test@example.com"));

        verify(memberService).login(any(MemberLoginRequest.class), any());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 회원")
    void login_Fail_MemberNotFound() throws Exception {
        given(memberService.login(any(MemberLoginRequest.class), any()))
                .willThrow(new IllegalArgumentException("존재하지 않는 회원입니다."));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(memberService).login(any(MemberLoginRequest.class), any());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_PasswordMismatch() throws Exception {
        given(memberService.login(any(MemberLoginRequest.class), any()))
                .willThrow(new IllegalArgumentException("비밀번호가 일치하지 않습니다."));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(memberService).login(any(MemberLoginRequest.class), any());
    }

    @Test
    @DisplayName("로그아웃 성공 - Authentication에서 loginId 추출하여 Redis 삭제")
    void logout_Success() {
        // given
        given(oauth2LogoutService.logoutFromOAuth2Provider(eq("test@example.com"))).willReturn(false);
        given(oauth2LogoutService.getOAuth2LogoutUrl(eq("test@example.com"))).willReturn(null);
        doNothing().when(memberService).logout(eq("test@example.com"), eq(response));

        // when
        ResponseEntity<LogoutResponse> result = memberController.logout(authentication, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(oauth2LogoutService).logoutFromOAuth2Provider(eq("test@example.com"));
        verify(oauth2LogoutService).getOAuth2LogoutUrl(eq("test@example.com"));
        verify(memberService).logout(eq("test@example.com"), eq(response));
    }

    @Test
    @DisplayName("로그아웃 성공 - 인증되지 않은 상태에서도 쿠키 클리어")
    void logout_Success_Unauthenticated() {
        // given
        given(oauth2LogoutService.getOAuth2LogoutUrl(null)).willReturn(null);
        doNothing().when(memberService).logout(eq(""), eq(response));

        // when
        ResponseEntity<LogoutResponse> result = memberController.logout(null, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(oauth2LogoutService).getOAuth2LogoutUrl(null);
        verify(memberService).logout(eq(""), eq(response));
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 쿠키에서 리프레시 토큰 추출")
    void refreshToken_Success() throws Exception {
        // given
        String refreshTokenValue = "validRefreshToken";
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshTokenValue);

        given(memberService.refreshToken(eq(refreshTokenValue), any(HttpServletResponse.class))).willReturn(memberResponse);

        // when
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.loginId").value("test@example.com"));

        // then
        verify(memberService).refreshToken(eq(refreshTokenValue), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰 없음")
    void refreshToken_Fail_NoRefreshToken() throws Exception {
        // given - 쿠키 없이 요청

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(memberService, never()).refreshToken(anyString(), any());
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void withdraw_Success() {
        // given
        given(oauth2LogoutService.unlinkFromOAuth2Provider(eq("test@example.com"))).willReturn(false);
        doNothing().when(memberService).logout(eq("test@example.com"), eq(response));
        doNothing().when(memberService).deleteMember(eq("test@example.com"));

        // when
        ResponseEntity<Map<String, Object>> result = memberController.withdraw(authentication, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(oauth2LogoutService).unlinkFromOAuth2Provider(eq("test@example.com"));
        verify(memberService).logout(eq("test@example.com"), eq(response));
        verify(memberService).deleteMember(eq("test@example.com"));
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 인증되지 않은 사용자")
    void withdraw_Fail_Unauthenticated() {
        // when
        ResponseEntity<Map<String, Object>> result = memberController.withdraw(null, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("success")).isEqualTo(false);
        assertThat(result.getBody().get("message")).isEqualTo("인증이 필요합니다.");

        verify(oauth2LogoutService, never()).unlinkFromOAuth2Provider(anyString());
        verify(memberService, never()).deleteMember(anyString());
        verify(memberService, never()).logout(anyString(), any());
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 존재하지 않는 회원")
    void withdraw_Fail_MemberNotFound() {
        // given
        given(oauth2LogoutService.unlinkFromOAuth2Provider(eq("test@example.com"))).willReturn(false);
        doNothing().when(memberService).logout(eq("test@example.com"), eq(response));
        doThrow(new IllegalArgumentException("존재하지 않는 회원입니다."))
                .when(memberService).deleteMember(eq("test@example.com"));

        // when & then
        assertThatThrownBy(() -> memberController.withdraw(authentication, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(oauth2LogoutService).unlinkFromOAuth2Provider(eq("test@example.com"));
        verify(memberService).logout(eq("test@example.com"), eq(response));
        verify(memberService).deleteMember(eq("test@example.com"));
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_Success() {
        // given - 현재 Controller는 직접 memberId를 사용
        given(memberService.getMemberById(1L)).willReturn(memberResponse);

        // when
        ResponseEntity<MemberResponse> result = memberController.getMyInfo(authentication);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(memberResponse);
        verify(memberService).getMemberById(1L);
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 인증되지 않은 사용자")
    void getMyInfo_Fail_Unauthenticated() {
        // when & then
        assertThatThrownBy(() -> memberController.getMyInfo(null))
                .isInstanceOf(MemberAuthenticationException.class)
                .hasMessage("인증이 필요합니다.");

        verify(memberService, never()).getMemberById(anyLong());
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 존재하지 않는 회원")
    void getMyInfo_Fail_MemberNotFound() {
        // given
        given(memberService.getMemberById(1L))
                .willThrow(new IllegalArgumentException("존재하지 않는 회원입니다."));

        // when & then
        assertThatThrownBy(() -> memberController.getMyInfo(authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberService).getMemberById(1L);
    }

    // ===== 이메일 인증 관련 테스트 =====

    @Test
    @DisplayName("이메일 전송 성공 - 비로그인 사용자 (요청 바디에서 loginId 추출)")
    void sendEmail_Success_NonLoggedInUser() throws Exception {
        // given
        MemberEmailRequestDto requestDto = new MemberEmailRequestDto();
        requestDto.setLoginId("test@example.com");
        doNothing().when(memberService).sendCodeToEmailByLoginId("test@example.com");

        // when and then
        mockMvc.perform(post("/api/auth/sendEmail")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 전송 성공"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.success").value(true));

        verify(memberService).sendCodeToEmailByLoginId("test@example.com");
    }

    @Test
    @DisplayName("이메일 전송 실패 - 존재하지 않는 회원")
    void sendEmail_Fail_MemberNotFound() throws Exception {
        // given
        MemberEmailRequestDto requestDto = new MemberEmailRequestDto();
        requestDto.setLoginId("nonexistent@example.com");
        doThrow(new IllegalArgumentException("해당 로그인 ID의 회원이 없습니다."))
                .when(memberService).sendCodeToEmailByLoginId("nonexistent@example.com");

        // when and then
        mockMvc.perform(post("/api/auth/sendEmail")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService).sendCodeToEmailByLoginId("nonexistent@example.com");
    }

    @Test
    @DisplayName("이메일 인증번호 검증 성공 - 비로그인 사용자")
    void verifyEmail_Success_NonLoggedInUser() throws Exception {
        // given
        given(memberService.verifyAuthCode("test@example.com", "123456")).willReturn(true);

        EmailVerifyCodeRequestDto requestDto = new EmailVerifyCodeRequestDto();
        requestDto.setLoginId("test@example.com");
        requestDto.setVerificationCode("123456");

        // when and then
        mockMvc.perform(post("/api/auth/verifyEmail")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증번호 검증 성공"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.success").value(true));

        verify(memberService).verifyAuthCode("test@example.com", "123456");
    }

    @Test
    @DisplayName("이메일 인증번호 검증 실패 - 잘못된 인증번호")
    void verifyEmail_Fail_InvalidCode() throws Exception {
        // given
        given(memberService.verifyAuthCode("test@example.com", "999999")).willReturn(false);

        EmailVerifyCodeRequestDto requestDto = new EmailVerifyCodeRequestDto();
        requestDto.setLoginId("test@example.com");
        requestDto.setVerificationCode("999999");

        // when and then
        mockMvc.perform(post("/api/auth/verifyEmail")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService).verifyAuthCode("test@example.com", "999999");
    }

    @Test
    @DisplayName("이메일 인증번호 검증 실패 - 유효성 검증 실패")
    void verifyEmail_Fail_ValidationError() throws Exception {
        // given
        EmailVerifyCodeRequestDto invalidRequest = new EmailVerifyCodeRequestDto();
        invalidRequest.setLoginId("test@example.com");
        invalidRequest.setVerificationCode("12345"); // 6자리가 아님

        // when and then
        mockMvc.perform(post("/api/auth/verifyEmail")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService, never()).verifyAuthCode(anyString(), anyString());
    }

    // ===== 비밀번호 재설정 관련 테스트 =====

    @Test
    @DisplayName("비밀번호 재설정 성공 - 요청 바디에서 loginId 추출")
    void resetPassword_Success_WithLoginIdInBody() throws Exception {
        // given
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        requestDto.setLoginId("test@example.com");
        requestDto.setNewPassword("newPassword123");
        requestDto.setSuccess(true);

        doNothing().when(memberService).resetPassword("test@example.com", "newPassword123", true);

        // when and then
        mockMvc.perform(post("/api/auth/passwordReset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 재설정되었습니다."))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.success").value(true));

        verify(memberService).resetPassword("test@example.com", "newPassword123", true);
        verify(memberService).logout(eq("test@example.com"), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 인증되지 않음 (success = false)")
    void resetPassword_Fail_NotAuthenticated() throws Exception {
        // given
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        requestDto.setLoginId("test@example.com");
        requestDto.setNewPassword("newPassword123");
        requestDto.setSuccess(false);

        doThrow(new IllegalArgumentException("이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다."))
                .when(memberService).resetPassword("test@example.com", "newPassword123", false);

        // when and then
        mockMvc.perform(post("/api/auth/passwordReset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService).resetPassword("test@example.com", "newPassword123", false);
        verify(memberService, never()).logout(anyString(), any());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 존재하지 않는 회원")
    void resetPassword_Fail_MemberNotFound() throws Exception {
        // given
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        requestDto.setLoginId("nonexistent@example.com");
        requestDto.setNewPassword("newPassword123");
        requestDto.setSuccess(true);

        doThrow(new IllegalArgumentException("존재하지 않는 회원입니다."))
                .when(memberService).resetPassword("nonexistent@example.com", "newPassword123", true);

        // when and then
        mockMvc.perform(post("/api/auth/passwordReset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(memberService).resetPassword("nonexistent@example.com", "newPassword123", true);
        verify(memberService, never()).logout(anyString(), any());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - loginId 없음")
    void resetPassword_Fail_NoLoginId() throws Exception {
        // given - loginId 없이 요청
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto();
        requestDto.setNewPassword("newPassword123");
        requestDto.setSuccess(true);
        // loginId는 설정하지 않음

        // when and then - loginId가 없으면 예외가 발생해야 함
        mockMvc.perform(post("/api/auth/passwordReset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService, never()).resetPassword(anyString(), anyString(), any());
        verify(memberService, never()).logout(anyString(), any());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 유효성 검증 실패")
    void resetPassword_Fail_ValidationError() throws Exception {
        // given
        ResetPasswordRequestDto invalidRequest = new ResetPasswordRequestDto();
        // loginId는 이제 optional이므로 빈 문자열 사용 (null은 허용)
        invalidRequest.setNewPassword(""); // 빈 비밀번호
        invalidRequest.setSuccess(null); // null success

        // when and then
        mockMvc.perform(post("/api/auth/passwordReset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService, never()).resetPassword(anyString(), anyString(), any());
        verify(memberService, never()).logout(anyString(), any());
    }

    @Test
    @DisplayName("비밀번호 검증 성공 - 로그인된 사용자")
    void verifyPassword_Success_LoggedInUser() throws Exception {
        // given
        PasswordVerifyRequestDto requestDto = new PasswordVerifyRequestDto();
        requestDto.setPassword("password123");

        given(memberService.extractLoginIdFromToken(anyString())).willReturn("test@example.com");
        given(memberService.verifyPassword("test@example.com", "password123")).willReturn(true);

        // when and then - with() 사용해 Authentication 주입
        mockMvc.perform(post("/api/auth/verifyPassword")
                        .with(csrf())
                        .with(request -> {
                            request.setAttribute("org.springframework.security.web.authentication.WebAuthenticationDetails.REMOTE_ADDRESS", "127.0.0.1");
                            return request;
                        })
                        .principal(authentication)
                        .header("Authorization", "Bearer validAccessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호 검증 성공"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.success").value(true));

        verify(memberService).extractLoginIdFromToken(anyString());
        verify(memberService).verifyPassword("test@example.com", "password123");
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 비밀번호 불일치")
    void verifyPassword_Fail_PasswordMismatch() throws Exception {
        // given
        PasswordVerifyRequestDto requestDto = new PasswordVerifyRequestDto();
        requestDto.setPassword("wrongPassword");

        given(memberService.extractLoginIdFromToken(anyString())).willReturn("test@example.com");
        given(memberService.verifyPassword("test@example.com", "wrongPassword")).willReturn(false);

        // when and then
        mockMvc.perform(post("/api/auth/verifyPassword")
                        .with(csrf())
                        .principal(authentication)
                        .header("Authorization", "Bearer validAccessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService).extractLoginIdFromToken(anyString());
        verify(memberService).verifyPassword("test@example.com", "wrongPassword");
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 로그인되지 않은 사용자")
    void verifyPassword_Fail_NotLoggedIn() throws Exception {
        // given
        PasswordVerifyRequestDto requestDto = new PasswordVerifyRequestDto();
        requestDto.setPassword("password123");

        // when and then - Authorization 헤더 없이 요청
        mockMvc.perform(post("/api/auth/verifyPassword")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService, never()).extractLoginIdFromToken(anyString());
        verify(memberService, never()).verifyPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 검증 성공 - 로그인된 사용자도 가능")
    void verifyEmail_Success_LoggedInUser() throws Exception {
        // given
        EmailVerifyCodeRequestDto requestDto = new EmailVerifyCodeRequestDto();
        requestDto.setLoginId("test@example.com");
        requestDto.setVerificationCode("123456");

        given(memberService.verifyAuthCode("test@example.com", "123456")).willReturn(true);

        // when and then - principal과 함께 요청 (로그인 상태)
        mockMvc.perform(post("/api/auth/verifyEmail")
                        .with(csrf())
                        .principal(authentication)
                        .header("Authorization", "Bearer validAccessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증번호 검증 성공"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.success").value(true));

        verify(memberService).verifyAuthCode("test@example.com", "123456");
    }

}