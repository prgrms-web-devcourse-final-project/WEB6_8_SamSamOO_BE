package com.ai.lawyer.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDto {
    @NotBlank(message = "이메일을 입력해주세요.")
    private String loginId;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    private String newPassword;

    @NotNull(message = "인증 성공 여부가 필요합니다.")
    private Boolean success;
}