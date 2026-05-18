package com.project.his.config.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * PgVector 向量库配置
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiVectorStoreConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.ai.rag", name = "enabled", havingValue = "true")
    public JdbcTemplate aiPgJdbcTemplate(AiProperties properties) {
        AiProperties.PgVector.Datasource datasource = properties.getPgvector().getDatasource();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(datasource.getUrl());
        dataSource.setUsername(datasource.getUsername());
        dataSource.setPassword(datasource.getPassword());
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai.rag", name = "enabled", havingValue = "true")
    public VectorStore vectorStore(JdbcTemplate aiPgJdbcTemplate, EmbeddingModel embeddingModel, AiProperties properties) {
        AiProperties.PgVector pgVector = properties.getPgvector();
        return PgVectorStore.builder(aiPgJdbcTemplate, embeddingModel)
                .dimensions(pgVector.getDimensions())
                .initializeSchema(pgVector.isInitializeSchema())
                .build();
    }
}
