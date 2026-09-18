package com.project.his.config.ai.tools;

import com.project.his.config.ai.event.AiEvent;
import com.project.his.config.ai.event.ToolEventEmitter;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.vo.AiRagSearchResult;
import com.project.his.config.ai.event.AiEvent;
import com.project.his.config.ai.event.ToolEventEmitter;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.po.Drug;
import com.project.his.domain.vo.AiRagSearchResult;
import com.project.his.service.entity.IAiRagService;
import com.project.his.service.entity.IDrugService;
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
    private final IDrugService drugService;

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

    @Tool(description = "查询药品库存信息。当患者咨询用药、医生需要了解药品规格和库存时可调用此工具。返回药品名称、规格、厂家、单价、库存状态。")
    public String searchDrugs(
            @ToolParam(description = "药品名称关键词") String drugName) {

        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        if (emitter != null) {
            emitter.emitToolCall("searchDrugs", null);
        }

        log.info("AI调用工具: searchDrugs, drugName={}", drugName);
        if (drugName == null || drugName.trim().isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchDrugs", false);
            return "请提供药品名称关键词";
        }

        List<Drug> drugs = drugService.searchByName(drugName.trim());
        if (drugs.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchDrugs", true);
            return "未找到与「" + drugName + "」相关的药品，建议换个关键词或咨询药师。";
        }

        StringBuilder sb = new StringBuilder("药品查询结果：\n\n");
        for (int i = 0; i < drugs.size(); i++) {
            Drug d = drugs.get(i);
            sb.append("【").append(i + 1).append("】").append(d.getDrugName()).append("\n");
            if (d.getSpecification() != null) sb.append("  规格: ").append(d.getSpecification()).append("\n");
            if (d.getManufacturer() != null) sb.append("  厂家: ").append(d.getManufacturer()).append("\n");
            sb.append("  单价: ").append(d.getUnitPrice()).append("元/").append(d.getUnit()).append("\n");
            sb.append("  库存: ").append(d.getStockQuantity()).append(d.getUnit());
            if (d.getStockQuantity() <= 0) {
                sb.append(" (缺货)");
            } else if (d.getStockQuantity() < 10) {
                sb.append(" (库存紧张)");
            }
            sb.append("\n");
            if (d.getDescription() != null) sb.append("  说明: ").append(d.getDescription()).append("\n");
            sb.append("\n");
        }
        sb.append("请基于以上药品信息回答用户问题，注意不要提供具体用药剂量，最终处方需由医生开具。");

        if (emitter != null) emitter.emitToolResult("searchDrugs", true);
        return sb.toString();
    }

    @Tool(description = "智能分诊评估。根据患者描述的症状评估紧急程度（红色=急诊/黄色=24h内就诊/绿色=常规预约），并推荐就诊科室。严重症状（突发剧烈胸痛、大出血、呼吸困难、意识丧失等）一律建议急诊。")
    public String triageEvaluate(
            @ToolParam(description = "患者症状描述，包括症状类型、持续时间、严重程度、伴随症状等") String symptoms) {

        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        if (emitter != null) {
            emitter.emitToolCall("triageEvaluate", null);
        }

        log.info("AI调用工具: triageEvaluate, symptoms={}", symptoms);
        if (symptoms == null || symptoms.trim().isEmpty()) {
            if (emitter != null) emitter.emitToolResult("triageEvaluate", false);
            return "请描述您的症状，包括主要不适、持续时间、严重程度等。";
        }

        // 急诊关键词检查
        String lower = symptoms.toLowerCase();
        boolean isEmergency = lower.contains("剧烈胸痛") || lower.contains("胸痛") && (lower.contains("剧烈") || lower.contains("突然"))
                || lower.contains("大出血") || lower.contains("呼吸困难") || lower.contains("意识丧失")
                || lower.contains("昏迷") || lower.contains("严重外伤") || lower.contains("中毒");
        boolean isUrgent = lower.contains("高热") || lower.contains("持续") && lower.contains("疼痛")
                || lower.contains("咳血") || lower.contains("便血") || lower.contains("严重过敏");

        StringBuilder sb = new StringBuilder("智能分诊评估结果：\n\n");
        sb.append("【患者描述】").append(symptoms.trim()).append("\n\n");

        if (isEmergency) {
            sb.append("【紧急程度】🔴 红色 - 紧急\n");
            sb.append("【建议】您的症状可能提示急危重症，请立即前往急诊科就医，不要延误！\n");
            sb.append("【推荐科室】急诊科\n");
        } else if (isUrgent) {
            sb.append("【紧急程度】🟡 黄色 - 需关注\n");
            sb.append("【建议】建议您在24小时内就诊，可通过系统预约相应科室。如症状加重请及时前往急诊。\n");
        } else {
            sb.append("【紧急程度】🟢 绿色 - 常规\n");
            sb.append("【建议】您的症状目前不属于急症，建议预约门诊就诊。如症状持续或加重请及时就医。\n");
        }

        sb.append("\n请基于以上分诊结果回答用户，并给出进一步的就医建议。分诊结果仅供参考，最终以医生判断为准。");

        if (emitter != null) emitter.emitToolResult("triageEvaluate", true);
        return sb.toString();
    }
}
