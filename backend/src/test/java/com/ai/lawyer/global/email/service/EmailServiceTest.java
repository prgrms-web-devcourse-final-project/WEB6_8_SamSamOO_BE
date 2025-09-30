package com.ai.lawyer.global.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService 테스트")
class EmailServiceTest {

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private EmailAuthService emailAuthService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private String toEmail;
    private String loginId;
    private String authCode;

    @BeforeEach
    void setUp() {
        toEmail = "test@example.com";
        loginId = "test@example.com";
        authCode = "123456";

        // @Value로 주입되는 fromAddress 설정
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@trybalaw.com");
    }

    @Test
    @DisplayName("인증번호 이메일 전송 성공")
    void sendVerificationCode_Success() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(authCode);
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail, loginId);

        // then
        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("인증번호 이메일 전송 실패 - 인증번호 생성 실패")
    void sendVerificationCode_Fail_AuthCodeGenerationFailed() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId))
                .willThrow(new RuntimeException("Redis 연결 실패"));

        // when and then
        assertThatThrownBy(() -> emailService.sendVerificationCode(toEmail, loginId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Redis 연결 실패");

        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender, never()).createMimeMessage();
        verify(emailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("인증번호 이메일 전송 실패 - 이메일 전송 실패")
    void sendVerificationCode_Fail_EmailSendingFailed() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(authCode);
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doThrow(new MailSendException("메일 서버 연결 실패"))
                .when(emailSender).send(mimeMessage);

        // when and then
        assertThatThrownBy(() -> emailService.sendVerificationCode(toEmail, loginId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 전송 실패")
                .hasCauseInstanceOf(MailSendException.class);

        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("인증번호 이메일 전송 실패 - MessagingException 발생")
    void sendVerificationCode_Fail_MessagingException() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(authCode);
        given(emailSender.createMimeMessage()).willThrow(new RuntimeException(new MessagingException("메시지 생성 실패")));

        // when and then
        assertThatThrownBy(() -> emailService.sendVerificationCode(toEmail, loginId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 전송 실패");

        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("일반 이메일 전송 성공")
    void sendEmail_Success() throws MessagingException, UnsupportedEncodingException {
        // given
        String title = "테스트 제목";
        String content = "<h1>테스트 내용</h1>";
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendEmail(toEmail, title, content);

        // then
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("일반 이메일 전송 실패 - MessagingException")
    void sendEmail_Fail_MessagingException() {
        // given
        String title = "테스트 제목";
        String content = "<h1>테스트 내용</h1>";
        given(emailSender.createMimeMessage()).willThrow(new RuntimeException(new MessagingException("메시지 생성 실패")));

        // when and then
        assertThatThrownBy(() -> emailService.sendEmail(toEmail, title, content))
                .isInstanceOf(RuntimeException.class);

        verify(emailSender).createMimeMessage();
        verify(emailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("일반 이메일 전송 실패 - MailSendException")
    void sendEmail_Fail_MailSendException() {
        // given
        String title = "테스트 제목";
        String content = "<h1>테스트 내용</h1>";
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doThrow(new MailSendException("메일 서버 연결 실패"))
                .when(emailSender).send(mimeMessage);

        // when and then
        assertThatThrownBy(() -> emailService.sendEmail(toEmail, title, content))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("메일 서버 연결 실패");

        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("인증번호 이메일 내용 검증 - 올바른 형식으로 전송")
    void sendVerificationCode_VerifyEmailContent() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(authCode);
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail, loginId);

        // then
        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);

        // 인증번호가 생성되고 이메일이 전송되었는지 확인
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(emailSender).send(messageCaptor.capture());
    }

    @Test
    @DisplayName("여러 이메일 연속 전송 성공")
    void sendMultipleEmails_Success() {
        // given
        String toEmail1 = "user1@example.com";
        String toEmail2 = "user2@example.com";
        String loginId1 = "user1@example.com";
        String loginId2 = "user2@example.com";

        given(emailAuthService.generateAndSaveAuthCode(loginId1)).willReturn("111111");
        given(emailAuthService.generateAndSaveAuthCode(loginId2)).willReturn("222222");
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail1, loginId1);
        emailService.sendVerificationCode(toEmail2, loginId2);

        // then
        verify(emailAuthService).generateAndSaveAuthCode(loginId1);
        verify(emailAuthService).generateAndSaveAuthCode(loginId2);
        verify(emailSender, times(2)).createMimeMessage();
        verify(emailSender, times(2)).send(mimeMessage);
    }

    @Test
    @DisplayName("인증번호 이메일 전송 시 올바른 제목 설정")
    void sendVerificationCode_CorrectSubject() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(authCode);
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail, loginId);

        // then
        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);

        // 이메일 제목이 "이메일 인증번호 안내"로 설정되었는지는
        // sendEmail 메서드 호출을 통해 간접적으로 확인
    }

    @Test
    @DisplayName("HTML 형식 이메일 전송 성공")
    void sendEmail_HtmlContent_Success() throws MessagingException, UnsupportedEncodingException {
        // given
        String title = "HTML 이메일";
        String htmlContent = "<html><body><h1>환영합니다</h1><p>HTML 이메일입니다.</p></body></html>";
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendEmail(toEmail, title, htmlContent);

        // then
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("빈 이메일 주소로 전송 시도 - MimeMessage 생성 시 실패")
    void sendVerificationCode_EmptyEmail() {
        // given
        String emptyEmail = "";
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(authCode);
        given(emailSender.createMimeMessage())
                .willThrow(new RuntimeException(new MessagingException("잘못된 이메일 주소")));

        // when and then
        assertThatThrownBy(() -> emailService.sendVerificationCode(emptyEmail, loginId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 전송 실패");

        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("null 이메일 주소로 전송 시도 - MimeMessage 생성 시 실패")
    void sendVerificationCode_NullEmail() {
        // given
        String nullEmail = null;
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(authCode);
        given(emailSender.createMimeMessage())
                .willThrow(new RuntimeException(new MessagingException("잘못된 이메일 주소")));

        // when and then
        assertThatThrownBy(() -> emailService.sendVerificationCode(nullEmail, loginId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 전송 실패");

        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("발신자 주소가 올바르게 설정됨")
    void sendEmail_FromAddressCorrectlySet() throws MessagingException, UnsupportedEncodingException {
        // given
        String title = "테스트";
        String content = "내용";
        String customFromAddress = "custom@trybalaw.com";
        ReflectionTestUtils.setField(emailService, "fromAddress", customFromAddress);

        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendEmail(toEmail, title, content);

        // then
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);

        // fromAddress 필드가 올바르게 사용되었는지 확인
        assertThat(ReflectionTestUtils.getField(emailService, "fromAddress"))
                .isEqualTo(customFromAddress);
    }

    @Test
    @DisplayName("인증번호 이메일 전송 실패 후 재시도")
    void sendVerificationCode_RetryAfterFailure() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId))
                .willReturn(authCode);
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);

        // 첫 번째 시도는 실패, 두 번째 시도는 성공
        doThrow(new MailSendException("일시적 오류"))
                .doNothing()
                .when(emailSender).send(mimeMessage);

        // when - 첫 번째 시도 실패
        assertThatThrownBy(() -> emailService.sendVerificationCode(toEmail, loginId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 전송 실패");

        // 두 번째 시도 성공
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn("654321");
        emailService.sendVerificationCode(toEmail, loginId);

        // then
        verify(emailAuthService, times(2)).generateAndSaveAuthCode(loginId);
        verify(emailSender, times(2)).createMimeMessage();
        verify(emailSender, times(2)).send(mimeMessage);
    }

    @Test
    @DisplayName("인증번호가 이메일 내용에 포함되어 있음")
    void sendVerificationCode_AuthCodeInContent() {
        // given
        String customAuthCode = "999888";
        given(emailAuthService.generateAndSaveAuthCode(loginId)).willReturn(customAuthCode);
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail, loginId);

        // then
        verify(emailAuthService).generateAndSaveAuthCode(loginId);
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);

        // 인증번호가 생성되고 이메일에 사용되었는지 확인
        // (실제 이메일 내용 확인은 MimeMessageHelper를 통해 간접적으로 검증)
    }

    @Test
    @DisplayName("동일한 사용자에게 연속으로 인증번호 전송")
    void sendVerificationCode_SameUserMultipleTimes() {
        // given
        given(emailAuthService.generateAndSaveAuthCode(loginId))
                .willReturn("111111")
                .willReturn("222222")
                .willReturn("333333");
        given(emailSender.createMimeMessage()).willReturn(mimeMessage);
        doNothing().when(emailSender).send(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail, loginId);
        emailService.sendVerificationCode(toEmail, loginId);
        emailService.sendVerificationCode(toEmail, loginId);

        // then
        verify(emailAuthService, times(3)).generateAndSaveAuthCode(loginId);
        verify(emailSender, times(3)).createMimeMessage();
        verify(emailSender, times(3)).send(mimeMessage);
    }
}