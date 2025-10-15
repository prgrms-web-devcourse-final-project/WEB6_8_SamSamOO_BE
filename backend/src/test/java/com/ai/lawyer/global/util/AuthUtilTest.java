package com.ai.lawyer.global.util;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.OAuth2Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUtil 테스트")
class AuthUtilTest {

    @Mock
    private EntityManager entityManager;

    private Member localMember;
    private OAuth2Member oauth2Member;

    @BeforeEach
    void setUp() {
        // AuthUtil의 static EntityManager를 초기화
        // 반환값은 사용하지 않지만 static 필드 설정을 위해 생성자 호출 필요
        @SuppressWarnings("unused")
        AuthUtil authUtil = new AuthUtil(entityManager);

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
        given(entityManager.find(Member.class, memberId)).willReturn(localMember);

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getLoginId()).isEqualTo("local@test.com");
        assertThat(result.getName()).isEqualTo("로컬사용자");

        verify(entityManager).find(Member.class, memberId);
    }

    @Test
    @DisplayName("OAuth2 회원 조회 성공 - Member 테이블에 없을 때")
    void getMemberOrThrow_OAuth2Member_Success() {
        // given
        Long memberId = 2L;
        given(entityManager.find(Member.class, memberId)).willReturn(null);
        given(entityManager.find(OAuth2Member.class, memberId)).willReturn(oauth2Member);

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

        verify(entityManager).find(Member.class, memberId);
        verify(entityManager).find(OAuth2Member.class, memberId);
    }

    @Test
    @DisplayName("OAuth2 회원을 Member로 변환 - 비밀번호는 빈 문자열")
    void getMemberOrThrow_OAuth2Member_NoPassword() {
        // given
        Long memberId = 2L;
        given(entityManager.find(Member.class, memberId)).willReturn(null);
        given(entityManager.find(OAuth2Member.class, memberId)).willReturn(oauth2Member);

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
        given(entityManager.find(Member.class, memberId)).willReturn(null);
        given(entityManager.find(OAuth2Member.class, memberId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> AuthUtil.getMemberOrThrow(memberId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다");

        verify(entityManager).find(Member.class, memberId);
        verify(entityManager).find(OAuth2Member.class, memberId);
    }

    @Test
    @DisplayName("로컬 회원 우선 조회 - 양쪽 테이블에 같은 ID가 있을 때")
    void getMemberOrThrow_PrioritizeLocalMember() {
        // given
        Long memberId = 1L;
        given(entityManager.find(Member.class, memberId)).willReturn(localMember);
        // OAuth2 Member는 조회되지 않아야 함

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("local@test.com");

        verify(entityManager).find(Member.class, memberId);
        // OAuth2Member는 조회되지 않음을 검증
        org.mockito.Mockito.verify(entityManager, org.mockito.Mockito.never())
                .find(OAuth2Member.class, memberId);
    }

    @Test
    @DisplayName("loginType으로 로컬 회원 조회 성공")
    void getMemberOrThrow_WithLoginType_Local_Success() {
        // given
        Long memberId = 1L;
        String loginType = "LOCAL";
        given(entityManager.find(Member.class, memberId)).willReturn(localMember);

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId, loginType);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getLoginId()).isEqualTo("local@test.com");
        assertThat(result.getName()).isEqualTo("로컬사용자");

        verify(entityManager).find(Member.class, memberId);
        // OAuth2Member는 조회되지 않음
        org.mockito.Mockito.verify(entityManager, org.mockito.Mockito.never())
                .find(OAuth2Member.class, memberId);
    }

    @Test
    @DisplayName("loginType으로 OAuth2 회원 조회 성공")
    void getMemberOrThrow_WithLoginType_OAuth2_Success() {
        // given
        Long memberId = 2L;
        String loginType = "OAUTH2";
        given(entityManager.find(OAuth2Member.class, memberId)).willReturn(oauth2Member);

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId, loginType);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(2L);
        assertThat(result.getLoginId()).isEqualTo("oauth@test.com");
        assertThat(result.getName()).isEqualTo("소셜사용자");
        assertThat(result.getAge()).isEqualTo(25);
        assertThat(result.getGender()).isEqualTo(Member.Gender.FEMALE);

        verify(entityManager).find(OAuth2Member.class, memberId);
        // Member는 조회되지 않음
        org.mockito.Mockito.verify(entityManager, org.mockito.Mockito.never())
                .find(Member.class, memberId);
    }

    @Test
    @DisplayName("loginType이 LOCAL이지만 회원을 찾을 수 없을 때 예외 발생")
    void getMemberOrThrow_WithLoginType_Local_NotFound() {
        // given
        Long memberId = 999L;
        String loginType = "LOCAL";
        given(entityManager.find(Member.class, memberId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> AuthUtil.getMemberOrThrow(memberId, loginType))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다");

        verify(entityManager).find(Member.class, memberId);
    }

    @Test
    @DisplayName("loginType이 OAUTH2이지만 회원을 찾을 수 없을 때 예외 발생")
    void getMemberOrThrow_WithLoginType_OAuth2_NotFound() {
        // given
        Long memberId = 999L;
        String loginType = "OAUTH2";
        given(entityManager.find(OAuth2Member.class, memberId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> AuthUtil.getMemberOrThrow(memberId, loginType))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회원 정보를 찾을 수 없습니다");

        verify(entityManager).find(OAuth2Member.class, memberId);
    }

    @Test
    @DisplayName("loginType이 null일 때는 기본값 LOCAL로 처리")
    void getMemberOrThrow_WithLoginType_Null_DefaultsToLocal() {
        // given
        Long memberId = 1L;
        String loginType = null;
        given(entityManager.find(Member.class, memberId)).willReturn(localMember);

        // when
        Member result = AuthUtil.getMemberOrThrow(memberId, loginType);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginId()).isEqualTo("local@test.com");
        verify(entityManager).find(Member.class, memberId);
    }
}
