package com.ai.lawyer.domain.member.service;

import com.ai.lawyer.domain.member.dto.MemberLoginRequest;
import com.ai.lawyer.domain.member.dto.MemberResponse;
import com.ai.lawyer.domain.member.dto.MemberSignupRequest;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MemberService memberService;

    private MemberSignupRequest signupRequest;
    private MemberLoginRequest loginRequest;
    private Member member;

    // 자주 중복되는 로그 메시지만 상수로 관리
    private static final String MOCK_VERIFICATION_START_LOG = "Mock 호출 검증 시작";

    @BeforeEach
    void setUp() {
        signupRequest = MemberSignupRequest.builder()
                .loginId("test@example.com")
                .password("password123")
                .nickname("tester")
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
                .nickname("tester")
                .age(25)
                .gender(Member.Gender.MALE)
                .name("테스트")
                .role(Member.Role.USER)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // given
        log.info("=== 회원가입 성공 테스트 시작 ===");
        log.info("테스트 데이터: 이메일={}, 닉네임={}", signupRequest.getLoginId(), signupRequest.getNickname());

        given(memberRepository.existsByLoginId(signupRequest.getLoginId())).willReturn(false);
        given(memberRepository.existsByNickname(signupRequest.getNickname())).willReturn(false);
        given(passwordEncoder.encode(signupRequest.getPassword())).willReturn("encodedPassword");
        given(memberRepository.save(any(Member.class))).willReturn(member);
        log.info("Mock 설정 완료: 이메일 중복 없음, 닉네임 중복 없음, 비밀번호 인코딩 성공");

        // when
        log.info("회원가입 서비스 호출 중...");
        MemberResponse result = memberService.signup(signupRequest);
        log.info("회원가입 완료: memberId={}", result.getMemberId());

        // then
        log.info("검증 시작: 반환된 회원 정보 확인");
        assertThat(result).as("회원가입 결과가 null이 아님").isNotNull();
        assertThat(result.getLoginId()).as("로그인 ID 일치").isEqualTo("test@example.com");
        assertThat(result.getNickname()).as("닉네임 일치").isEqualTo("tester");
        assertThat(result.getAge()).as("나이 일치").isEqualTo(25);
        assertThat(result.getGender()).as("성별 일치").isEqualTo(Member.Gender.MALE);
        assertThat(result.getName()).as("이름 일치").isEqualTo("테스트");
        assertThat(result.getRole()).as("기본 역할이 USER로 설정됨").isEqualTo(Member.Role.USER);
        log.info("회원 정보 검증 완료");

        log.info(MOCK_VERIFICATION_START_LOG);
        verify(memberRepository).existsByLoginId(signupRequest.getLoginId());
        log.info("이메일 중복 체크 호출 확인");
        verify(memberRepository).existsByNickname(signupRequest.getNickname());
        log.info("닉네임 중복 체크 호출 확인");
        verify(passwordEncoder).encode(signupRequest.getPassword());
        log.info("비밀번호 인코딩 호출 확인");
        verify(memberRepository).save(any(Member.class));
        log.info("회원 저장 호출 확인");
        log.info("=== 회원가입 성공 테스트 완료 ===");
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
            memberService.signup(signupRequest);
        })
                .as("이메일 중복 시 IllegalArgumentException 발생")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 이메일입니다.");
        log.info("예상된 예외 발생 확인");

        log.info("호출 검증: 이메일 중복으로 인한 early return 확인");
        verify(memberRepository).existsByLoginId(signupRequest.getLoginId());
        log.info("이메일 중복 체크 호출됨");
        verify(memberRepository, never()).existsByNickname(anyString());
        log.info("닉네임 체크는 호출되지 않음 (이메일 중복으로 early return)");
        verify(passwordEncoder, never()).encode(anyString());
        log.info("비밀번호 인코딩 호출되지 않음");
        verify(memberRepository, never()).save(any(Member.class));
        log.info("회원 저장 호출되지 않음");
        log.info("=== 회원가입 실패(이메일 중복) 테스트 완료 ===");
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signup_Fail_DuplicateNickname() {
        // given
        given(memberRepository.existsByLoginId(signupRequest.getLoginId())).willReturn(false);
        given(memberRepository.existsByNickname(signupRequest.getNickname())).willReturn(true);

        // when and then
        assertThatThrownBy(() -> memberService.signup(signupRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 닉네임입니다.");

        verify(memberRepository).existsByLoginId(signupRequest.getLoginId());
        verify(memberRepository).existsByNickname(signupRequest.getNickname());
        verify(passwordEncoder, never()).encode(anyString());
        verify(memberRepository, never()).save(any(Member.class));
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

        given(tokenProvider.validateToken(refreshToken)).willReturn(true);
        given(tokenProvider.getUsernameFromToken(refreshToken)).willReturn("test@example.com");
        given(memberRepository.findByLoginId("test@example.com")).willReturn(Optional.of(member));
        given(tokenProvider.generateAccessToken(member)).willReturn("newAccessToken");
        given(tokenProvider.generateRefreshToken(member)).willReturn("newRefreshToken");
        log.info("Mock 설정 완료: 토큰 유효, 회원 존재, 새 토큰 생성 준비");

        // when
        log.info("토큰 재발급 서비스 호출 중...");
        MemberResponse result = memberService.refreshToken(refreshToken, response);
        log.info("토큰 재발급 완료: 회원 ID={}", result.getMemberId());

        // then
        log.info("검증 시작: 토큰 재발급 결과 확인");
        assertThat(result).as("토큰 재발급 결과가 null이 아님").isNotNull();
        assertThat(result.getLoginId()).as("재발급된 토큰의 회원 이메일 일치").isEqualTo("test@example.com");
        log.info("토큰 재발급 결과 검증 완료");

        log.info("{}: RTR(Refresh Token Rotation) 패턴 확인", MOCK_VERIFICATION_START_LOG);
        verify(tokenProvider).validateToken(refreshToken);
        log.info("1단계: 리프레시 토큰 유효성 검증");
        verify(tokenProvider).getUsernameFromToken(refreshToken);
        log.info("2단계: 토큰에서 사용자명 추출");
        verify(memberRepository).findByLoginId("test@example.com");
        log.info("3단계: 회원 정보 조회");
        verify(tokenProvider).generateAccessToken(member);
        log.info("4단계: 새 액세스 토큰 생성");
        verify(tokenProvider).generateRefreshToken(member);
        log.info("5단계: 새 리프레시 토큰 생성 (RTR 패턴)");
        verify(cookieUtil).setTokenCookies(response, "newAccessToken", "newRefreshToken");
        log.info("6단계: 새 토큰들을 쿠키로 설정");
        log.info("=== 토큰 재발급 성공 테스트 완료 ===");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 리프레시 토큰")
    void refreshToken_Fail_InvalidToken() {
        // given
        String refreshToken = "invalidRefreshToken";
        given(tokenProvider.validateToken(refreshToken)).willReturn(false);

        // when and then
        assertThatThrownBy(() -> memberService.refreshToken(refreshToken, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다.");

        verify(tokenProvider).validateToken(refreshToken);
        verify(tokenProvider, never()).getUsernameFromToken(anyString());
        verify(memberRepository, never()).findByLoginId(anyString());
        verify(cookieUtil, never()).setTokenCookies(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void withdraw_Success() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        memberService.withdraw(memberId);

        // then
        verify(memberRepository).findById(memberId);
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 존재하지 않는 회원")
    void withdraw_Fail_MemberNotFound() {
        // given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.withdraw(memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberRepository).findById(memberId);
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
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // when
        memberService.logout(response);

        // then
        verify(cookieUtil).clearTokenCookies(response);
    }
}