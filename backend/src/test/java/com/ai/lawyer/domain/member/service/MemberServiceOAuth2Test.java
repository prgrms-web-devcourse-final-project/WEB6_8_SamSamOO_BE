package com.ai.lawyer.domain.member.service;

import com.ai.lawyer.domain.member.dto.MemberResponse;
import com.ai.lawyer.domain.member.dto.OAuth2LoginTestRequest;
import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.OAuth2Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import com.ai.lawyer.global.jwt.CookieUtil;
import com.ai.lawyer.global.jwt.TokenProvider;
import com.ai.lawyer.global.email.service.EmailAuthService;
import com.ai.lawyer.global.email.service.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService OAuth2 테스트")
class MemberServiceOAuth2Test {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceOAuth2Test.class);

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuth2MemberRepository oauth2MemberRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailAuthService emailAuthService;

    @Mock
    private com.ai.lawyer.domain.post.repository.PostRepository postRepository;

    @Mock
    private com.ai.lawyer.domain.poll.repository.PollVoteRepository pollVoteRepository;

    @Mock
    private com.ai.lawyer.domain.chatbot.repository.HistoryRepository historyRepository;

    @Mock
    private com.ai.lawyer.domain.chatbot.repository.ChatRepository chatRepository;

    @Mock
    private com.ai.lawyer.domain.chatbot.repository.ChatPrecedentRepository chatPrecedentRepository;

    @Mock
    private com.ai.lawyer.domain.chatbot.repository.ChatLawRepository chatLawRepository;

    @Mock
    private HttpServletResponse response;

    private MemberService memberService;

    private OAuth2Member kakaoMember;
    private OAuth2Member naverMember;

    @BeforeEach
    void setUp() {
        // MemberService 생성
        memberService = new MemberService(
                memberRepository,
                passwordEncoder,
                tokenProvider,
                cookieUtil,
                emailService,
                emailAuthService,
                postRepository,
                pollVoteRepository,
                historyRepository,
                chatRepository,
                chatPrecedentRepository,
                chatLawRepository
        );
        memberService.setOauth2MemberRepository(oauth2MemberRepository);

        kakaoMember = OAuth2Member.builder()
                .loginId("kakao@test.com")
                .email("kakao@test.com")
                .name("카카오사용자")
                .age(35)
                .gender(Member.Gender.MALE)
                .provider(OAuth2Member.Provider.KAKAO)
                .providerId("kakao123")
                .role(Member.Role.USER)
                .build();

        naverMember = OAuth2Member.builder()
                .loginId("naver@test.com")
                .email("naver@test.com")
                .name("네이버사용자")
                .age(30)
                .gender(Member.Gender.FEMALE)
                .provider(OAuth2Member.Provider.NAVER)
                .providerId("naver456")
                .role(Member.Role.USER)
                .build();
    }

    @Test
    @DisplayName("OAuth2 회원 - 토큰 재발급 성공 (카카오)")
    void refreshToken_Success_KakaoOAuth2Member() {
        // given
        log.info("=== OAuth2 카카오 회원 토큰 재발급 테스트 시작 ===");
        String refreshToken = "validRefreshToken";

        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn("kakao@test.com");
        given(tokenProvider.validateRefreshToken("kakao@test.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("kakao@test.com")).willReturn(Optional.empty());
        given(oauth2MemberRepository.findByLoginId("kakao@test.com")).willReturn(Optional.of(kakaoMember));
        given(tokenProvider.generateAccessToken(kakaoMember)).willReturn("newAccessToken");
        given(tokenProvider.generateRefreshToken(kakaoMember)).willReturn("newRefreshToken");
        log.info("Mock 설정 완료: OAuth2 카카오 회원 존재, 토큰 유효");

        // when
        log.info("OAuth2 회원 토큰 재발급 서비스 호출 중...");
        MemberResponse result = memberService.refreshToken(refreshToken, response);
        log.info("OAuth2 회원 토큰 재발급 완료: 이메일={}", result.getEmail());

        // then
        log.info("검증 시작: OAuth2 회원 토큰 재발급 결과 확인");
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("kakao@test.com");
        assertThat(result.getEmail()).isEqualTo("kakao@test.com");
        assertThat(result.getName()).isEqualTo("카카오사용자");

        verify(tokenProvider).findUsernameByRefreshToken(refreshToken);
        verify(tokenProvider).validateRefreshToken("kakao@test.com", refreshToken);
        verify(memberRepository).findByLoginId("kakao@test.com");
        verify(oauth2MemberRepository).findByLoginId("kakao@test.com");
        verify(tokenProvider).deleteAllTokens("kakao@test.com");
        verify(tokenProvider).generateAccessToken(kakaoMember);
        verify(tokenProvider).generateRefreshToken(kakaoMember);
        verify(cookieUtil).setTokenCookies(response, "newAccessToken", "newRefreshToken");
        log.info("=== OAuth2 카카오 회원 토큰 재발급 테스트 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 회원 - 토큰 재발급 성공 (네이버)")
    void refreshToken_Success_NaverOAuth2Member() {
        // given
        log.info("=== OAuth2 네이버 회원 토큰 재발급 테스트 시작 ===");
        String refreshToken = "validRefreshToken";

        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn("naver@test.com");
        given(tokenProvider.validateRefreshToken("naver@test.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("naver@test.com")).willReturn(Optional.empty());
        given(oauth2MemberRepository.findByLoginId("naver@test.com")).willReturn(Optional.of(naverMember));
        given(tokenProvider.generateAccessToken(naverMember)).willReturn("newAccessToken");
        given(tokenProvider.generateRefreshToken(naverMember)).willReturn("newRefreshToken");
        log.info("Mock 설정 완료: OAuth2 네이버 회원 존재, 토큰 유효");

        // when
        MemberResponse result = memberService.refreshToken(refreshToken, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("naver@test.com");
        assertThat(result.getEmail()).isEqualTo("naver@test.com");
        assertThat(result.getName()).isEqualTo("네이버사용자");

        verify(oauth2MemberRepository).findByLoginId("naver@test.com");
        verify(tokenProvider).generateAccessToken(naverMember);
        verify(tokenProvider).generateRefreshToken(naverMember);
        log.info("=== OAuth2 네이버 회원 토큰 재발급 테스트 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 회원 - ID로 회원 조회 성공 (카카오)")
    void getMemberById_Success_KakaoOAuth2Member() {
        // given
        log.info("=== OAuth2 카카오 회원 ID로 조회 테스트 시작 ===");
        Long memberId = 1L;
        // memberId 필드 설정 (리플렉션 사용)
        org.springframework.test.util.ReflectionTestUtils.setField(kakaoMember, "memberId", memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
        given(oauth2MemberRepository.findById(memberId)).willReturn(Optional.of(kakaoMember));
        log.info("Mock 설정 완료: OAuth2 카카오 회원 존재");

        // when
        log.info("OAuth2 회원 조회 서비스 호출 중...");
        MemberResponse result = memberService.getMemberById(memberId);
        log.info("OAuth2 회원 조회 완료: 이메일={}", result.getEmail());

        // then
        log.info("검증 시작: OAuth2 회원 조회 결과 확인");
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getLoginId()).isEqualTo("kakao@test.com");
        assertThat(result.getEmail()).isEqualTo("kakao@test.com");
        assertThat(result.getName()).isEqualTo("카카오사용자");

        verify(memberRepository).findById(memberId);
        verify(oauth2MemberRepository).findById(memberId);
        log.info("=== OAuth2 카카오 회원 ID로 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 회원 - ID로 회원 조회 성공 (네이버)")
    void getMemberById_Success_NaverOAuth2Member() {
        // given
        Long memberId = 2L;
        org.springframework.test.util.ReflectionTestUtils.setField(naverMember, "memberId", memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
        given(oauth2MemberRepository.findById(memberId)).willReturn(Optional.of(naverMember));

        // when
        MemberResponse result = memberService.getMemberById(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getLoginId()).isEqualTo("naver@test.com");
        assertThat(result.getEmail()).isEqualTo("naver@test.com");
        assertThat(result.getName()).isEqualTo("네이버사용자");

        verify(oauth2MemberRepository).findById(memberId);
    }

    @Test
    @DisplayName("OAuth2 회원 - ID로 회원 조회 실패 (존재하지 않는 회원)")
    void getMemberById_Fail_OAuth2MemberNotFound() {
        // given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
        given(oauth2MemberRepository.findById(memberId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> memberService.getMemberById(memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        verify(memberRepository).findById(memberId);
        verify(oauth2MemberRepository).findById(memberId);
    }

    @Test
    @DisplayName("OAuth2 로그인 테스트 - 신규 카카오 회원 생성 및 토큰 발급")
    void oauth2LoginTest_NewKakaoMember() {
        // given
        log.info("=== OAuth2 로그인 테스트 (신규 카카오 회원) 시작 ===");
        OAuth2LoginTestRequest request = OAuth2LoginTestRequest.builder()
                .email("new-kakao@test.com")
                .name("신규카카오")
                .age(25)
                .gender("MALE")
                .provider("KAKAO")
                .providerId("new-kakao-123")
                .build();

        OAuth2Member newMember = OAuth2Member.builder()
                .loginId("new-kakao@test.com")
                .email("new-kakao@test.com")
                .name("신규카카오")
                .age(25)
                .gender(Member.Gender.MALE)
                .provider(OAuth2Member.Provider.KAKAO)
                .providerId("new-kakao-123")
                .role(Member.Role.USER)
                .build();

        given(oauth2MemberRepository.findByLoginId("new-kakao@test.com")).willReturn(Optional.empty());
        given(oauth2MemberRepository.save(any(OAuth2Member.class))).willReturn(newMember);
        given(tokenProvider.generateAccessToken(any(OAuth2Member.class))).willReturn("accessToken");
        given(tokenProvider.generateRefreshToken(any(OAuth2Member.class))).willReturn("refreshToken");
        log.info("Mock 설정 완료: 신규 OAuth2 회원 생성, 토큰 생성 준비");

        // when
        log.info("OAuth2 로그인 테스트 서비스 호출 중...");
        MemberResponse result = memberService.oauth2LoginTest(request, response);
        log.info("OAuth2 로그인 테스트 완료: 이메일={}", result.getEmail());

        // then
        log.info("검증 시작: 신규 OAuth2 회원 생성 및 토큰 발급 확인");
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("new-kakao@test.com");
        assertThat(result.getEmail()).isEqualTo("new-kakao@test.com");
        assertThat(result.getName()).isEqualTo("신규카카오");

        verify(oauth2MemberRepository).findByLoginId("new-kakao@test.com");
        verify(oauth2MemberRepository).save(any(OAuth2Member.class));
        verify(tokenProvider).generateAccessToken(any(OAuth2Member.class));
        verify(tokenProvider).generateRefreshToken(any(OAuth2Member.class));
        verify(cookieUtil).setTokenCookies(response, "accessToken", "refreshToken");
        log.info("=== OAuth2 로그인 테스트 (신규 카카오 회원) 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 로그인 테스트 - 기존 네이버 회원 로그인 및 토큰 발급")
    void oauth2LoginTest_ExistingNaverMember() {
        // given
        log.info("=== OAuth2 로그인 테스트 (기존 네이버 회원) 시작 ===");
        OAuth2LoginTestRequest request = OAuth2LoginTestRequest.builder()
                .email("naver@test.com")
                .name("네이버사용자")
                .age(30)
                .gender("FEMALE")
                .provider("NAVER")
                .providerId("naver456")
                .build();

        given(oauth2MemberRepository.findByLoginId("naver@test.com")).willReturn(Optional.of(naverMember));
        given(tokenProvider.generateAccessToken(naverMember)).willReturn("accessToken");
        given(tokenProvider.generateRefreshToken(naverMember)).willReturn("refreshToken");
        log.info("Mock 설정 완료: 기존 OAuth2 회원 존재, 토큰 생성 준비");

        // when
        log.info("OAuth2 로그인 테스트 서비스 호출 중...");
        MemberResponse result = memberService.oauth2LoginTest(request, response);
        log.info("OAuth2 로그인 테스트 완료: 이메일={}", result.getEmail());

        // then
        log.info("검증 시작: 기존 OAuth2 회원 로그인 및 토큰 발급 확인");
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("naver@test.com");
        assertThat(result.getEmail()).isEqualTo("naver@test.com");
        assertThat(result.getName()).isEqualTo("네이버사용자");

        verify(oauth2MemberRepository).findByLoginId("naver@test.com");
        verify(oauth2MemberRepository, never()).save(any(OAuth2Member.class));
        verify(tokenProvider).generateAccessToken(naverMember);
        verify(tokenProvider).generateRefreshToken(naverMember);
        verify(cookieUtil).setTokenCookies(response, "accessToken", "refreshToken");
        log.info("=== OAuth2 로그인 테스트 (기존 네이버 회원) 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 회원 - 로그아웃 성공")
    void logout_Success_OAuth2Member() {
        // given
        log.info("=== OAuth2 회원 로그아웃 테스트 시작 ===");
        String loginId = "kakao@test.com";
        log.info("로그아웃 대상 OAuth2 사용자: {}", loginId);

        // when
        log.info("OAuth2 회원 로그아웃 서비스 호출 중...");
        memberService.logout(loginId, response);
        log.info("OAuth2 회원 로그아웃 완료");

        // then
        log.info("검증 시작: OAuth2 회원 Redis 토큰 삭제 및 쿠키 클리어 확인");
        verify(tokenProvider).deleteAllTokens(loginId);
        log.info("OAuth2 회원 Redis에서 모든 토큰 삭제 호출 확인");
        verify(cookieUtil).clearTokenCookies(response);
        log.info("OAuth2 회원 쿠키에서 토큰 클리어 호출 확인");
        log.info("=== OAuth2 회원 로그아웃 테스트 완료 ===");
    }

    @Test
    @DisplayName("OAuth2 회원 - JWT 토큰에서 loginId 추출 성공")
    void extractLoginIdFromToken_Success_OAuth2Member() {
        // given
        String token = "valid.oauth2.jwt.token";
        String expectedLoginId = "kakao@test.com";
        given(tokenProvider.getLoginIdFromToken(token)).willReturn(expectedLoginId);

        // when
        String result = memberService.extractLoginIdFromToken(token);

        // then
        assertThat(result).isEqualTo(expectedLoginId);
        verify(tokenProvider).getLoginIdFromToken(token);
    }

    @Test
    @DisplayName("로컬 회원 우선 조회 후 OAuth2 회원 조회 - refreshToken")
    void refreshToken_LocalMemberFirst_ThenOAuth2Member() {
        // given
        log.info("=== 로컬 회원 우선 조회 후 OAuth2 회원 조회 테스트 시작 ===");
        String refreshToken = "validRefreshToken";

        given(tokenProvider.findUsernameByRefreshToken(refreshToken)).willReturn("kakao@test.com");
        given(tokenProvider.validateRefreshToken("kakao@test.com", refreshToken)).willReturn(true);
        given(memberRepository.findByLoginId("kakao@test.com")).willReturn(Optional.empty());
        log.info("로컬 회원 조회 결과: 없음");
        given(oauth2MemberRepository.findByLoginId("kakao@test.com")).willReturn(Optional.of(kakaoMember));
        log.info("OAuth2 회원 조회 결과: 존재");
        given(tokenProvider.generateAccessToken(kakaoMember)).willReturn("newAccessToken");
        given(tokenProvider.generateRefreshToken(kakaoMember)).willReturn("newRefreshToken");

        // when
        MemberResponse result = memberService.refreshToken(refreshToken, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("kakao@test.com");

        verify(memberRepository).findByLoginId("kakao@test.com");
        verify(oauth2MemberRepository).findByLoginId("kakao@test.com");
        log.info("=== 로컬 회원 우선 조회 후 OAuth2 회원 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("로컬 회원 우선 조회 후 OAuth2 회원 조회 - getMemberById")
    void getMemberById_LocalMemberFirst_ThenOAuth2Member() {
        // given
        log.info("=== ID로 조회 시 로컬 회원 우선 조회 후 OAuth2 회원 조회 테스트 시작 ===");
        Long memberId = 1L;
        org.springframework.test.util.ReflectionTestUtils.setField(kakaoMember, "memberId", memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
        log.info("로컬 회원 조회 결과: 없음");
        given(oauth2MemberRepository.findById(memberId)).willReturn(Optional.of(kakaoMember));
        log.info("OAuth2 회원 조회 결과: 존재");

        // when
        MemberResponse result = memberService.getMemberById(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getLoginId()).isEqualTo("kakao@test.com");

        verify(memberRepository).findById(memberId);
        verify(oauth2MemberRepository).findById(memberId);
        log.info("=== ID로 조회 시 로컬 회원 우선 조회 후 OAuth2 회원 조회 테스트 완료 ===");
    }
}
