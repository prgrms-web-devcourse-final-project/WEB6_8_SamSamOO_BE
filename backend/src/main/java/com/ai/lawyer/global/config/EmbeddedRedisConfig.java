package com.ai.lawyer.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

import jakarta.annotation.PreDestroy;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "spring.data.redis.embedded", havingValue = "true", matchIfMissing = true)
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void startRedis() {
        try {
            redisServer = RedisServer.builder()
                    .port(redisPort)
                    .setting("max_memory 128M")
                    .build();

            if (!redisServer.isActive()) {
                redisServer.start();
                log.info("=== Embedded Redis 서버가 포트 {}에서 시작되었습니다 ===", redisPort);
            }
        } catch (Exception e) {
            log.error("=== Embedded Redis 서버 시작 실패: {} ===", e.getMessage());
        }
    }

    @PreDestroy
    @EventListener(ContextClosedEvent.class)
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            log.info("=== Embedded Redis 서버가 중지되었습니다 ===");
        }
    }
}
