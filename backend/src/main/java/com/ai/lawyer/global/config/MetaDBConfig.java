package com.ai.lawyer.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Profile("!test & !test-ci")
@Configuration
public class MetaDBConfig {

    @Value("${spring.datasource-meta.url}")
    private String url;

    @Value("${spring.datasource-meta.username}")
    private String username;

    @Value("${spring.datasource-meta.password}")
    private String password;

    @Value("${spring.datasource-meta.driver-class-name}")
    private String driver;

    @Bean
    @BatchDataSource
    public DataSource metaDBSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driver)
                .build();
    }

}
