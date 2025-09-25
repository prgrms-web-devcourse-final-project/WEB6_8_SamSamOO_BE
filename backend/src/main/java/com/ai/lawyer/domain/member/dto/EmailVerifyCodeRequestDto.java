package com.ai.lawyer.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerifyCodeRequestDto {
    // 선택적 필드 - JWT 토큰이 있으면 불필요, 없으면 필수
    private String loginId;

    @NotBlank(message = "인증번호를 입력해주세요.")
    @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
    private String verificationCode;
}