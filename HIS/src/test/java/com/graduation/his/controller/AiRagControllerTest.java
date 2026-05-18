package com.graduation.his.controller;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.his.domain.dto.AiRagAddRequest;
import com.graduation.his.domain.dto.AiRagDocument;
import com.graduation.his.domain.dto.AiRagSearchRequest;
import com.graduation.his.domain.vo.AiRagSearchResult;
import com.graduation.his.service.entity.IAiRagService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest(
        controllers = AiRagController.class,
        excludeAutoConfiguration = { DataSourceAutoConfiguration.class,MybatisPlusAutoConfiguration.class },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = com.graduation.his.config.SaTokenConfigure.class
        )
)
@ContextConfiguration(classes = AiRagControllerTest.TestApplication.class)
class AiRagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IAiRagService aiRagService;

    @Test
    void addDocumentsShouldReturnCount() throws Exception {
        AiRagAddRequest request = AiRagAddRequest.builder()
                .documents(List.of(
                        AiRagDocument.builder()
                                .text("doc-1")
                                .metadata(Map.of("source", "test"))
                                .build()))
                .build();

        when(aiRagService.addDocuments(any())).thenReturn(1);

        mockMvc.perform(post("/ai/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void searchShouldReturnResults() throws Exception {
        AiRagSearchRequest request = AiRagSearchRequest.builder()
                .query("symptom")
                .build();

        AiRagSearchResult result = AiRagSearchResult.builder()
                .id("doc-1")
                .text("content")
                .metadata(Map.of("source", "test"))
                .build();

        when(aiRagService.search(any())).thenReturn(List.of(result));

        mockMvc.perform(post("/ai/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("doc-1"))
                .andExpect(jsonPath("$.data[0].text").value("content"))
                .andExpect(jsonPath("$.data[0].metadata.source").value("test"));
    }

    @TestConfiguration

    @SpringBootConfiguration
    @org.springframework.context.annotation.Import({AiRagController.class, AiRagControllerTest.MockConfig.class})
    static class TestApplication {
    }

    static class MockConfig {

        @Bean
        public IAiRagService aiRagService() {
            return Mockito.mock(IAiRagService.class);
        }
    }
}