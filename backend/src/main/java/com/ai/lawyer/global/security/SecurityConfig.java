package com.ai.lawyer.global.security;

import com.ai.lawyer.global.jwt.JwtAuthenticationFilter;
import com.ai.lawyer.global.oauth.CustomOAuth2UserService;
import com.ai.lawyer.global.oauth.OAuth2FailureHandler;
import com.ai.lawyer.global.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정
 * - JWT 기반 인증
 * - OAuth2 소셜 로그인 (카카오, 네이버)
 * - CORS 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Value("${custom.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    // 인증 없이 접근 가능한 공개 엔드포인트
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",           // 회원 인증 (로그인, 회원가입, OAuth2 등)
            "/api/public/**",         // 공개 API
            "/oauth2/**",             // OAuth2 인증 시작
            "/login/oauth2/**",       // OAuth2 콜백
            "/v3/api-docs/**",        // Swagger API 문서
            "/swagger-ui/**",         // Swagger UI
            "/swagger-ui.html",       // Swagger UI HTML
            "/api/posts/**",          // 게시글 (공개)
            "/api/polls/{pollId}/statics", // 투표 통계 (공개)
            "/api/precedent/**",      // 판례 (공개)
            "/api/law/**",            // 법령 (공개)
            "/api/law-word/**",       // 법률 용어 (공개)
            "/api/chat/**",           // 챗봇 (공개)
            "/api/home/**",         // 홈 (공개)
            "/h2-console/**",          // H2 콘솔 (개발용)
            "/actuator/health", "/actuator/health/**", "/actuator/info",    // Spring Actuator
            "/api/actuator/health", "/api/actuator/health/**", "/api/actuator/info"
    };

    // CORS 허용 메서드
    private static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    };

    // CORS 허용 헤더
    private static final String[] ALLOWED_HEADERS = {
            "Authorization", "Content-Type", "Accept", "X-Requested-With"
    };

    // 인증 실패 시 반환할 JSON 메시지
    private static final String UNAUTHORIZED_JSON =
            "{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 기본 보안 설정
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // 세션 정책: JWT 기반 인증이므로 세션 사용 안 함 (STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // H2 콘솔을 위한 frameOptions 설정
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // 접근 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))

                // 인증 실패 시 JSON 응답 (HTML 로그인 페이지 대신)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(UNAUTHORIZED_JSON);
                        }))

                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 이전)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(ALLOWED_METHODS));
        configuration.setAllowedHeaders(Arrays.asList(ALLOWED_HEADERS));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
