package com.ai.lawyer.global.sentry;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SentryManualConfig {

    @Value("${sentry.dsn:}")
    private String sentryDsn;

    @Value("${sentry.environment:prod}")
    private String environment;

    @Value("${sentry.release:unknown}")
    private String release;

    @PostConstruct
    public void init() {
        if (sentryDsn == null || sentryDsn.isBlank()) {
            log.warn("Sentry DSN not set — skipping Sentry init.");
            return;
        }

        Sentry.init(options -> {
            options.setDsn(sentryDsn);
            options.setEnvironment(environment);
            options.setRelease(release);
            options.setSendDefaultPii(true);
            options.setEnableExternalConfiguration(true);
            options.setTracesSampleRate(1.0); // 100% 트레이스 수집
            log.info("Sentry manually initialized (forcing enable)");
        });
    }
}