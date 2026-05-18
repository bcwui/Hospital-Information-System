package com.project.his.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class minIOConfig {
    @Bean
    public MinioClient minioClient(){
        //构建客户端对象
        return MinioClient.builder()
                .endpoint("http://localhost",9000,false)
                .credentials("minioadmin","minioadmin")
                .build();
    }
}
