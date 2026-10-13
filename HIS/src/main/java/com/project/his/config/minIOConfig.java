package com.project.his.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class minIOConfig {

    /**
     * MinIO 客户端
     * 地址与密钥改为从 application.yml 的 minio.* 读取，便于生产环境配置
     * （默认值为本机开发用的 127.0.0.1:9000 / minioadmin，可参考 application.example.yml）
     */
    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint:http://127.0.0.1:9000}") String endpoint,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
