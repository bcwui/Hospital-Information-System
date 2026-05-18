package com.project.his.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 相关配置
 */
@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private Rag rag = new Rag();

    private PgVector pgvector = new PgVector();

    private Memory memory = new Memory();

    @Data
    public static class Rag {
        /**
         * 是否启用 RAG
         */
        private boolean enabled = true;

        /**
         * 检索条数
         */
        private int topK = 4;

        /**
         * 相似度阈值
         */
        private double similarityThreshold = 0.5;

        /**
         * 过滤表达式（可选）
         */
        private String filterExpression;
    }

    @Data
    public static class PgVector {
        /**
         * 向量维度
         */
        private int dimensions = 2048;

        /**
         * 是否自动初始化 schema
         */
        private boolean initializeSchema = false;

        private Datasource datasource = new Datasource();

        @Data
        public static class Datasource {
            /**
             * JDBC URL
             */
            private String url;

            /**
             * 用户名
             */
            private String username;

            /**
             * 密码
             */
            private String password;
        }
    }

    @Data
    public static class Memory {
        /**
         * Redis key 前缀
         */
        private String keyPrefix = "ai-chat-memory-";

        /**
         * TTL（秒）
         */
        private long ttlSeconds = 21600;

        /**
         * 最大消息数
         */
        private int maxMessages = 20;
    }
}
