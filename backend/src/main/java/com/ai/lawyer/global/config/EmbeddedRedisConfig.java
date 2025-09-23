package com.ai.lawyer.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import redis.embedded.RedisServer;

import jakarta.annotation.PreDestroy;
import java.io.IOException;

@Configuration
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.embedded:true}")
    private boolean embedded;

    private RedisServer redisServer;

    @EventListener(ContextRefreshedEvent.class)
    public void startRedis() {
        if (embedded) {
            try {
                redisServer = RedisServer.builder()
                    .port(redisPort)
                    .setting("maxmemory 128M") // 메모리 제한 설정
                    .build();
                
                if (!redisServer.isActive()) {
                    redisServer.start();
                    System.out.println("=== Embedded Redis 서버가 포트 " + redisPort + "에서 시작되었습니다 ===");
                }
            } catch (Exception e) {
                System.err.println("=== Embedded Redis 서버 시작 실패: " + e.getMessage() + " ===");
                throw e;
            }
        }
    }

    @PreDestroy
    @EventListener(ContextClosedEvent.class)
    public void stopRedis() {
        if (embedded && redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            System.out.println("=== Embedded Redis 서버가 중지되었습니다 ===");
        }
    }
}