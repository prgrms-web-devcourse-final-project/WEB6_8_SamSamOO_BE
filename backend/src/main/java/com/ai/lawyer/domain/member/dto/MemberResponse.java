package com.ai.lawyer.domain.member.dto;

import com.ai.lawyer.domain.member.entity.Member;
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
    private String nickname;
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
                .nickname(member.getNickname())
                .age(member.getAge())
                .gender(member.getGender())
                .role(member.getRole())
                .name(member.getName())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}