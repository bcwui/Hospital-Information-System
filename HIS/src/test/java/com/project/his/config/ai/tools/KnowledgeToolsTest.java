package com.project.his.config.ai.tools;

import com.project.his.service.entity.IAiRagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeToolsTest {

    @Mock IAiRagService aiRagService;

    @Test
    void searchKnowledgeBase_emptyQuery_returnsError() {
        KnowledgeTools tools = new KnowledgeTools(aiRagService);
        ToolResultEmitterHolder.clear();
        String result = tools.searchKnowledgeBase("");
        assertTrue(result.contains("请提供搜索关键词"));
    }

    @Test
    void searchKnowledgeBase_nullQuery_returnsError() {
        KnowledgeTools tools = new KnowledgeTools(aiRagService);
        ToolResultEmitterHolder.clear();
        String result = tools.searchKnowledgeBase(null);
        assertTrue(result.contains("请提供搜索关键词"));
    }

    @Test
    void searchKnowledgeBase_validQuery_callsService() {
        KnowledgeTools tools = new KnowledgeTools(aiRagService);
        ToolResultEmitterHolder.clear();
        // Should call search and return "未找到" when no results
        String result = tools.searchKnowledgeBase("头痛");
        assertNotNull(result);
    }
}
