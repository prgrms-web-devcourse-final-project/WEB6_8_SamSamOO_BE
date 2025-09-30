package com.ai.lawyer.global.initData;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1)
@Profile("!test")
@RequiredArgsConstructor
public class InitData implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        makeAdmin();
    }

    @Transactional
    void makeAdmin() {
        long count = memberRepository.count();
        if (count > 0) {
            log.info("InitData skipped: users count={}", count);
            return;
        }

        Member member = memberRepository.save(
                Member.builder()
                        .loginId("admin@example.com")
                        .password(passwordEncoder.encode("1234"))
                        .age(30)
                        .gender(Member.Gender.FEMALE)
                        .name("admin")
                        .role(Member.Role.USER)
                        .build()
        );

        log.warn("=== Admin user created: {} ===", member.getLoginId());
    }
}