package com.ai.lawyer.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth2 로그인 테스트 요청 (개발/테스트용)")
public class OAuth2LoginTestRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이어야 합니다")
    @Schema(description = "사용자 이메일", example = "test@kakao.com")
    private String email;

    @NotBlank(message = "이름은 필수입니다")
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @NotNull(message = "나이는 필수입니다")
    @Schema(description = "사용자 나이", example = "25")
    private Integer age;

    @NotBlank(message = "성별은 필수입니다")
    @Schema(description = "사용자 성별 (MALE/FEMALE/OTHER)", example = "MALE")
    private String gender;

    @NotBlank(message = "Provider는 필수입니다")
    @Schema(description = "OAuth Provider (KAKAO/NAVER)", example = "KAKAO")
    private String provider;

    @NotBlank(message = "Provider ID는 필수입니다")
    @Schema(description = "OAuth Provider의 사용자 ID", example = "123456789")
    private String providerId;
}
