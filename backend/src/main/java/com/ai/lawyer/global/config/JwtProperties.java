package com.ai.lawyer.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "custom.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secretKey;
    private AccessToken accessToken = new AccessToken();

    @Getter
    @Setter
    public static class AccessToken {
        private long expirationSeconds;
    }
}