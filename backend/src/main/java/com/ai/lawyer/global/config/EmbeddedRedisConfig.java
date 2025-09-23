package com.ai.lawyer.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import redis.embedded.RedisServer;

import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(name = "spring.data.redis.embedded", havingValue = "true", matchIfMissing = true)
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @EventListener(ContextRefreshedEvent.class)
    public void startRedis() {
        try {
            redisServer = RedisServer.builder()
                    .port(redisPort)
                    .setting("maxmemory 128M")
                    .build();

            if (!redisServer.isActive()) {
                redisServer.start();
                System.out.println("=== Embedded Redis 서버가 포트 " + redisPort + "에서 시작되었습니다 ===");
            }
        } catch (Exception e) {
            System.err.println("=== Embedded Redis 서버 시작 실패: " + e.getMessage() + " ===");
        }
    }

    @PreDestroy
    @EventListener(ContextClosedEvent.class)
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            System.out.println("=== Embedded Redis 서버가 중지되었습니다 ===");
        }
    }
}
