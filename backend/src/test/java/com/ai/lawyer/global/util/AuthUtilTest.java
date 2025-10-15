package com.ai.lawyer.global.util;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.OAuth2Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import com.ai.lawyer.domain.member.repositories.OAuth2MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUtil 테스트")
class AuthUtilTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuth2MemberRepository oauth2MemberRepository;

    private Member localMember;
    private OAuth2Member oauth2Member;

    @BeforeEach
    void setUp() {
        AuthUtil authUtil = new AuthUtil(memberRepository);
        authUtil.setOauth2MemberRepository(oauth2MemberRepository);

        localMember = Member.builder()
                .memberId(1L)
                .loginId("local@test.com")
                .password("encodedPassword")
                .name("로컬사용자")
                .age(30)
                .gender(Member.Gender.MALE)
                .role(Member.Role.USER)
                .build();

        oauth2Member = OAuth2Member.builder()
                .memberId(2L)
                .loginId("oauth@test.com")
                .email("oauth@test.com")
                .name("소셜사용자")
                .age(25)
                .gender(Member.Gender.FEMALE)
                .provider(OAuth2Member.Provider.KAKAO)
                .providerId("kakao123")
                .role(Member.Role.USER)
                .build();
    }

    @Test
    @DisplayName("로컬 회원 조회 성공")
    void getMemberOrThrow_LocalMember_Success() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(localMember));

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getLoginId()).isEqualTo("local@test.com");
        assertThat(result.getName()).isEqualTo("로컬사용자");

        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("OAuth2 회원 조회 성공 - Member 테이블에 없을 때")
    void getMemberOrThrow_OAuth2Member_Success() {
        // given
        Long memberId = 2L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
        given(oauth2MemberRepository.findById(memberId)).willReturn(Optional.of(oauth2Member));

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(2L);
        assertThat(result.getLoginId()).isEqualTo("oauth@test.com");
        assertThat(result.getName()).isEqualTo("소셜사용자");
        assertThat(result.getAge()).isEqualTo(25);
        assertThat(result.getGender()).isEqualTo(Member.Gender.FEMALE);
        assertThat(result.getRole()).isEqualTo(Member.Role.USER);

        verify(memberRepository).findById(memberId);
        verify(oauth2MemberRepository).findById(memberId);
    }

    @Test
    @DisplayName("OAuth2 회원을 Member로 변환 - 비밀번호는 빈 문자열")
    void getMemberOrThrow_OAuth2Member_NoPassword() {
        // given
        Long memberId = 2L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
        given(oauth2MemberRepository.findById(memberId)).willReturn(Optional.of(oauth2Member));

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId);

        // then
        assertThat(result.getPassword()).isEqualTo("");
    }

    @Test
    @DisplayName("회원을 찾을 수 없을 때 예외 발생")
    void getMemberOrThrow_MemberNotFound_ThrowsException() {
        // given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());
        given(oauth2MemberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> AuthUtil.getMemberOrThrow(memberId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다");

        verify(memberRepository).findById(memberId);
        verify(oauth2MemberRepository).findById(memberId);
    }

    @Test
    @DisplayName("로컬 회원 우선 조회 - 양쪽 테이블에 같은 ID가 있을 때")
    void getMemberOrThrow_PrioritizeLocalMember() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(localMember));
        // OAuth2 repository는 호출되지 않아야 함

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("local@test.com");

        verify(memberRepository).findById(memberId);
        // OAuth2 repository는 호출되지 않음을 검증
        org.mockito.Mockito.verifyNoInteractions(oauth2MemberRepository);
    }

    @Test
    @DisplayName("OAuth2MemberRepository가 null일 때도 정상 동작")
    void getMemberOrThrow_NullOAuth2Repository() {
        // given
        Long memberId = 1L;
        // OAuth2 repository를 설정하지 않음
        given(memberRepository.findById(memberId)).willReturn(Optional.of(localMember));

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("local@test.com");
    }
}
