package com.ai.lawyer.domain.member.controller;

import com.ai.lawyer.domain.member.dto.MemberLoginRequest;
import com.ai.lawyer.domain.member.dto.MemberResponse;
import com.ai.lawyer.domain.member.dto.MemberSignupRequest;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MemberController memberController;

    private MemberSignupRequest signupRequest;
    private MemberLoginRequest loginRequest;
    private MemberResponse memberResponse;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController).build();
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
                "test@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() throws Exception {
        // given
        given(memberService.signup(any(MemberSignupRequest.class))).willReturn(memberResponse);

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

        verify(memberService).signup(any(MemberSignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_DuplicateEmail() throws Exception {
        // given
        given(memberService.signup(any(MemberSignupRequest.class)))
                .willThrow(new IllegalArgumentException("이미 존재하는 이메일입니다."));

        // when and then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(memberService).signup(any(MemberSignupRequest.class));
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

        verify(memberService, never()).signup(any(MemberSignupRequest.class));
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
        // given
        given(memberService.login(any(MemberLoginRequest.class), any()))
                .willThrow(new IllegalArgumentException("존재하지 않는 회원입니다."));

        // when and then
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
        // given
        given(memberService.login(any(MemberLoginRequest.class), any()))
                .willThrow(new IllegalArgumentException("비밀번호가 일치하지 않습니다."));

        // when and then
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
        doNothing().when(memberService).logout(eq("test@example.com"), eq(response));

        // when
        ResponseEntity<Void> result = memberController.logout(authentication, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(memberService).logout(eq("test@example.com"), eq(response));
    }

    @Test
    @DisplayName("로그아웃 성공 - 인증되지 않은 상태에서도 쿠키 클리어")
    void logout_Success_Unauthenticated() {
        // given
        doNothing().when(memberService).logout(eq(""), eq(response));

        // when
        ResponseEntity<Void> result = memberController.logout(null, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(memberService).logout(eq(""), eq(response));
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 쿠키에서 Refresh Token 추출하여 Redis 검증")
    void refreshToken_Success() {
        // given
        Cookie[] cookies = {new Cookie("refreshToken", "validRefreshToken")};
        given(request.getCookies()).willReturn(cookies);
        given(memberService.refreshToken(eq("validRefreshToken"), eq(response)))
                .willReturn(memberResponse);

        // when
        ResponseEntity<MemberResponse> result = memberController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(memberResponse);

        // 쿠키에서 refreshToken이 정상적으로 추출되어 서비스에 전달되는지 검증
        verify(memberService).refreshToken(eq("validRefreshToken"), eq(response));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰 없음")
    void refreshToken_Fail_NoRefreshToken() {
        // given
        given(request.getCookies()).willReturn(null);

        // when
        ResponseEntity<MemberResponse> result = memberController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(memberService, never()).refreshToken(anyString(), any());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refreshToken_Fail_InvalidToken() {
        // given
        Cookie[] cookies = {new Cookie("refreshToken", "invalidRefreshToken")};
        given(request.getCookies()).willReturn(cookies);
        given(memberService.refreshToken(eq("invalidRefreshToken"), eq(response)))
                .willThrow(new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

        // when
        ResponseEntity<MemberResponse> result = memberController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(memberService).refreshToken(eq("invalidRefreshToken"), eq(response));
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void withdraw_Success() {
        // given
        Member mockMember = Member.builder()
                .memberId(1L)
                .loginId("test@example.com")
                .build();
        given(memberService.findByLoginId("test@example.com")).willReturn(mockMember);
        doNothing().when(memberService).withdraw(1L);
        doNothing().when(memberService).logout(eq("test@example.com"), eq(response));

        // when
        ResponseEntity<Void> result = memberController.withdraw(authentication, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(memberService).findByLoginId("test@example.com");
        verify(memberService).withdraw(1L);
        verify(memberService).logout(eq("test@example.com"), eq(response));
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 인증되지 않은 사용자")
    void withdraw_Fail_Unauthenticated() {
        // when
        ResponseEntity<Void> result = memberController.withdraw(null, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(memberService, never()).withdraw(anyLong());
        verify(memberService, never()).logout(anyString(), any());
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 존재하지 않는 회원")
    void withdraw_Fail_MemberNotFound() {
        // given
        given(memberService.findByLoginId("test@example.com"))
                .willThrow(new IllegalArgumentException("존재하지 않는 회원입니다."));

        // when
        ResponseEntity<Void> result = memberController.withdraw(authentication, response);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberService).findByLoginId("test@example.com");
        verify(memberService, never()).withdraw(anyLong());
        verify(memberService, never()).logout(anyString(), any());
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_Success() {
        // given
        Member mockMember = Member.builder()
                .memberId(1L)
                .loginId("test@example.com")
                .build();
        given(memberService.findByLoginId("test@example.com")).willReturn(mockMember);
        given(memberService.getMemberById(1L)).willReturn(memberResponse);

        // when
        ResponseEntity<MemberResponse> result = memberController.getMyInfo(authentication);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(memberResponse);
        verify(memberService).findByLoginId("test@example.com");
        verify(memberService).getMemberById(1L);
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 인증되지 않은 사용자")
    void getMyInfo_Fail_Unauthenticated() {
        // when
        ResponseEntity<MemberResponse> result = memberController.getMyInfo(null);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(memberService, never()).getMemberById(anyLong());
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 존재하지 않는 회원")
    void getMyInfo_Fail_MemberNotFound() {
        // given
        given(memberService.findByLoginId("test@example.com"))
                .willThrow(new IllegalArgumentException("존재하지 않는 회원입니다."));

        // when
        ResponseEntity<MemberResponse> result = memberController.getMyInfo(authentication);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberService).findByLoginId("test@example.com");
        verify(memberService, never()).getMemberById(anyLong());
    }
}