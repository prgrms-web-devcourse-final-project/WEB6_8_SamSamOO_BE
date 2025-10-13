package com.ai.lawyer.global.initData;

import com.ai.lawyer.domain.member.entity.Member;
import com.ai.lawyer.domain.member.repositories.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.List;
@Component
public class InitData implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(InitData.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // bcrypt 해시인지 판단하기 위한 간단 패턴
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.*");

    public InitData(MemberRepository memberRepository,
                    PasswordEncoder passwordEncoder){
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        //관리자 로그인 아이디
        List<String> targetLoginIds = List.of("admin@example.com", "admin@test.com");

        for (String loginId : targetLoginIds) {
            log.info("InitData: checking password encoding for [{}]", loginId);

            Optional<Member> opt = memberRepository.findByLoginId(loginId);
            if (opt.isEmpty()) {
                log.info("InitData: target account not found [{}]. 아무 작업도 수행하지 않습니다.", loginId);
                continue;
            }

            Member member = opt.get();
            String stored = member.getPassword();

            if (isBcryptHash(stored)) {
                log.info("InitData: {} 계정의 비밀번호는 이미 bcrypt 해시입니다. 변경 없음.", loginId);
                continue;
            }

            // 여기서 stored는 평문으로 추정됨 -> 절대 로그에 찍지 않음
            String encoded = passwordEncoder.encode(stored);

            member.updatePassword(encoded);
            memberRepository.save(member);

            log.info("InitData: {} 계정의 비밀번호를 안전하게 암호화하여 저장했습니다.", loginId);
        }
    }

    private boolean isBcryptHash(String s) {
        return s != null && BCRYPT_PATTERN.matcher(s).matches();
    }
}