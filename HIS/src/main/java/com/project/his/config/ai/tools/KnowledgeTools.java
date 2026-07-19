package com.project.his.config.ai.tools;

import com.project.his.config.ai.event.AiEvent;
import com.project.his.config.ai.event.ToolEventEmitter;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.vo.AiRagSearchResult;
import com.project.his.service.entity.IAiRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeTools {

    private final IAiRagService aiRagService;

    @Tool(description = "搜索医疗知识库。当患者咨询疾病症状、用药、就医流程、医保政策等需要专业知识的问题时调用此工具。返回相关文档片段及引用来源。")
    public String searchKnowledgeBase(
            @ToolParam(description = "搜索关键词或问题描述") String query) {

        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        if (emitter != null) {
            emitter.emitToolCall("searchKnowledgeBase", null);
        }

        log.info("AI调用工具: searchKnowledgeBase, query={}", query);
        if (query == null || query.trim().isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchKnowledgeBase", false);
            return "请提供搜索关键词";
        }

        AiRagSearchRequest request = AiRagSearchRequest.builder()
                .query(query.trim())
                .topK(4)
                .build();

        List<AiRagSearchResult> results = aiRagService.search(request);

        if (results == null || results.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchKnowledgeBase", true);
            return "未找到相关知识，建议换个问法或咨询人工客服。";
        }

        // Push reference sources
        if (emitter != null) {
            List<AiEvent.RefSource> sources = new ArrayList<>();
            for (AiRagSearchResult r : results) {
                String title = "未知来源";
                Map<String, Object> meta = r.getMetadata();
                if (meta != null && meta.get("title") != null) {
                    title = meta.get("title").toString();
                }
                String snippet = r.getText() != null && r.getText().length() > 100
                        ? r.getText().substring(0, 100) + "..." : r.getText();
                sources.add(AiEvent.RefSource.builder()
                        .title(title)
                        .snippet(snippet != null ? snippet : "")
                        .build());
            }
            emitter.emitRef(sources);
        }

        StringBuilder sb = new StringBuilder("医疗知识库检索结果（请基于以下信息回答用户问题）：\n\n");
        for (int i = 0; i < results.size(); i++) {
            AiRagSearchResult r = results.get(i);
            String title = null;
            if (r.getMetadata() != null && r.getMetadata().get("title") != null) {
                title = r.getMetadata().get("title").toString();
            }
            sb.append("【").append(i + 1).append("】");
            if (title != null) sb.append(" 来源: ").append(title);
            sb.append("\n").append(r.getText()).append("\n\n");
        }
        sb.append("回答时请引用上述知识来源，标注序号如 [1]、[2]。");

        if (emitter != null) emitter.emitToolResult("searchKnowledgeBase", true);
        return sb.toString();
    }
}
