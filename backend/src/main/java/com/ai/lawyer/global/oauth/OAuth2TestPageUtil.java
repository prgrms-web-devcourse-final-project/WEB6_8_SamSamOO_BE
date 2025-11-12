package com.ai.lawyer.global.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * OAuth2 테스트 페이지 HTML 파일을 읽어서 반환하는 유틸리티 클래스
 * 개발/테스트 전용 클래스입니다.
 * 프로덕션 배포 시 이 클래스와 templates/oauth2-test 디렉토리를 삭제하세요.
 */
@Slf4j
@Component
public class OAuth2TestPageUtil {

    private static final String TEMPLATE_DIR = "templates/oauth2-test/";

    /**
     * OAuth2 테스트 메인 페이지 HTML 반환
     */
    public String getTestPageHtml() {
        return readHtmlFile("test-page.html");
    }

    /**
     * OAuth2 로그인 성공 페이지 HTML 반환
     * @param title 제목
     * @param message 메시지
     * @param details 상세 정보 (null 가능)
     * @return HTML 문자열
     */
    public String getSuccessPageHtml(String title, String message, String details) {
        String html = readHtmlFile("success-page.html");

        // 플레이스홀더 치환
        html = html.replace("{{TITLE}}", escapeHtml(title));
        html = html.replace("{{MESSAGE}}", escapeHtml(message));
        html = html.replace("{{CLASS}}", message.contains("성공") ? "success" : "error");
        html = html.replace("{{DETAILS}}",
            details != null ? "<div class='details'>" + escapeHtml(details) + "</div>" : "");

        return html;
    }

    /**
     * OAuth2 로그인 실패 페이지 HTML 반환
     * @param errorMessage 에러 메시지
     * @return HTML 문자열
     */
    public String getFailurePageHtml(String errorMessage) {
        String html = readHtmlFile("failure-page.html");
        html = html.replace("{{ERROR_MESSAGE}}", escapeHtml(errorMessage));
        return html;
    }

    /**
     * HTML 파일 읽기
     * @param fileName 파일명
     * @return HTML 문자열
     */
    private String readHtmlFile(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_DIR + fileName);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.error("HTML 파일 읽기 실패: {}", fileName, e);
            return getFallbackErrorHtml(fileName);
        }
    }

    /**
     * 파일 읽기 실패 시 폴백 HTML
     */
    private String getFallbackErrorHtml(String fileName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>오류</title>
            </head>
            <body>
                <h1>파일을 찾을 수 없습니다</h1>
                <p>%s 파일을 읽을 수 없습니다.</p>
            </body>
            </html>
            """, fileName);
    }

    /**
     * HTML 이스케이프 처리 (XSS 방지)
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }
}
