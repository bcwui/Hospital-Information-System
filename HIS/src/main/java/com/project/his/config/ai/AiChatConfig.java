package com.graduation.his.config.ai;

import com.graduation.his.common.ai.memory.RedissonChatMemoryRepository;
import com.graduation.his.config.ai.tools.AppointmentTools;
import com.graduation.his.config.ai.tools.HospitalTools;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * ChatClient 与记忆/RAG/Tools 配置
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiChatConfig {

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
    @ConditionalOnProperty(prefix = "app.ai.rag", name = "enabled", havingValue = "true")
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore, AiProperties properties) {
        AiProperties.Rag rag = properties.getRag();
        SearchRequest.Builder builder = SearchRequest.builder()
                .topK(rag.getTopK())
                .similarityThreshold(rag.getSimilarityThreshold());
        if (StringUtils.hasText(rag.getFilterExpression())) {
            builder.filterExpression(rag.getFilterExpression());
        }
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(builder.build())
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                 org.springframework.beans.factory.ObjectProvider<QuestionAnswerAdvisor> qaAdvisorProvider,
                                 HospitalTools hospitalTools,
                                 AppointmentTools appointmentTools) {
        QuestionAnswerAdvisor qaAdvisor = qaAdvisorProvider.getIfAvailable();
        ChatClient.Builder clientBuilder = builder
                .defaultTools(hospitalTools, appointmentTools)
                .defaultAdvisors(messageChatMemoryAdvisor);
        if (qaAdvisor != null) {
            clientBuilder.defaultAdvisors(qaAdvisor);
        }
        return clientBuilder.build();
    }
}
