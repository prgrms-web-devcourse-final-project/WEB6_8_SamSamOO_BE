package com.ai.lawyer.global.sentry;

import io.sentry.Sentry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SentryTestController {

    @GetMapping("/sentry-test")
    public String triggerError() {
        try {
            throw new RuntimeException("Sentry test error - Balaw 프로젝트");
        } catch (Exception e) {
            Sentry.captureException(e);
        }
        return "Error sent to Sentry!";
    }
}