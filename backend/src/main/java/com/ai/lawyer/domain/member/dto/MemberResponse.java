package com.ai.lawyer.domain.member.dto;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.entity.MemberAdapter;
import com.ai.lawyer.domain.member.entity.OAuth2Member;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {

    private Long memberId;
    private String loginId;
    private String email;  // 이메일 추가
    private Integer age;
    private Member.Gender gender;
    private Member.Role role;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .loginId(member.getLoginId())
                .email(member.getLoginId())  // 로컬 회원은 loginId가 이메일
                .age(member.getAge())
                .gender(member.getGender())
                .role(member.getRole())
                .name(member.getName())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }

    public static MemberResponse from(MemberAdapter memberAdapter) {
        if (memberAdapter instanceof Member) {
            return from((Member) memberAdapter);
        } else if (memberAdapter instanceof OAuth2Member) {
            OAuth2Member oauth2Member = (OAuth2Member) memberAdapter;
            return MemberResponse.builder()
                    .memberId(oauth2Member.getMemberId())
                    .loginId(oauth2Member.getLoginId())
                    .email(oauth2Member.getEmail())  // OAuth2Member의 email 컬럼
                    .age(oauth2Member.getAge())
                    .gender(oauth2Member.getGender())
                    .role(oauth2Member.getRole())
                    .name(oauth2Member.getName())
                    .createdAt(oauth2Member.getCreatedAt())
                    .updatedAt(oauth2Member.getUpdatedAt())
                    .build();
        }
        throw new IllegalArgumentException("Unsupported member type");
    }
}