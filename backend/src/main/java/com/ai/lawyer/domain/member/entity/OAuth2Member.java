package com.ai.lawyer.domain.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth2_member",
        indexes = {
                @Index(name = "idx_oauth2_member_login_id", columnList = "login_id"),
                @Index(name = "idx_oauth2_member_provider", columnList = "provider, provider_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode()
public class OAuth2Member implements MemberAdapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "login_id", nullable = false, unique = true, length = 100)
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일(로그인 ID)은 필수입니다")
    private String loginId;

    @Column(name = "email", nullable = false, length = 100)
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일은 필수입니다")
    private String email;

    @Column(name = "age", nullable = false)
    @NotNull(message = "나이는 필수입니다")
    @Min(value = 14, message = "최소 14세 이상이어야 합니다")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    @NotNull(message = "성별은 필수입니다")
    private Member.Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Member.Role role = Member.Role.USER;

    @Column(name = "name", nullable = false, length = 20)
    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @NotNull(message = "OAuth Provider는 필수입니다")
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    @NotBlank(message = "Provider ID는 필수입니다")
    private String providerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Getter
    public enum Provider {
        KAKAO("카카오"), NAVER("네이버");
        private final String description;
        Provider(String description) { this.description = description; }
    }
}
