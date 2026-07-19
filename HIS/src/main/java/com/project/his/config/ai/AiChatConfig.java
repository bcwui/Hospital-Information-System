package com.project.his.config.ai;

import com.project.his.common.ai.memory.RedissonChatMemoryRepository;
import com.project.his.config.ai.tools.AppointmentTools;
import com.project.his.config.ai.tools.HospitalTools;
import com.project.his.config.ai.tools.KnowledgeTools;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * ChatClient 与记忆/RAG/Tools 配置
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiChatConfig {

    /**
     * 装饰所有 Reactor 调度器的 ExecutorService，使其支持 TTL 上下文传递。
     * 这样 boundedElastic 等线程池执行 Tool 方法时，ThreadLocal 值不会丢失。
     */
    @PostConstruct
    public void enableTtlForReactors() {
        Schedulers.setExecutorServiceDecorator("ttl-decorator",
                (scheduler, service) -> {
                    if (service instanceof java.util.concurrent.ScheduledThreadPoolExecutor stpe) {
                        return com.alibaba.ttl.threadpool.TtlExecutors.getTtlScheduledExecutorService(stpe);
                    }
                    return service;
                }
        );
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(RedissonClient redissonClient, AiProperties properties) {
        AiProperties.Memory memory = properties.getMemory();
        Duration ttl = memory.getTtlSeconds() > 0 ? Duration.ofSeconds(memory.getTtlSeconds()) : null;
        return new RedissonChatMemoryRepository(redissonClient, memory.getKeyPrefix(), ttl, memory.getMaxMessages());
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository, AiProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(properties.getMemory().getMaxMessages())
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                 HospitalTools hospitalTools,
                                 @org.springframework.context.annotation.Lazy AppointmentTools appointmentTools,
                                 KnowledgeTools knowledgeTools) {
        return builder
                .defaultTools(hospitalTools, appointmentTools, knowledgeTools)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
    }
}
