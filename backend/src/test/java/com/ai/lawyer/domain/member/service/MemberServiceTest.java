package com.ai.lawyer.domain.member.service;

import com.ai.lawyer.domain.member.dto.*;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.email.service.EmailService;
import com.ai.lawyer.global.email.service.EmailAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 테스트")
class MemberServiceTest {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceTest.class);

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuth2MemberRepository oauth2MemberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailAuthService emailAuthService;

    @Mock
    private HttpServletResponse response;

    private MemberService memberService;

    private MemberSignupRequest signupRequest;
    private MemberLoginRequest loginRequest;
    private Member member;

    // 자주 중복되는 로그 메시지만 상수로 관리
    private static final String MOCK_VERIFICATION_START_LOG = "Mock 호출 검증 시작";

    @BeforeEach
    void setUp() {
        // MemberService 생성
        memberService = new MemberService(
                memberRepository,
                passwordEncoder,
                tokenProvider,
                cookieUtil,
                emailService,
                emailAuthService
        );
        memberService.setOauth2MemberRepository(oauth2MemberRepository);

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

        member = Member.builder()
                .memberId(1L)
                .loginId("test@example.com")
                .password("encodedPassword")
                .age(25)
                .gender(Member.Gender.MALE)
                .name("테스트")
                .role(Member.Role.USER)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        given(memberRepository.existsByLoginId(signupRequest.getLoginId())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.getPassword())).willReturn("encodedPassword");
        given(memberRepository.save(any(Member.class))).willReturn(member);

        MemberResponse result = memberService.signup(signupRequest, response);

        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("test@example.com");
        assertThat(result.getAge()).isEqualTo(25);
        assertThat(result.getGender()).isEqualTo(Member.Gender.MALE);
        assertThat(result.getName()).isEqualTo("테스트");
        assertThat(result.getRole()).isEqualTo(Member.Role.USER);

        verify(memberRepository).existsByLoginId(signupRequest.getLoginId());
        verify(passwordEncoder).encode(signupRequest.getPassword());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_DuplicateEmail() {
        // given
        log.info("=== 회원가입 실패(이메일 중복) 테스트 시작 ===");
        log.info("중복 이메일: {}", signupRequest.getLoginId());

        given(memberRepository.existsByLoginId(signupRequest.getLoginId())).willReturn(true);
        log.info("Mock 설정: 이메일 중복 상황 시뮬레이션");

        // when and then
        log.info("예외 발생 검증 시작");
        assertThatThrownBy(() -> {
            log.info("회원가입 시도 중... (실패 예상)");
            memberService.signup(signupRequest, response);
        })
                .as("이메일 중복 시 IllegalArgumentException 발생")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 이메일입니다.");
        log.info("예상된 예외 발생 확인");

        log.info("호출 검증: 이메일 중복으로 인한 early return 확인");
        verify(memberRepository).existsByLoginId(signupRequest.getLoginId());
        verify(passwordEncoder, never()).encode(anyString());
        log.info("비밀번호 인코딩 호출되지 않음");
        verify(memberRepository, never()).save(any(Member.class));
        log.info("회원 저장 호출되지 않음");
        log.info("=== 회원가입 실패(이메일 중복) 테스트 완료 ===");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        log.info("=== 로그인 성공 테스트 시작 ===");
        log.info("로그인 시도: 이메일={}", loginRequest.getLoginId());

        given(memberRepository.findByLoginId(loginRequest.getLoginId())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())).willReturn(true);
        given(tokenProvider.generateAccessToken(member)).willReturn("accessToken");
        given(tokenProvider.generateRefreshToken(member)).willReturn("refreshToken");
        log.info("Mock 설정 완료: 회원 존재, 비밀번호 일치, 토큰 생성 준비");

        // when
        log.info("로그인 서비스 호출 중...");
        MemberResponse result = memberService.login(loginRequest, response);
        log.info("로그인 완료: 회원 ID={}", result.getMemberId());

        // then
        log.info("검증 시작: 로그인 결과 확인");
        assertThat(result).as("로그인 결과가 null이 아님").isNotNull();
        assertThat(result.getLoginId()).as("로그인된 회원의 이메일 일치").isEqualTo("test@example.com");
        log.info("로그인 결과 검증 완료");

        log.info(MOCK_VERIFICATION_START_LOG);
        verify(memberRepository).findByLoginId(loginRequest.getLoginId());
        log.info("회원 조회 호출 확인");
        verify(passwordEncoder).matches(loginRequest.getPassword(), member.getPassword());
        log.info("비밀번호 검증 호출 확인");
        verify(tokenProvider).generateAccessToken(member);
        log.info("액세스 토큰 생성 호출 확인");
        verify(tokenProvider).generateRefreshToken(member);
        log.info("리프레시 토큰 생성 호출 확인");
        verify(cookieUtil).setTokenCookies(response, "accessToken", "refreshToken");
        log.info("쿠키 설정 호출 확인");
        log.info("=== 로그인 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 회원")
    void login_Fail_MemberNotFound() {
        // given
        given(memberRepository.findByLoginId(loginRequest.getLoginId())).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.login(loginRequest, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberRepository).findByLoginId(loginRequest.getLoginId());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenProvider, never()).generateAccessToken(any());
        verify(tokenProvider, never()).generateRefreshToken(any());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_PasswordMismatch() {
        // given
        given(memberRepository.findByLoginId(loginRequest.getLoginId())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())).willReturn(false);

        // when and then
        assertThatThrownBy(() -> memberService.login(loginRequest, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");

        verify(memberRepository).findByLoginId(loginRequest.getLoginId());
        verify(passwordEncoder).matches(loginRequest.getPassword(), member.getPassword());
        verify(tokenProvider, never()).generateAccessToken(any());
        verify(tokenProvider, never()).generateRefreshToken(any());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_Success() {
        // given
        log.info("=== 토큰 재발급 성공 테스트 시작 ===");
        String refreshToken = "validRefreshToken";
        log.info("리프레시 토큰: {}", refreshToken);

        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn("test@example.com");
        given(tokenProvider.validateRefreshToken("test@example.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("test@example.com")).willReturn(Optional.of(member));
        given(tokenProvider.generateAccessToken(member)).willReturn("newAccessToken");
        given(tokenProvider.generateRefreshToken(member)).willReturn("newRefreshToken");
        log.info("Mock 설정 완료: Redis 토큰 유효, 회원 존재, 새 토큰 생성 준비");

        // when
        log.info("토큰 재발급 서비스 호출 중...");
        MemberResponse result = memberService.refreshToken(refreshToken, response);
        log.info("토큰 재발급 완료: 회원 ID={}", result.getMemberId());

        // then
        log.info("검증 시작: 토큰 재발급 결과 확인");
        assertThat(result).as("토큰 재발급 결과가 null이 아님").isNotNull();
        assertThat(result.getLoginId()).as("재발급된 토큰의 회원 이메일 일치").isEqualTo("test@example.com");
        log.info("토큰 재발급 결과 검증 완료");

        log.info("{}: RTR(Refresh Token Rotation) 패턴 및 Redis 검증", MOCK_VERIFICATION_START_LOG);
        verify(tokenProvider).findUsernameByRefreshToken(refreshToken);
        log.info("1단계: Redis에서 refreshToken 으로 사용자명 찾기");
        verify(tokenProvider).validateRefreshToken("test@example.com", refreshToken);
        log.info("2단계: Redis에서 리프레시 토큰 유효성 검증");
        verify(memberRepository).findByLoginId("test@example.com");
        log.info("3단계: 회원 정보 조회");
        verify(tokenProvider).deleteAllTokens("test@example.com");
        log.info("4단계: 기존 모든 토큰 Redis에서 삭제 (RTR 패턴)");
        verify(tokenProvider).generateAccessToken(member);
        log.info("5단계: 새 액세스 토큰 생성");
        verify(tokenProvider).generateRefreshToken(member);
        log.info("6단계: 새 리프레시 토큰 생성 후 Redis에 저장 (RTR 패턴)");
        verify(cookieUtil).setTokenCookies(response, "newAccessToken", "newRefreshToken");
        log.info("7단계: 새 토큰들을 쿠키로 설정");
        log.info("=== 토큰 재발급 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 리프레시 토큰")
    void refreshToken_Fail_InvalidToken() {
        // given
        String refreshToken = "invalidRefreshToken";
        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn(null);

        // when and then
        assertThatThrownBy(() -> memberService.refreshToken(refreshToken, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다.");

        verify(tokenProvider).findUsernameByRefreshToken(refreshToken);
        verify(tokenProvider, never()).validateRefreshToken(anyString(), anyString());
        verify(memberRepository, never()).findByLoginId(anyString());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void withdraw_Success() {
        // given
        String loginId = "test@example.com";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));

        // when
        memberService.deleteMember(loginId);

        // then
        verify(memberRepository).findByLoginId(loginId);
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 존재하지 않는 회원")
    void withdraw_Fail_MemberNotFound() {
        // given
        String loginId = "nonexistent@example.com";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        // when
        memberService.deleteMember(loginId);  // 존재하지 않아도 예외 발생하지 않음 (로그만 출력)

        // then
        verify(memberRepository).findByLoginId(loginId);
        verify(memberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("회원 조회 성공")
    void getMemberById_Success() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        MemberResponse result = memberService.getMemberById(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getLoginId()).isEqualTo("test@example.com");

        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("회원 조회 실패 - 존재하지 않는 회원")
    void getMemberById_Fail_MemberNotFound() {
        // given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.getMemberById(memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("로그아웃 성공 - Redis 토큰 삭제 및 쿠키 클리어")
    void logout_Success() {
        // given
        log.info("=== 로그아웃 성공 테스트 시작 ===");
        String loginId = "test@example.com";
        log.info("로그아웃 대상 사용자: {}", loginId);

        // when
        log.info("로그아웃 서비스 호출 중...");
        memberService.logout(loginId, response);
        log.info("로그아웃 완료");

        // then
        log.info("검증 시작: Redis 토큰 삭제 및 쿠키 클리어 확인");
        verify(tokenProvider).deleteAllTokens(loginId);
        log.info("Redis에서 모든 토큰 삭제 호출 확인");
        verify(cookieUtil).clearTokenCookies(response);
        log.info("쿠키에서 토큰 클리어 호출 확인");
        log.info("=== 로그아웃 성공 테스트 완료 ===");
    }

    // ===== 이메일 인증 관련 테스트 =====

    @Test
    @DisplayName("이메일 인증번호 전송 성공")
    void sendCodeToEmailByLoginId_Success() {
        // given
        log.info("=== 이메일 인증번호 전송 성공 테스트 시작 ===");
        String loginId = "test@example.com";
        log.info("인증번호 전송 대상: {}", loginId);

        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        doNothing().when(emailService).sendVerificationCode(loginId, loginId);
        log.info("Mock 설정 완료: 회원 존재, 이메일 서비스 준비");

        // when
        log.info("이메일 인증번호 전송 서비스 호출 중...");
        memberService.sendCodeToEmailByLoginId(loginId);
        log.info("이메일 인증번호 전송 완료");

        // then
        log.info("검증 시작: 회원 조회 및 이메일 전송 확인");
        verify(memberRepository).findByLoginId(loginId);
        log.info("회원 존재 여부 조회 호출 확인");
        verify(emailService).sendVerificationCode(loginId, loginId);
        log.info("이메일 인증번호 전송 서비스 호출 확인");
        log.info("=== 이메일 인증번호 전송 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("이메일 인증번호 전송 실패 - 존재하지 않는 회원")
    void sendCodeToEmailByLoginId_Fail_MemberNotFound() {
        // given
        String loginId = "nonexistent@example.com";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.sendCodeToEmailByLoginId(loginId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 로그인 ID의 회원이 없습니다.");

        verify(memberRepository).findByLoginId(loginId);
        verify(emailService, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증번호 검증 성공")
    void verifyAuthCode_Success() {
        // given
        log.info("=== 이메일 인증번호 검증 성공 테스트 시작 ===");
        String loginId = "test@example.com";
        String verificationCode = "123456";
        log.info("인증번호 검증: 이메일={}, 코드={}", loginId, verificationCode);

        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(emailAuthService.verifyAuthCode(loginId, verificationCode)).willReturn(true);
        log.info("Mock 설정 완료: 회원 존재, 인증번호 일치");

        // when
        log.info("이메일 인증번호 검증 서비스 호출 중...");
        boolean result = memberService.verifyAuthCode(loginId, verificationCode);
        log.info("이메일 인증번호 검증 완료: 결과={}", result);

        // then
        log.info("검증 시작: 인증번호 검증 결과 확인");
        assertThat(result).as("인증번호 검증 성공").isTrue();
        verify(memberRepository).findByLoginId(loginId);
        log.info("회원 존재 여부 조회 호출 확인");
        verify(emailAuthService).verifyAuthCode(loginId, verificationCode);
        log.info("이메일 인증번호 검증 서비스 호출 확인");
        log.info("=== 이메일 인증번호 검증 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("이메일 인증번호 검증 실패 - 잘못된 인증번호")
    void verifyAuthCode_Fail_InvalidCode() {
        // given
        String loginId = "test@example.com";
        String verificationCode = "999999";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(emailAuthService.verifyAuthCode(loginId, verificationCode)).willReturn(false);

        // when
        boolean result = memberService.verifyAuthCode(loginId, verificationCode);

        // then
        assertThat(result).as("잘못된 인증번호로 검증 실패").isFalse();
        verify(memberRepository).findByLoginId(loginId);
        verify(emailAuthService).verifyAuthCode(loginId, verificationCode);
    }

    @Test
    @DisplayName("이메일 인증번호 검증 실패 - 존재하지 않는 회원")
    void verifyAuthCode_Fail_MemberNotFound() {
        // given
        String loginId = "nonexistent@example.com";
        String verificationCode = "123456";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.verifyAuthCode(loginId, verificationCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberRepository).findByLoginId(loginId);
        verify(emailAuthService, never()).verifyAuthCode(anyString(), anyString());
    }

    // ===== 비밀번호 재설정 관련 테스트 =====

    @Test
    @DisplayName("비밀번호 재설정 성공 - 클라이언트 success와 Redis 인증 모두 확인")
    void resetPassword_Success() {
        // given
        log.info("=== 비밀번호 재설정 성공 테스트 시작 ===");
        String loginId = "test@example.com";
        String newPassword = "newPassword123";
        Boolean success = true;
        log.info("비밀번호 재설정: 이메일={}, 인증성공={}", loginId, success);

        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(emailAuthService.isEmailVerified(loginId)).willReturn(true); // Redis 인증 성공
        given(passwordEncoder.encode(newPassword)).willReturn("encodedNewPassword");
        doNothing().when(tokenProvider).deleteAllTokens(loginId);
        doNothing().when(emailAuthService).clearAuthData(loginId);
        log.info("Mock 설정 완료: 회원 존재, 클라이언트 인증 성공, Redis 인증 성공, 비밀번호 인코딩 준비");

        // when
        log.info("비밀번호 재설정 서비스 호출 중...");
        memberService.resetPassword(loginId, newPassword, success);
        log.info("비밀번호 재설정 완료");

        // then
        log.info("검증 시작: 비밀번호 재설정 프로세스 확인");
        verify(memberRepository).findByLoginId(loginId);
        log.info("회원 존재 여부 조회 호출 확인");
        verify(emailAuthService).isEmailVerified(loginId);
        log.info("Redis 이메일 인증 성공 여부 확인 호출 확인");
        verify(passwordEncoder).encode(newPassword);
        log.info("새 비밀번호 인코딩 호출 확인");
        verify(memberRepository).save(member);
        log.info("회원 정보 저장 호출 확인");
        verify(emailAuthService).clearAuthData(loginId);
        log.info("인증 데이터 삭제 호출 확인");
        verify(tokenProvider).deleteAllTokens(loginId);
        log.info("기존 모든 토큰 삭제 호출 확인 (보안상 로그아웃 처리)");
        log.info("=== 비밀번호 재설정 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 클라이언트 success = false")
    void resetPassword_Fail_ClientSuccessFalse() {
        // given
        String loginId = "test@example.com";
        String newPassword = "newPassword123";
        Boolean success = false;
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        // Redis Mock 설정 제거 - 클라이언트 success가 false면 Redis 확인 안함

        // when and then
        assertThatThrownBy(() -> memberService.resetPassword(loginId, newPassword, success))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다.");

        verify(memberRepository).findByLoginId(loginId);
        // 클라이언트 success가 false이면 Redis 확인 전에 실패
        verify(emailAuthService, never()).isEmailVerified(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).save(any(Member.class));
        verify(tokenProvider, never()).deleteAllTokens(anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - Redis 인증되지 않음")
    void resetPassword_Fail_RedisNotVerified() {
        // given
        String loginId = "test@example.com";
        String newPassword = "newPassword123";
        Boolean success = true;
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(emailAuthService.isEmailVerified(loginId)).willReturn(false); // Redis 인증 실패

        // when and then
        assertThatThrownBy(() -> memberService.resetPassword(loginId, newPassword, success))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다.");

        verify(memberRepository).findByLoginId(loginId);
        verify(emailAuthService).isEmailVerified(loginId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).save(any(Member.class));
        verify(tokenProvider, never()).deleteAllTokens(anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - success = null")
    void resetPassword_Fail_NullSuccess() {
        // given
        String loginId = "test@example.com";
        String newPassword = "newPassword123";
        Boolean success = null;
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));

        // when and then
        assertThatThrownBy(() -> memberService.resetPassword(loginId, newPassword, success))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 인증을 완료해야 비밀번호를 재설정할 수 있습니다.");

        verify(memberRepository).findByLoginId(loginId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).save(any(Member.class));
        verify(tokenProvider, never()).deleteAllTokens(anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 존재하지 않는 회원")
    void resetPassword_Fail_MemberNotFound() {
        // given
        String loginId = "nonexistent@example.com";
        String newPassword = "newPassword123";
        Boolean success = true;
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.resetPassword(loginId, newPassword, success))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberRepository).findByLoginId(loginId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).save(any(Member.class));
        verify(tokenProvider, never()).deleteAllTokens(anyString());
    }

    @Test
    @DisplayName("JWT 토큰에서 loginId 추출 성공")
    void extractLoginIdFromToken_Success() {
        // given
        String token = "valid.jwt.token";
        String expectedLoginId = "test@example.com";
        given(tokenProvider.getLoginIdFromToken(token)).willReturn(expectedLoginId);

        // when
        String result = memberService.extractLoginIdFromToken(token);

        // then
        assertThat(result).isEqualTo(expectedLoginId);
        verify(tokenProvider).getLoginIdFromToken(token);
    }

    @Test
    @DisplayName("JWT 토큰에서 loginId 추출 실패 - 유효하지 않은 토큰")
    void extractLoginIdFromToken_Fail_InvalidToken() {
        // given
        String token = "invalid.jwt.token";
        given(tokenProvider.getLoginIdFromToken(token)).willReturn(null);

        // when
        String result = memberService.extractLoginIdFromToken(token);

        // then
        assertThat(result).isNull();
        verify(tokenProvider).getLoginIdFromToken(token);
    }

    @Test
    @DisplayName("비밀번호 검증 성공")
    void verifyPassword_Success() {
        // given
        log.info("=== 비밀번호 검증 성공 테스트 시작 ===");
        String loginId = "test@example.com";
        String password = "password123";
        log.info("비밀번호 검증: 이메일={}", loginId);

        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(password, member.getPassword())).willReturn(true);
        doNothing().when(emailAuthService).markPasswordVerified(loginId);
        log.info("Mock 설정 완료: 회원 존재, 비밀번호 일치");

        // when
        log.info("비밀번호 검증 서비스 호출 중...");
        boolean result = memberService.verifyPassword(loginId, password);
        log.info("비밀번호 검증 완료: 결과={}", result);

        // then
        log.info("검증 시작: 비밀번호 검증 결과 확인");
        assertThat(result).as("비밀번호 검증 성공").isTrue();
        verify(memberRepository).findByLoginId(loginId);
        log.info("회원 존재 여부 조회 호출 확인");
        verify(passwordEncoder).matches(password, member.getPassword());
        log.info("비밀번호 일치 여부 검증 호출 확인");
        verify(emailAuthService).markPasswordVerified(loginId);
        log.info("Redis에 비밀번호 검증 성공 표시 저장 호출 확인");
        log.info("=== 비밀번호 검증 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 비밀번호 불일치")
    void verifyPassword_Fail_PasswordMismatch() {
        // given
        String loginId = "test@example.com";
        String password = "wrongPassword";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(password, member.getPassword())).willReturn(false);

        // when
        boolean result = memberService.verifyPassword(loginId, password);

        // then
        assertThat(result).as("비밀번호 불일치로 검증 실패").isFalse();
        verify(memberRepository).findByLoginId(loginId);
        verify(passwordEncoder).matches(password, member.getPassword());
        verify(emailAuthService, never()).markPasswordVerified(anyString());
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 존재하지 않는 회원")
    void verifyPassword_Fail_MemberNotFound() {
        // given
        String loginId = "nonexistent@example.com";
        String password = "password123";
        given(memberRepository.findByLoginId(loginId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.verifyPassword(loginId, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberRepository).findByLoginId(loginId);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}