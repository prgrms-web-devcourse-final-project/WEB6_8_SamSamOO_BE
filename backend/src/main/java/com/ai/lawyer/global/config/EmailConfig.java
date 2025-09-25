package com.ai.lawyer.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required}")
    private boolean starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout}")
    private int timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout}")
    private int writeTimeout;

    @Value("${spring.mail.from:no-reply@trybalaw.com}")
    private String fromAddress;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        // 기본 발신자 설정: 인증 전용 no-reply 이메일
        // Gmail SMTP의 제약을 우회하기 위한 다양한 설정 시도
        Properties properties = getMailProperties();

        // 발신자 관련 모든 속성에 no-reply 주소 설정
        properties.put("mail.smtp.from", fromAddress);
        properties.put("mail.from", fromAddress);
        properties.put("mail.smtp.envelope.from", fromAddress);
        properties.put("mail.mime.address.strict", "false");
        properties.put("mail.smtp.dsn.notify", "NEVER");

        mailSender.setJavaMailProperties(properties);

        return mailSender;
    }

    private Properties getMailProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", auth);
        properties.put("mail.smtp.starttls.enable", starttlsEnable);
        properties.put("mail.smtp.starttls.required", starttlsRequired);
        properties.put("mail.smtp.connectiontimeout", connectionTimeout);
        properties.put("mail.smtp.timeout", timeout);
        properties.put("mail.smtp.writetimeout", writeTimeout);

        // 발신자 관련 추가 설정
        properties.put("mail.smtp.from", fromAddress);
        properties.put("mail.from", fromAddress);
        properties.put("mail.smtp.envelope.from", fromAddress);

        // Gmail의 발신자 변경 방지 시도
        properties.put("mail.smtp.allow8bitmime", true);
        properties.put("mail.smtp.sendpartial", true);

        return properties;
    }
}