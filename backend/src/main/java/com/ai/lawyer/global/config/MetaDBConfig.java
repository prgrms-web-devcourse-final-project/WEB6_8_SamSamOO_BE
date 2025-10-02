package com.ai.lawyer.global.config;

import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MetaDBConfig {

    @Bean
    @BatchDataSource
    @ConfigurationProperties(prefix = "spring.datasource-meta")
    @ConditionalOnProperty(name = "meta.datasource.enabled", havingValue = "true", matchIfMissing = true)
    public DataSource metaDBSource() {
        return DataSourceBuilder.create().build();
    }

}