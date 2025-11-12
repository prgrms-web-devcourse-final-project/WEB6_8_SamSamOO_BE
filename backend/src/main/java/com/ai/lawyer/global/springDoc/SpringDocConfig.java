package com.ai.lawyer.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                .addOpenApiCustomizer(orderBySummaryNumber())
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
                .addOpenApiCustomizer(orderBySummaryNumber())
                .build();
    }

    @Bean
    GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("챗봇과 관련된 API")
                .pathsToMatch("/api/chat/**")
                .packagesToScan("com.ai.lawyer.domain.chatbot.controller")
                .build();
    }

    @Bean
    GroupedOpenApi homeApi() {
        return GroupedOpenApi.builder()
                .group("Home API")
                .pathsToMatch("/api/home/**")
                .packagesToScan("com.ai.lawyer.domain.home.controller")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/").description("Relative (proxy-friendly)")
                ));
    }

    private OpenApiCustomizer orderBySummaryNumber() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            Map<String, PathItem> sortedPaths = new LinkedHashMap<>();

            // 정렬을 위해 summary 안에 있는 번호 추출
            Pattern pattern = Pattern.compile("^(\\d+)\\..*");

            openApi.getPaths().entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> {
                        PathItem pathItem = e.getValue();
                        // POST, GET, 등등 중 첫 번째 Operation의 summary 사용
                        Operation op = pathItem.readOperations().stream().findFirst().orElse(null);
                        if (op == null || op.getSummary() == null) return Integer.MAX_VALUE;

                        Matcher matcher = pattern.matcher(op.getSummary());
                        if (matcher.find()) {
                            return Integer.parseInt(matcher.group(1));
                        }
                        return Integer.MAX_VALUE;
                    }))
                    .forEachOrdered(entry -> sortedPaths.put(entry.getKey(), entry.getValue()));

            openApi.setPaths(new io.swagger.v3.oas.models.Paths());
            sortedPaths.forEach(openApi.getPaths()::addPathItem);
        };
    }
}