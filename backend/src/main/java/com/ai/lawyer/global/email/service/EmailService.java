package com.ai.lawyer.global.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;
    private final EmailAuthService emailAuthService;

    @Value("${spring.mail.from:no-reply@trybalaw.com}")
    private String fromAddress;

    public void sendVerificationCode(String toEmail, String loginId) {
        // 인증번호 생성 및 Redis 저장
        String code = emailAuthService.generateAndSaveAuthCode(loginId);

        String subject = "이메일 인증번호 안내";
        String content = "<h3>아래 인증번호를 입력해주세요</h3>"
                + "<h2>" + code + "</h2>"
                + "<p>본 인증번호는 5분간 유효합니다.</p>";

        try {
            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    public void sendEmail(String toEmail, String title, String content) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(toEmail);
        helper.setSubject(title);
        helper.setText(content, true);

        helper.setFrom(fromAddress, "BaLaw 이메일 인증");
        helper.setReplyTo(fromAddress);

        log.info("발신자: {} (표시 이름: BaLaw 이메일 인증)", fromAddress);

        emailSender.send(message);
    }
}