package com.ai.lawyer.global.oauth;

import com.ai.lawyer.domain.member.entity.MemberAdapter;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class PrincipalDetails implements OAuth2User, UserDetails {

    private final MemberAdapter member;
    private final Map<String, Object> attributes;

    // OAuth2 로그인용
    public PrincipalDetails(MemberAdapter member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    // 일반 로그인용
    public PrincipalDetails(MemberAdapter member) {
        this.member = member;
        this.attributes = null;
    }

    // OAuth2User 메서드
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return member.getName();
    }

    // UserDetails 메서드
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + member.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        // OAuth2Member는 password가 없으므로 null 반환
        if (member.isLocalMember()) {
            return ((com.ai.lawyer.domain.member.entity.Member) member).getPassword();
        }
        return null;
    }

    @Override
    public String getUsername() {
        return member.getLoginId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
