package com.project.his.config.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ToolEventEmitter {

    private static final Map<String, String> TOOL_LABELS = Map.ofEntries(
            Map.entry("listDepartments", "正在查询科室列表"),
            Map.entry("listClinicsByDepartment", "正在查询门诊列表"),
            Map.entry("searchClinics", "正在搜索门诊"),
            Map.entry("listDoctorsByClinic", "正在查询医生列表"),
            Map.entry("searchDoctors", "正在搜索医生"),
            Map.entry("getDoctorSchedules", "正在查询医生排班"),
            Map.entry("getAvailableSchedules", "正在查询可预约排班"),
            Map.entry("getMyAppointments", "正在查询您的预约"),
            Map.entry("createAppointment", "正在为您创建预约"),
            Map.entry("cancelAppointment", "正在为您取消预约"),
            Map.entry("searchKnowledgeBase", "正在检索医疗知识库")
    );

    private final String sessionId;

    public void emitToolCall(String toolName, String toolLabel) {
        Sinks.Many<AiEvent> sink = AiEventSinkRegistry.get(sessionId);
        if (sink == null) return;
        sink.tryEmitNext(AiEvent.builder()
                .type("tool_call")
                .toolName(toolName)
                .toolLabel(toolLabel != null ? toolLabel : TOOL_LABELS.getOrDefault(toolName, "正在执行: " + toolName))
                .status("running")
                .build());
    }

    public void emitToolResult(String toolName, boolean success) {
        Sinks.Many<AiEvent> sink = AiEventSinkRegistry.get(sessionId);
        if (sink == null) return;
        sink.tryEmitNext(AiEvent.builder()
                .type("tool_result")
                .toolName(toolName)
                .status(success ? "success" : "error")
                .build());
    }

    public void emitCard(String cardType, Object data) {
        Sinks.Many<AiEvent> sink = AiEventSinkRegistry.get(sessionId);
        if (sink == null) return;
        sink.tryEmitNext(AiEvent.builder()
                .type("card")
                .cardType(cardType)
                .data(data)
                .build());
    }

    public void emitRef(List<AiEvent.RefSource> sources) {
        Sinks.Many<AiEvent> sink = AiEventSinkRegistry.get(sessionId);
        if (sink == null) return;
        sink.tryEmitNext(AiEvent.builder()
                .type("ref")
                .sources(sources)
                .build());
    }

    public void emitError(String message) {
        Sinks.Many<AiEvent> sink = AiEventSinkRegistry.get(sessionId);
        if (sink == null) return;
        sink.tryEmitNext(AiEvent.builder()
                .type("error")
                .message(message)
                .build());
    }

    public void emitDone() {
        Sinks.Many<AiEvent> sink = AiEventSinkRegistry.get(sessionId);
        if (sink == null) return;
        sink.tryEmitNext(AiEvent.builder()
                .type("done")
                .sessionId(sessionId)
                .build());
        sink.tryEmitComplete();
    }
}
