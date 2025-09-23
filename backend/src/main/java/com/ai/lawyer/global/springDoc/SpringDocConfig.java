package com.ai.lawyer.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "AI Lawyer API", version = "beta", description = "AI 변호사 서비스 API 문서입니다."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SpringDocConfig {

    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("Member API")
                .pathsToMatch("/api/auth/**")
                .packagesToScan("com.ai.lawyer.domain.member.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi postApi() {
        return GroupedOpenApi.builder()
                .group("Post API")
                .pathsToMatch("/api/posts/**")
                .packagesToScan("com.ai.lawyer.domain.post.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi pollApi() {
        return GroupedOpenApi.builder()
                .group("Poll API")
                .pathsToMatch("/api/polls/**")
                .packagesToScan("com.ai.lawyer.domain.poll.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("All APIs")
                .pathsToMatch("/api/**")
                .packagesToScan("com.ai.lawyer.domain")
                .build();
    }
}