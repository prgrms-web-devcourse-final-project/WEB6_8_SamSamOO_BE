package com.ai.lawyer.domain.member.dto;

import com.ai.lawyer.domain.member.entity.Member;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSignupRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일(로그인 ID)은 필수입니다")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @NotNull(message = "나이는 필수입니다")
    @Min(value = 14, message = "최소 14세 이상이어야 합니다")
    private Integer age;

    @NotNull(message = "성별은 필수입니다")
    private Member.Gender gender;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다")
    private String name;
}