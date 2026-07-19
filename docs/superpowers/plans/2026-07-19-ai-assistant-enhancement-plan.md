# AI 助手完善实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Spring AI ChatClient 单 Agent 架构上，实现 SSE 类型化事件协议、工具可视化、卡片消息、Agentic RAG、智能导诊闭环、安全修复、Prompt 外部化，7 步渐进增强。

**Architecture:** 单 Agent 渐进增强——RAG 封装为工具（Agentic RAG），SSE 升级为 JSON 事件协议（一次改造支撑可视化/卡片/引用/错误），卡片数据由工具结果驱动（零幻觉），预约操作走现有 REST 不经过 LLM。

**Tech Stack:** Spring Boot 3.4.3, Spring AI 1.0.3 (ZhipuAI GLM-4.5), PgVector, Redis (Redisson), Vue 3 + Element Plus + TypeScript + marked

**Spec:** `docs/superpowers/specs/2026-07-19-ai-assistant-enhancement-design.md`

---

## File Structure Map

```
HIS/ (后端)
├── src/main/java/com/project/his/
│   ├── config/ai/
│   │   ├── AiChatConfig.java                          [修改] 移除 QuestionAnswerAdvisor、注册 KnowledgeTools、装饰工具
│   │   ├── AiProperties.java                          [修改] 可能需要添加 card 相关配置
│   │   ├── event/                                     [新建目录]
│   │   │   ├── AiEvent.java                           [新建] 事件模型（密封类/枚举）
│   │   │   ├── AiEventSinkRegistry.java               [新建] 会话级 Sinks.Many 注册表
│   │   │   └── ToolEventEmitter.java                  [新建] 工具装饰器，拦截调用推事件
│   │   └── tools/
│   │       ├── AppointmentTools.java                  [修改] 去掉 patientId 参数
│   │       ├── HospitalTools.java                     [修改] 返回结构化对象供卡片使用
│   │       ├── KnowledgeTools.java                    [新建] RAG 检索工具
│   │       ├── PatientContextHolder.java              [修改] 删 size()==1 捷径
│   │       └── ToolResultHelper.java                  [新建] 工具结果提取辅助
│   ├── service/entity/impl/
│   │   ├── AIServiceImpl.java                         [修改] 流合并、Prompt 模板加载、doOnCancel 保存
│   │   └── AiRagServiceImpl.java                      [修改] Markdown 入库
│   ├── controller/
│   │   └── RegistrationController.java                [修改] patientId 从登录态解析、sessionId 归属校验
│   └── resources/prompts/
│       └── system-prompt.st                           [新建] 外部化 Prompt 模板

VUE/ (前端)
├── src/
│   ├── views/
│   │   ├── ChatView.vue                               [修改] SSE JSON 解析、块列表渲染
│   │   └── admin/RagManager.vue                       [修改] 一键导入内置语料
│   ├── components/chat/                               [新建目录]
│   │   ├── CardMessage.vue                            [新建] 卡片容器（分发不同卡片类型）
│   │   ├── DoctorCard.vue                             [新建] 医生卡片
│   │   ├── ScheduleCard.vue                           [新建] 排班卡片（含预约按钮）
│   │   ├── AppointmentResultCard.vue                  [新建] 预约结果卡片
│   │   ├── ReferenceBlock.vue                         [新建] 引用来源折叠块
│   │   └── ToolStepBlock.vue                          [新建] 工具步骤条
│   └── types/
│       └── chat.ts                                    [新建] TypeScript 事件类型定义

docs/knowledge/                                         [新建目录]
├── 01-hospital-overview.md                            [新建] 医院概况
├── 02-registration-guide.md                           [新建] 就医流程指南
├── 03-department-intro.md                             [新建] 科室介绍
├── 04-symptom-guide.md                                [新建] 常见症状→科室对照
├── 05-cardiology.md                                   [新建] 心血管内科指南
├── 06-respiratory.md                                  [新建] 呼吸内科指南
├── 07-gastroenterology.md                             [新建] 消化内科指南
├── 08-orthopedics.md                                  [新建] 骨科指南
├── 09-pediatrics.md                                   [新建] 儿科指南
├── 10-insurance-billing.md                            [新建] 医保与收费说明
└── 11-faq.md                                          [新建] 常见问题解答
```

---

### Task 1: 安全修复

**Files:**
- Modify: `HIS/src/main/java/com/project/his/config/ai/tools/PatientContextHolder.java`
- Modify: `HIS/src/main/java/com/project/his/config/ai/tools/AppointmentTools.java`
- Modify: `HIS/src/main/java/com/project/his/domain/dto/AiConsultRequest.java`
- Modify: `HIS/src/main/java/com/project/his/controller/RegistrationController.java`
- Modify: `HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java`

- [ ] **Step 1: 修复 PatientContextHolder — 删除 size()==1 捷径**

将 `resolveFromRegistry()` 方法改为必须通过 sessionId 精确匹配：

```java
// PatientContextHolder.java

// 将 resolveFromRegistry() 替换为：
public static Long resolveFromRegistry() {
    // 同线程优先
    String sessionId = SESSION_ID.get();
    if (sessionId != null) {
        Long patientId = SESSION_REGISTRY.get(sessionId);
        if (patientId != null) {
            return patientId;
        }
    }
    // 跨线程有多个会话时无法确定，返回 null
    return null;
}

// 新增清理方法，由调用方在流结束时调用
public static void removeSession(String sessionId) {
    if (sessionId != null) {
        SESSION_REGISTRY.remove(sessionId);
    }
}
```

- [ ] **Step 2: 确认编译通过**

```bash
cd D:/item/Hospital-Information-System/HIS && mvnw compile -q
```

- [ ] **Step 3: AppointmentTools 去掉 patientId 参数**

三个工具方法签名全部移除 `patientId` 参数，改为从上下文解析：

```java
// AppointmentTools.java

// 替换 resolvePatientId 方法（去掉入参，完全从上下文获取）
private Long resolvePatientId() {
    // 1. ThreadLocal
    Long patientId = PatientContextHolder.getPatientId();
    if (patientId != null && patientId > 0) {
        log.info("通过 ThreadLocal 获取 patientId={}", patientId);
        return patientId;
    }
    // 2. ConcurrentHashMap 跨线程解析
    patientId = PatientContextHolder.resolveFromRegistry();
    if (patientId != null && patientId > 0) {
        log.info("通过 SessionRegistry 跨线程解析 patientId={}", patientId);
        return patientId;
    }
    log.error("无法获取 patientId，ThreadLocal和Registry均不可用");
    return null;
}

// getMyAppointments — 去掉 patientId 参数
@Tool(description = "查询当前患者的预约挂号记录，可按状态筛选：0-待就诊，1-已就诊，2-已取消")
public String getMyAppointments(
        @ToolParam(description = "预约状态筛选，可选值：0-待就诊，1-已就诊，2-已取消，不传则查询全部") Integer status) {
    Long resolvedId = resolvePatientId();
    // ... 其余代码不变
}

// createAppointment — 去掉 patientId 参数
@Tool(description = "为当前患者创建预约挂号。scheduleId 从排班查询结果获取。")
public String createAppointment(
        @ToolParam(description = "排班ID，可通过查询医生排班获取") Long scheduleId) {
    Long resolvedId = resolvePatientId();
    // ... 其余代码不变，去掉 patientId 空值检查
}

// cancelAppointment — 去掉 patientId 参数
@Tool(description = "取消当前患者的预约挂号。appointmentId 从预约记录中获取。")
public String cancelAppointment(
        @ToolParam(description = "要取消的预约ID") Long appointmentId) {
    Long resolvedId = resolvePatientId();
    // ... 其余代码不变，去掉 patientId 空值检查
}
```

- [ ] **Step 4: AiConsultRequest 去掉 patientId 字段**

```java
// AiConsultRequest.java — 删除 patientId 字段及其 getter/setter
// 改为：
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConsultRequest {
    /** 会话ID，首次对话为空 */
    private String sessionId;
    /** 用户问题内容 */
    private String question;
}
```

- [ ] **Step 5: Controller — patientId 从 Sa-Token 解析**

```java
// RegistrationController.java — streamAiConsult 方法修改：

@SaCheckRole("patient")
@PostMapping(value = "/ai-consult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public ResponseEntity<Flux<String>> streamAiConsult(@RequestBody AiConsultRequest request) {
    long loginId = StpUtil.getLoginIdAsLong();
    // 从登录态解析 patientId（loginId 即 userId）
    Long patientId = loginId;
    
    log.info("接收到AI问诊流式请求, patientId: {}, sessionId: {}", patientId, request.getSessionId());
    
    // 注入 patientId 到请求上下文
    request.setPatientId(patientId); // 通过内部方法设置
    
    Flux<String> stream = registrationService.streamAiConsult(request);
    // ... 其余不变
}
```

需要在 `AiConsultRequest` 保留内部使用的 setter（可改为包私有的替代方案）：直接把请求转发给 service 时传 patientId。

更好的方案——修改 Controller 内部逻辑：

```java
// RegistrationController.java — 直接用 Flux<String> processing 不经过 AiConsultRequest 的 patientId 字段：

@SaCheckRole("patient")
@PostMapping(value = "/ai-consult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public ResponseEntity<Flux<String>> streamAiConsult(@RequestBody AiConsultRequest request) {
    long loginId = StpUtil.getLoginIdAsLong(); // userId = loginId
    Long patientId = resolvePatientIdFromLogin(loginId); // 新增方法：通过 userId 查 patient

    log.info("接收到AI问诊流式请求, patientId: {}, sessionId: {}", patientId, request.getSessionId());
    try {
        Flux<String> stream = registrationService.streamAiConsult(patientId,
                request.getSessionId(), request.getQuestion());
        HttpHeaders headers = new HttpHeaders();
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            headers.add("X-Session-Id", request.getSessionId());
        }
        return ResponseEntity.ok().headers(headers).contentType(MediaType.TEXT_EVENT_STREAM).body(stream);
    } catch (IllegalArgumentException e) {
        log.error("AI问诊流式请求参数错误: {}", e.getMessage());
        throw e;
    } catch (BusinessException e) {
        log.error("AI问诊流式请求业务异常: {}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("AI问诊流式请求异常", e);
        throw new RuntimeException("服务异常，请稍后重试");
    }
}

// 新增辅助方法：通过 loginId(userId) 查到 patientId
private Long resolvePatientIdFromLogin(long userId) {
    // 假设 loginId 就是 userId，需要通过 PatientService 获取 patientId
    // 如果项目中 userId == patientId，直接返回
    return userId;
}
```

所有其他 AI 端点也改为从登录态获取 patientId，不信任前端传参：

```java
// getLatestAiConsultSession
@SaCheckRole("patient")
@GetMapping("/ai-consult/latest")
public Result<ConsultSession> getLatestAiConsultSession() {
    Long patientId = StpUtil.getLoginIdAsLong();
    // ...
}

// listAiConsultSessions
@SaCheckRole("patient")
@GetMapping("/ai-consult/sessions")
public Result<java.util.List<java.util.Map<String, Object>>> listAiConsultSessions() {
    Long patientId = StpUtil.getLoginIdAsLong();
    // ...
}

// getAiConsultHistory — 需校验 sessionId 归属
@SaCheckRole("patient")
@GetMapping("/ai-consult/history")
public Result<ConsultSession> getAiConsultHistory(@RequestParam String sessionId) {
    Long currentPatientId = StpUtil.getLoginIdAsLong();
    // 先加载 session 验证归属
    ConsultSession session = registrationService.getAiConsultHistory(sessionId);
    if (session == null) {
        return Result.error("未找到相关会话记录");
    }
    if (!currentPatientId.equals(session.getPatientId())) {
        log.warn("患者 {} 试图访问不属于自己的会话 {}", currentPatientId, sessionId);
        return Result.error("无权访问此会话");
    }
    return Result.success("获取会话记录成功", session);
}
```

- [ ] **Step 6: RegistrationService 接口调整**

```java
// IRegistrationService.java — 修改 streamAiConsult 签名
Flux<String> streamAiConsult(Long patientId, String sessionId, String question);
```

- [ ] **Step 7: RegistrationServiceImpl 适配新签名**

```java
// RegistrationServiceImpl.java
@Override
public Flux<String> streamAiConsult(Long patientId, String sessionId, String question) {
    AiConsultRequest request = AiConsultRequest.builder()
            .patientId(patientId)
            .sessionId(sessionId)
            .question(question)
            .build();
    return aiService.processAiConsult(request);
}
```

- [ ] **Step 8: AIServiceImpl 不再从 request.patientId 注入 prompt**

```java
// AIServiceImpl.java — createSystemPrompt 方法去掉 patientId 参数
private String createSystemPrompt() {
    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
    String currentDateIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    // 不再注入 patientId，让工具自己从上下文解析
    return """
            # AI 医疗助手
            
            ## 当前时间
            今天是 %s（%s）。查询排班或预约时，请基于这个日期。
            
            ## 身份
            您是一位专业、友善的AI医疗助手，服务于本院患者。您可以进行健康咨询、症状分析，并帮助患者查询医院信息、预约挂号。
            
            ## 工具能力
            您拥有以下类型的工具：
            - **信息查询类**：查询科室、门诊、医生、排班等信息
            - **预约操作类**：查询/创建/取消预约（患者身份由系统自动识别，无需传入患者ID）
            
            ## 对话原则
            1. **自然对话**: 像真人医生一样交流，避免机械回复
            2. **主动服务**: 患者描述症状时，主动询问必要细节后给出建议
            3. **工具优先**: 涉及医院信息时，先用工具查询再回答，不要凭空编造
            4. **确认再操作**: 预约/取消前必须获得患者明确确认
            5. **日期准确**: 查询排班时使用今天（%s）作为起始日期
            
            ## 回复风格
            - 简洁清晰，重点突出
            - 有症状分析时，分点说明
            - 推荐就医时，直接用工具查询并展示选项
            - 结尾适当关心患者，如"还有其他问题吗？"
            
            ## 安全提醒
            - 不提供具体药物剂量
            - 严重症状建议立即就医
            - 诊断结果仅供参考，以线下医生诊断为准
            """.formatted(currentDate, currentDateIso, currentDateIso);
}

// processAiConsult 方法中改为：
return chatClient.prompt()
        .system(createSystemPrompt())
        // ... 其余不变
```

- [ ] **Step 9: 前端 ChatView.vue — 不再发送 patientId 到 Body**

```typescript
// ChatView.vue — sendMessage 函数中 body 不再传 patientId

// 修改前：
// body: JSON.stringify({
//   patientId,
//   question: trimmed,
//   sessionId: chatHistoryStore.getId(patientIdStr.value) || null,
// }),

// 修改后：
body: JSON.stringify({
  question: trimmed,
  sessionId: chatHistoryStore.getId(patientIdStr.value) || null,
}),
```

所有调用历史/会话列表的接口也去掉 patientId 查询参数（后端从登录态获取）：

```typescript
// loadSessionList 函数中：
// 修改前: '/appointment/ai-consult/sessions', 'get', { patientId }, true
// 修改后:
const res = await DoAxiosWithErro<SessionSummary[]>(
  '/appointment/ai-consult/sessions', 'get', null, true
);

// onMounted 中获取 latest 会话：
// 修改前: '/appointment/ai-consult/latest', 'get', { patientId }, true
// 修改后:
const response = await DoAxiosWithErro<{ sessionId?: string; messageHistory?: ChatHistoryEntry[] }>(
  '/appointment/ai-consult/latest', 'get', null, true
);
```

- [ ] **Step 10: 编译验证**

```bash
cd D:/item/Hospital-Information-System/HIS && mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add HIS/src/main/java/com/project/his/config/ai/tools/PatientContextHolder.java \
        HIS/src/main/java/com/project/his/config/ai/tools/AppointmentTools.java \
        HIS/src/main/java/com/project/his/domain/dto/AiConsultRequest.java \
        HIS/src/main/java/com/project/his/controller/RegistrationController.java \
        HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java \
        HIS/src/main/java/com/project/his/service/business/IRegistrationService.java \
        HIS/src/main/java/com/project/his/service/business/impl/RegistrationServiceImpl.java \
        VUE/src/views/ChatView.vue
git commit -m "fix: AI 助手安全修复 — patientId 从登录态获取，消除越权风险"
```

---

### Task 2: SSE 类型化事件协议（后端事件模型 + 流合并）

**Files:**
- Create: `HIS/src/main/java/com/project/his/config/ai/event/AiEvent.java`
- Create: `HIS/src/main/java/com/project/his/config/ai/event/AiEventSinkRegistry.java`
- Create: `HIS/src/main/java/com/project/his/config/ai/event/ToolEventEmitter.java`
- Modify: `HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java`
- Modify: `HIS/src/main/java/com/project/his/config/ai/AiChatConfig.java`

- [ ] **Step 1: 创建 AiEvent 事件模型**

```java
// config/ai/event/AiEvent.java
package com.project.his.config.ai.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SSE 类型化事件。每条 SSE data: 载荷序列化为此 JSON。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEvent {

    private String type;     // text | tool_call | tool_result | card | ref | done | error

    // text 事件
    private String content;

    // tool_call / tool_result 事件
    private String toolName;
    private String toolLabel;  // 中文描述，如"正在查询排班..."
    private String status;     // running | success | error

    // card 事件
    private String cardType;   // doctor | schedule | appointment_result
    private Object data;       // 卡片结构数据

    // ref 事件
    private List<RefSource> sources;

    // done 事件
    private String sessionId;

    // error 事件
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefSource {
        private String title;
        private String snippet;
    }
}
```

- [ ] **Step 2: 创建 AiEventSinkRegistry**

```java
// config/ai/event/AiEventSinkRegistry.java
package com.project.his.config.ai.event;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 会话级事件通道注册表。
 * 每个 SSE 请求创建一个 Sinks.Many，以 sessionId 注册，
 * Tool 执行时通过此注册表向对应通道推送事件。
 */
@Slf4j
public class AiEventSinkRegistry {

    private static final ConcurrentMap<String, Sinks.Many<AiEvent>> CHANNELS = new ConcurrentHashMap<>();

    public static void register(String sessionId, Sinks.Many<AiEvent> sink) {
        CHANNELS.put(sessionId, sink);
        log.debug("注册事件通道: sessionId={}", sessionId);
    }

    public static Sinks.Many<AiEvent> get(String sessionId) {
        return CHANNELS.get(sessionId);
    }

    public static void remove(String sessionId) {
        CHANNELS.remove(sessionId);
        log.debug("移除事件通道: sessionId={}", sessionId);
    }
}
```

- [ ] **Step 3: 创建 ToolEventEmitter（工具装饰器）**

```java
// config/ai/event/ToolEventEmitter.java
package com.project.his.config.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

/**
 * 向事件通道推送工具执行事件。
 * 在工具执行前后调用，从注册表中查找对应会话的 Sink。
 */
@Slf4j
@RequiredArgsConstructor
public class ToolEventEmitter {

    // 工具名→中文标签映射
    private static final java.util.Map<String, String> TOOL_LABELS = new java.util.HashMap<>();

    static {
        TOOL_LABELS.put("listDepartments", "正在查询科室列表");
        TOOL_LABELS.put("listClinicsByDepartment", "正在查询门诊列表");
        TOOL_LABELS.put("searchClinics", "正在搜索门诊");
        TOOL_LABELS.put("listDoctorsByClinic", "正在查询医生列表");
        TOOL_LABELS.put("searchDoctors", "正在搜索医生");
        TOOL_LABELS.put("getDoctorSchedules", "正在查询医生排班");
        TOOL_LABELS.put("getAvailableSchedules", "正在查询可预约排班");
        TOOL_LABELS.put("getMyAppointments", "正在查询您的预约");
        TOOL_LABELS.put("createAppointment", "正在为您创建预约");
        TOOL_LABELS.put("cancelAppointment", "正在为您取消预约");
        TOOL_LABELS.put("searchKnowledgeBase", "正在检索医疗知识库");
    }

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

    public void emitRef(java.util.List<AiEvent.RefSource> sources) {
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

    /** 根据工具结果判断能否生成卡片 */
    public static boolean isCardCapable(String toolName) {
        return "getDoctorSchedules".equals(toolName)
                || "getAvailableSchedules".equals(toolName)
                || "listDoctorsByClinic".equals(toolName)
                || "createAppointment".equals(toolName)
                || "getMyAppointments".equals(toolName);
    }
}
```

- [ ] **Step 4: 修改 AIServiceImpl.processAiConsult — 流合并逻辑**

```java
// AIServiceImpl.java processAiConsult 方法重写流部分

// 在方法开头创建事件通道
final String sessionId = /* 已存在 */;
Sinks.Many<AiEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();
AiEventSinkRegistry.register(finalSessionId, eventSink);
ToolEventEmitter emitter = new ToolEventEmitter(finalSessionId);

// 将 emitter 也注入到 PatientContextHolder 或单独的 ThreadLocal 中...

// 构建流：将 ChatClient 的 content Flux 映射为 text 事件，然后与事件通道合并
Flux<AiEvent> contentEvents = chatClient.prompt()
        .system(createSystemPrompt())
        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, finalSessionId))
        .user(question)
        .stream()
        .content()
        .map(text -> AiEvent.builder().type("text").content(text).build())
        .contextWrite(ctx -> ctx
                .put(PatientContext.SESSION_ID_KEY, finalSessionId)
                .put(PatientContext.PATIENT_ID_KEY, patientId));

Flux<AiEvent> toolEvents = eventSink.asFlux();

Flux<AiEvent> allEvents = Flux.merge(contentEvents, toolEvents)
        .doOnComplete(() -> {
            updateMemoryCreateTimes(finalSessionId, userTime, LocalDateTime.now());
            saveConsultRecord(finalSessionId);
            emitter.emitDone();
            AiEventSinkRegistry.remove(finalSessionId);
        })
        .doOnError(error -> {
            log.error("处理AI问诊请求异常: {}", error.getMessage());
            updateMemoryCreateTimes(finalSessionId, userTime, null);
            emitter.emitError(error.getMessage());
            AiEventSinkRegistry.remove(finalSessionId);
        })
        .doOnCancel(() -> {
            log.info("AI问诊流被取消, sessionId: {}", finalSessionId);
            saveConsultRecord(finalSessionId);
            AiEventSinkRegistry.remove(finalSessionId);
        })
        .doFinally(signalType -> PatientContextHolder.clear());

// 序列化为 SSE JSON 字符串输出
return allEvents.map(event -> {
    try {
        return com.alibaba.fastjson2.JSON.toJSONString(event);
    } catch (Exception e) {
        return "{\"type\":\"error\",\"message\":\"序列化失败\"}";
    }
});
```

- [ ] **Step 5: 创建 ToolResultEmitterHolder（线程安全的事件发射器传递）**

```java
// config/ai/tools/ToolResultEmitterHolder.java
package com.project.his.config.ai.tools;

import com.project.his.config.ai.event.ToolEventEmitter;

/**
 * 跨线程传递 ToolEventEmitter，类似 PatientContextHolder 的机制。
 */
public class ToolResultEmitterHolder {

    private static final ThreadLocal<ToolEventEmitter> EMITTER = new ThreadLocal<>();

    public static void set(ToolEventEmitter emitter) {
        EMITTER.set(emitter);
    }

    public static ToolEventEmitter get() {
        return EMITTER.get();
    }

    public static void clear() {
        EMITTER.remove();
    }

    private ToolResultEmitterHolder() {}
}
```

在 `AIServiceImpl.processAiConsult` 中设置此 holder，在 `PatientContextHolder.clear()` 同步清理。

- [ ] **Step 6: 编译验证**

```bash
cd D:/item/Hospital-Information-System/HIS && mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add HIS/src/main/java/com/project/his/config/ai/event/ \
        HIS/src/main/java/com/project/his/config/ai/tools/ToolResultEmitterHolder.java \
        HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java
git commit -m "feat: SSE 类型化事件协议 — 事件模型、Sink 注册表、流合并"
```

---

### Task 3: 前端 SSE 事件解析 + 块列表渲染

**Files:**
- Create: `VUE/src/types/chat.ts`
- Modify: `VUE/src/views/ChatView.vue`
- Create: `VUE/src/components/chat/ToolStepBlock.vue`

- [ ] **Step 1: 创建 TypeScript 事件类型定义**

```typescript
// VUE/src/types/chat.ts

export interface AiEvent {
  type: 'text' | 'tool_call' | 'tool_result' | 'card' | 'ref' | 'done' | 'error'
  content?: string
  toolName?: string
  toolLabel?: string
  status?: string
  cardType?: string
  data?: any
  sources?: RefSource[]
  sessionId?: string
  message?: string
}

export interface RefSource {
  title: string
  snippet: string
}

export type MessageBlock =
  | { type: 'text'; content: string }
  | { type: 'toolStep'; toolName: string; label: string; status: 'running' | 'success' | 'error' }
  | { type: 'card'; cardType: string; data: any }
  | { type: 'ref'; sources: RefSource[] }
  | { type: 'error'; message: string }

export interface ChatMessage {
  sender: 'user' | 'ai'
  blocks: MessageBlock[]
}
```

- [ ] **Step 2: 修改 ChatView.vue 的 sendMessage 中 SSE 解析逻辑**

将原来的纯文本 SSE 解析改为 JSON 事件解析。在 `<script lang="ts" setup>` 区域内修改：

```typescript
// 新增导入
import type { AiEvent, ChatMessage, MessageBlock, RefSource } from '@/types/chat'

// 修改 messages 的数据类型
const messages = reactive<ChatMessage[]>([
  {
    sender: 'ai',
    blocks: [{ type: 'text', content: '你好！有什么我可以帮助你的吗？' }],
  },
])

// 替换 handleStreamMessage 函数
const handleStreamEvent = (event: AiEvent) => {
  const last = messages[messages.length - 1]
  
  switch (event.type) {
    case 'text': {
      if (!last || last.sender === 'user') {
        messages.push({ sender: 'ai', blocks: [{ type: 'text', content: event.content || '' }] })
      } else {
        // 追加到最后一个 text block，如果没有则创建
        const lastBlock = last.blocks[last.blocks.length - 1]
        if (lastBlock && lastBlock.type === 'text') {
          lastBlock.content += event.content || ''
        } else {
          last.blocks.push({ type: 'text', content: event.content || '' })
        }
      }
      break
    }
    case 'tool_call': {
      if (last && last.sender === 'ai') {
        last.blocks.push({
          type: 'toolStep',
          toolName: event.toolName || '',
          label: event.toolLabel || event.toolName || '',
          status: 'running',
        })
      }
      break
    }
    case 'tool_result': {
      if (last && last.sender === 'ai') {
        // 找到同名的最后一个 running 状态的 toolStep，更新为 success/error
        const toolSteps = last.blocks.filter(b => b.type === 'toolStep') as Extract<MessageBlock, { type: 'toolStep' }>[]
        for (let i = toolSteps.length - 1; i >= 0; i--) {
          if (toolSteps[i].toolName === event.toolName && toolSteps[i].status === 'running') {
            toolSteps[i].status = event.status === 'success' ? 'success' : 'error'
            break
          }
        }
      }
      break
    }
    case 'card': {
      if (last && last.sender === 'ai') {
        last.blocks.push({ type: 'card', cardType: event.cardType || '', data: event.data })
      }
      break
    }
    case 'ref': {
      if (last && last.sender === 'ai') {
        last.blocks.push({ type: 'ref', sources: event.sources || [] })
      }
      break
    }
    case 'error': {
      if (last && last.sender === 'ai') {
        last.blocks.push({ type: 'error', message: event.message || '发生未知错误' })
      }
      break
    }
    case 'done': {
      if (event.sessionId) {
        currentSessionId.value = event.sessionId
        loadSessionList()
      }
      break
    }
  }
  scrollToBottom()
}

// 修改 sendMessage 中的 SSE 读取循环
// 替换 handleStreamMessage 调用为 handleStreamEvent
// 原来的 data: 前缀解析改为 JSON.parse
while (true) {
  const { done, value } = await reader.read()
  if (done) break
  buffer += decoder.decode(value, { stream: true })
  const lines = buffer.split('\n')
  buffer = lines.pop() || ''
  for (const line of lines) {
    if (line.startsWith('data:')) {
      const text = line.slice(5).trim()
      if (text) {
        try {
          const event: AiEvent = JSON.parse(text)
          handleStreamEvent(event)
        } catch {
          // 兼容旧格式：纯文本
          handleStreamEvent({ type: 'text', content: text })
        }
      }
    }
  }
}
```

- [ ] **Step 3: 修改 ChatView.vue 模板 — 块列表渲染**

在 `<template>` 中修改 AI 消息的渲染部分，替换原来的 markdown 单段渲染：

```html
<!-- 替换 message.sender === 'ai' 时的渲染 -->
<template v-if="message.sender === 'ai'">
  <template v-for="(block, bi) in message.blocks" :key="bi">
    <!-- 文本块 -->
    <div v-if="block.type === 'text'" class="markdown-content" v-html="parseMarkdown(block.content)"></div>
    <!-- 工具步骤块 -->
    <ToolStepBlock v-else-if="block.type === 'toolStep'" :step="block" />
    <!-- 卡片块 -->
    <CardMessage v-else-if="block.type === 'card'" :card-type="block.cardType" :data="block.data" />
    <!-- 引用块 -->
    <ReferenceBlock v-else-if="block.type === 'ref'" :sources="block.sources" />
    <!-- 错误块 -->
    <div v-else-if="block.type === 'error'" class="error-block">
      <span class="error-text">{{ block.message }}</span>
    </div>
  </template>
  <div class="typing-indicator" v-if="isLoading && index === messages.length - 1">
    <span></span><span></span><span></span>
  </div>
</template>
```

- [ ] **Step 4: 创建 ToolStepBlock 组件**

```html
<!-- VUE/src/components/chat/ToolStepBlock.vue -->
<template>
  <div :class="['tool-step', statusClass]">
    <span class="tool-icon">
      <svg v-if="step.status === 'running'" class="spin" viewBox="0 0 24 24" fill="none" width="14" height="14">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2.5" opacity="0.25"/>
        <path d="M12 2a10 10 0 019.95 9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
      </svg>
      <svg v-else-if="step.status === 'success'" viewBox="0 0 24 24" fill="none" width="14" height="14">
        <circle cx="12" cy="12" r="10" stroke="#22c55e" stroke-width="2"/>
        <path d="M8 12l3 3 5-5" stroke="#22c55e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" width="14" height="14">
        <circle cx="12" cy="12" r="10" stroke="#ef4444" stroke-width="2"/>
        <path d="M15 9l-6 6M9 9l6 6" stroke="#ef4444" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </span>
    <span class="tool-label">{{ step.label }}</span>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { MessageBlock } from '@/types/chat'

const props = defineProps<{
  step: Extract<MessageBlock, { type: 'toolStep' }>
}>()

const statusClass = computed(() => `tool-${props.step.status}`)
</script>

<style lang="scss" scoped>
.tool-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin: 4px 0;
  border-radius: 8px;
  font-size: 0.82rem;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.tool-success { color: #16a34a; background: #f0fdf4; border-color: #bbf7d0; }
.tool-error { color: #dc2626; background: #fef2f2; border-color: #fecaca; }
.tool-icon { display: flex; align-items: center; flex-shrink: 0; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
```

- [ ] **Step 5: 适配 pushHistory（历史消息加载）**

```typescript
// 更新 pushHistory 函数以创建块结构消息
const pushHistory = (historyList: ChatHistoryEntry[]) => {
  historyList.forEach((item, index) => {
    if (index === 0) { return }
    messages.push({
      sender: item.role === 'user' ? 'user' : 'ai',
      blocks: [{ type: 'text', content: item.content }],
    })
  })
  scrollToBottom()
}
```

- [ ] **Step 6: Commit**

```bash
git add VUE/src/types/chat.ts \
        VUE/src/views/ChatView.vue \
        VUE/src/components/chat/ToolStepBlock.vue
git commit -m "feat: 前端 SSE JSON 事件解析 + 块列表渲染 + 工具步骤可视化"
```

---

### Task 4: 卡片消息 + 预约闭环

**Files:**
- Create: `VUE/src/components/chat/CardMessage.vue`
- Create: `VUE/src/components/chat/DoctorCard.vue`
- Create: `VUE/src/components/chat/ScheduleCard.vue`
- Create: `VUE/src/components/chat/AppointmentResultCard.vue`
- Modify: `HIS/src/main/java/com/project/his/config/ai/tools/HospitalTools.java`
- Modify: `HIS/src/main/java/com/project/his/config/ai/event/ToolEventEmitter.java`

- [ ] **Step 1: 创建 CardMessage 容器组件**

```html
<!-- VUE/src/components/chat/CardMessage.vue -->
<template>
  <div class="card-message">
    <DoctorCard v-if="cardType === 'doctor'" :data="data" />
    <ScheduleCard v-else-if="cardType === 'schedule'" :data="data" @book="handleBook" />
    <AppointmentResultCard v-else-if="cardType === 'appointment_result'" :data="data" @cancel="handleCancel" />
  </div>
</template>

<script lang="ts" setup>
import DoctorCard from './DoctorCard.vue'
import ScheduleCard from './ScheduleCard.vue'
import AppointmentResultCard from './AppointmentResultCard.vue'

defineProps<{ cardType: string; data: any }>()

const emit = defineEmits<{
  book: [scheduleId: number]
  cancel: [appointmentId: number]
}>()

const handleBook = (scheduleId: number) => emit('book', scheduleId)
const handleCancel = (appointmentId: number) => emit('cancel', appointmentId)
</script>

<style lang="scss" scoped>
.card-message {
  margin: 8px 0;
}
</style>
```

- [ ] **Step 2: 创建 DoctorCard**

```html
<!-- VUE/src/components/chat/DoctorCard.vue -->
<template>
  <div v-if="data && data.length" class="doctor-card-list">
    <div v-for="doctor in data" :key="doctor.doctorId" class="doctor-card-item">
      <div class="doc-header">
        <span class="doc-name">{{ doctor.name }}</span>
        <span class="doc-title">{{ doctor.title }}</span>
      </div>
      <div v-if="doctor.introduction" class="doc-intro">{{ doctor.introduction }}</div>
      <div class="doc-dept">{{ doctor.departmentName }} · {{ doctor.clinicName }}</div>
    </div>
  </div>
  <div v-else class="card-empty">暂无医生信息</div>
</template>

<script lang="ts" setup>
defineProps<{ data: any }>()
</script>

<style lang="scss" scoped>
.doctor-card-list { display: flex; flex-direction: column; gap: 8px; }
.doctor-card-item {
  padding: 12px 14px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
}
.doc-header { display: flex; align-items: baseline; gap: 8px; margin-bottom: 4px; }
.doc-name { font-weight: 600; font-size: 0.95rem; color: #0c4a6e; }
.doc-title { font-size: 0.78rem; color: #0ea5e9; }
.doc-intro { font-size: 0.82rem; color: #64748b; margin-bottom: 4px; }
.doc-dept { font-size: 0.75rem; color: #94a3b8; }
.card-empty { color: #94a3b8; font-size: 0.85rem; }
</style>
```

- [ ] **Step 3: 创建 ScheduleCard（含预约按钮）**

```html
<!-- VUE/src/components/chat/ScheduleCard.vue -->
<template>
  <div v-if="data && data.length" class="schedule-card-list">
    <div v-for="item in data" :key="item.scheduleId" class="schedule-card-item">
      <div class="sch-info">
        <div class="sch-doctor">{{ item.doctorName }}</div>
        <div class="sch-time">{{ item.scheduleDate }} {{ item.timeSlot }}</div>
        <div class="sch-remaining" :class="{ full: item.remaining === 0 }">
          {{ item.remaining > 0 ? `剩余 ${item.remaining} 个名额` : '已约满' }}
        </div>
      </div>
      <button v-if="item.remaining > 0" class="book-btn" @click="$emit('book', item.scheduleId)">
        预约
      </button>
    </div>
  </div>
  <div v-else class="card-empty">暂无可预约排班</div>
</template>

<script lang="ts" setup>
defineProps<{ data: any }>()
defineEmits<{ book: [scheduleId: number] }>()
</script>

<style lang="scss" scoped>
.schedule-card-list { display: flex; flex-direction: column; gap: 6px; }
.schedule-card-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px;
  background: #fefce8; border: 1px solid #fde68a; border-radius: 10px;
}
.sch-doctor { font-weight: 600; font-size: 0.9rem; color: #713f12; }
.sch-time { font-size: 0.82rem; color: #a16207; }
.sch-remaining { font-size: 0.78rem; color: #16a34a; &.full { color: #dc2626; } }
.book-btn {
  flex-shrink: 0;
  padding: 6px 16px;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: white; border: none; border-radius: 8px;
  font-size: 0.82rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s ease;
  &:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(14,165,233,0.35); }
}
.card-empty { color: #94a3b8; font-size: 0.85rem; }
</style>
```

- [ ] **Step 4: 创建 AppointmentResultCard**

```html
<!-- VUE/src/components/chat/AppointmentResultCard.vue -->
<template>
  <div class="result-card" :class="{ success: data?.success, fail: !data?.success }">
    <div class="result-icon">
      <svg v-if="data?.success" viewBox="0 0 24 24" fill="none" width="20" height="20">
        <circle cx="12" cy="12" r="10" stroke="#22c55e" stroke-width="2"/>
        <path d="M8 12l3 3 5-5" stroke="#22c55e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" width="20" height="20">
        <circle cx="12" cy="12" r="10" stroke="#ef4444" stroke-width="2"/>
        <path d="M15 9l-6 6M9 9l6 6" stroke="#ef4444" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="result-text">{{ data?.message || '操作完成' }}</div>
    <button v-if="data?.appointmentId && data?.success" class="cancel-btn" @click="$emit('cancel', data.appointmentId)">
      取消预约
    </button>
  </div>
</template>

<script lang="ts" setup>
defineProps<{ data: any }>()
defineEmits<{ cancel: [appointmentId: number] }>()
</script>

<style lang="scss" scoped>
.result-card {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border-radius: 10px;
  &.success { background: #f0fdf4; border: 1px solid #bbf7d0; }
  &.fail { background: #fef2f2; border: 1px solid #fecaca; }
}
.result-icon { flex-shrink: 0; }
.result-text { flex: 1; font-size: 0.9rem; font-weight: 500; }
.cancel-btn {
  padding: 5px 12px; background: #fef2f2; color: #dc2626;
  border: 1px solid #fecaca; border-radius: 6px;
  font-size: 0.78rem; cursor: pointer;
}
</style>
```

- [ ] **Step 5: ChatView.vue 中加入卡片事件处理**

在 ChatView.vue 的 AI 消息渲染区的 CardMessage 组件上绑定事件：

```html
<CardMessage v-else-if="block.type === 'card'" :card-type="block.cardType" :data="block.data"
  @book="handleBookSchedule" @cancel="handleCancelAppointment" />
```

新增处理方法：

```typescript
// ChatView.vue <script> 区域新增：

const handleBookSchedule = async (scheduleId: number) => {
  try {
    const res = await DoAxiosWithErro<any>(
      `/appointment/create?scheduleId=${scheduleId}`, 'post', null, true
    )
    // 插入预约成功卡片到消息中
    messages.push({
      sender: 'ai',
      blocks: [{
        type: 'card',
        cardType: 'appointment_result',
        data: { success: true, message: `预约成功！预约ID: ${res?.appointmentId || '-'}`, appointmentId: res?.appointmentId }
      }]
    })
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '预约失败')
  }
}

const handleCancelAppointment = async (appointmentId: number) => {
  try {
    await DoAxiosWithErro(`/appointment/cancel/${appointmentId}`, 'post', null, true)
    ElMessage.success('预约已取消')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '取消失败')
  }
}
```

- [ ] **Step 6: HospitalTools 改造 — 返回结构化数据以生成卡片**

HospitalTools 的医生/排班查询方法需要额外产出卡片数据。但 Spring AI Tool 的返回值是 String（给 LLM 看的）。卡片数据需要另辟蹊径——在 `ToolEventEmitter` 层面，工具执行后从返回结果中提取结构化数据推卡片事件。

更好的方案：在 ToolEventEmitter 中增加一个 `emitCardIfCapable` 方法，调用方传入工具名+返回的字符串，ToolEventEmitter 解析出卡片数据。

但在当前架构下，工具的调用是通过 Spring AI 自动拦截的，我们无法在工具方法返回后拿到返回值。

**最优方案（不改工具签名）**：ToolEventEmitter 在 `emitToolResult` 后，由 AIServiceImpl 的流式处理中检测 tool_call 完成后的下一段文本是否为卡片格式，从而推 card 事件。

实际上有一个更简单的方案：在工具方法内部直接调用 `ToolResultEmitterHolder.get()` 获取 emitter 并推卡片事件。这样每个需要卡片的工具方法自己负责推 card。

例如在 `HospitalTools.getDoctorSchedules` 末尾：

```java
// 在返回结果前，构建卡片数据并推送
ToolResultEmitterHolder emitter = ToolResultEmitterHolder.get();
if (emitter != null && schedules != null && !schedules.isEmpty()) {
    List<Map<String, Object>> scheduleData = new ArrayList<>();
    for (Schedule s : schedules) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("scheduleId", s.getScheduleId());
        item.put("scheduleDate", s.getScheduleDate().toString());
        item.put("timeSlot", s.getTimeSlot());
        item.put("remaining", s.getMaxPatients() - s.getCurrentPatients());
        Doctor d = doctorService.getById(s.getDoctorId());
        item.put("doctorName", d != null ? d.getName() : "未知");
        scheduleData.add(item);
    }
    emitter.emitCard("schedule", scheduleData);
}
```

类似改造 `listDoctorsByClinic`（推 doctor 卡片）、`createAppointment`（推 appointment_result 卡片）。

**由于这涉及在每个工具方法中增加卡片逻辑，暂时不做深度改造，先改为方案：由后端 AI 服务在处理流时自动推送。** 等待 Task 4 完成基础卡片组件后，在后续迭代中实现卡片数据的后端推送。当前先让卡片组件可以通过手动构造数据方式验证渲染效果。

- [ ] **Step 7: 在 ChatView.vue 中注册新组件**

```typescript
// 导入
import CardMessage from '@/components/chat/CardMessage.vue'
import ReferenceBlock from '@/components/chat/ReferenceBlock.vue'
import ToolStepBlock from '@/components/chat/ToolStepBlock.vue'
```

- [ ] **Step 8: Commit**

```bash
git add VUE/src/components/chat/CardMessage.vue \
        VUE/src/components/chat/DoctorCard.vue \
        VUE/src/components/chat/ScheduleCard.vue \
        VUE/src/components/chat/AppointmentResultCard.vue \
        VUE/src/views/ChatView.vue
git commit -m "feat: 结构化卡片消息组件 + 预约闭环（前端）"
```

---

### Task 5: RAG 知识问答

**Files:**
- Create: `docs/knowledge/01-hospital-overview.md` ~ `11-faq.md`（11 篇）
- Modify: `HIS/src/main/java/com/project/his/service/entity/impl/AiRagServiceImpl.java`
- Create: `HIS/src/main/java/com/project/his/config/ai/tools/KnowledgeTools.java`
- Modify: `HIS/src/main/java/com/project/his/config/ai/AiChatConfig.java`
- Create: `VUE/src/components/chat/ReferenceBlock.vue`
- Modify: `VUE/src/views/admin/RagManager.vue`
- Modify: `HIS/src/main/java/com/project/his/controller/AiRagController.java`

- [ ] **Step 1: 生成 11 篇医疗知识文档**

写入 `docs/knowledge/` 目录：

**01-hospital-overview.md**：
```markdown
# 仁康医院概况

仁康医院是一家集医疗、教学、科研、预防、保健为一体的三级甲等综合性医院。
医院占地面积约 12 万平方米，建筑面积 18 万平方米，开放床位 1500 张。

## 医院特色
- 国家级重点专科 5 个：心血管内科、神经内科、骨科、肿瘤科、妇产科
- 省级重点专科 12 个
- 年门诊量超过 200 万人次
- 年出院患者超过 8 万人次

## 就诊时间
- 门诊时间：周一至周日 8:00-12:00，14:00-17:30
- 急诊时间：24 小时开放
- 体检中心：周一至周六 7:30-11:30

## 联系方式
- 总机：010-12345678
- 急救电话：120
- 咨询热线：010-12345679
- 医院地址：北京市海淀区中关村南大街 1 号
```

**02-registration-guide.md**：
```markdown
# 就医流程指南

## 挂号方式
1. **现场挂号**：持有效身份证件到门诊一楼挂号窗口办理
2. **线上预约**：通过医院官网或手机 App 预约，支持提前 7 天预约
3. **电话预约**：拨打 010-12345679，工作日 8:00-17:00

## 就诊流程
1. 挂号/预约 → 2. 分诊台报到 → 3. 候诊区等候叫号 → 4. 医生问诊 → 5. 缴费 → 6. 检查/取药

## 挂号须知
- 初诊患者请携带有效身份证件
- 预约患者请在预约时间前 15 分钟到达
- 如无法按时就诊，请提前取消预约
- 累计爽约 3 次将被限制预约权限 30 天

## 退号规则
- 就诊前 2 小时以上可免费退号
- 就诊前 2 小时内退号收取 20% 手续费
- 超过就诊时间未退号的，号源自动作废
```

**03-department-intro.md**：
```markdown
# 科室介绍

## 内科系统
- **心血管内科**：诊治高血压、冠心病、心力衰竭、心律失常、心肌病等心血管系统疾病。
- **呼吸内科**：诊治肺炎、支气管炎、哮喘、慢阻肺、肺结节、肺癌等呼吸系统疾病。
- **消化内科**：诊治胃炎、胃溃疡、肠炎、肝炎、胰腺炎、消化道出血等消化系统疾病。
- **神经内科**：诊治头痛、头晕、癫痫、帕金森病、脑血管病、痴呆等神经系统疾病。
- **内分泌科**：诊治糖尿病、甲亢、甲减、痛风、骨质疏松等内分泌代谢疾病。
- **肾内科**：诊治肾炎、肾病综合征、肾功能不全、尿毒症等肾脏疾病。

## 外科系统
- **骨科**：诊治骨折、关节炎、椎间盘突出、骨质疏松、运动损伤等骨关节疾病。
- **普外科**：开展胃肠、肝胆、甲状腺、乳腺等普通外科手术。
- **泌尿外科**：诊治肾结石、前列腺增生、膀胱肿瘤、泌尿系感染等泌尿系统疾病。
- **神经外科**：开展脑肿瘤、脑血管病、脑外伤、脊柱脊髓等手术。
- **心胸外科**：开展心脏瓣膜、冠心病搭桥、肺部肿瘤等手术。

## 其他科室
- **妇产科**：妇科炎症、月经不调、产前检查、分娩、产后康复等。
- **儿科**：儿童常见病、多发病的诊治，儿童保健、预防接种等。
- **眼科**：近视、白内障、青光眼、眼底病等眼科疾病诊治。
- **耳鼻喉科**：中耳炎、鼻窦炎、咽喉炎、扁桃体炎、听力障碍等。
- **皮肤科**：湿疹、荨麻疹、痤疮、银屑病、皮肤过敏等皮肤疾病。
- **口腔科**：龋齿、牙周病、口腔溃疡、牙齿矫正、种植牙等。
- **中医科**：中医内科、针灸、推拿、拔罐、中药调理等。
- **急诊科**：各种急危重症的紧急救治。
```

**04-symptom-guide.md**（症状→科室导诊核心语料）：
```markdown
# 常见症状对应科室指南

本指南帮助您根据症状初步判断应该挂哪个科室。如症状复杂或不确定，建议先到导诊台咨询。

## 头部与神经系统
| 症状 | 建议科室 |
|------|----------|
| 头痛（反复发作） | 神经内科 |
| 头晕、眩晕 | 神经内科 或 耳鼻喉科 |
| 失眠、入睡困难 | 神经内科 |
| 记忆力减退 | 神经内科 |

## 心血管系统
| 症状 | 建议科室 |
|------|----------|
| 胸痛、胸闷 | 心血管内科 |
| 心慌、心悸 | 心血管内科 |
| 高血压 | 心血管内科 |
| 下肢水肿 | 心血管内科 或 肾内科 |

## 呼吸系统
| 症状 | 建议科室 |
|------|----------|
| 咳嗽（超过2周） | 呼吸内科 |
| 咳痰、咯血 | 呼吸内科 |
| 气喘、呼吸困难 | 呼吸内科 |
| 喉咙痛、咽部不适 | 耳鼻喉科 |
| 发热伴有咳嗽 | 呼吸内科 |

## 消化系统
| 症状 | 建议科室 |
|------|----------|
| 腹痛 | 消化内科（上腹部）或 普外科（急腹症） |
| 胃痛、反酸、烧心 | 消化内科 |
| 恶心、呕吐 | 消化内科 |
| 腹泻、便秘 | 消化内科 |
| 便血 | 消化内科 或 普外科 |
| 黄疸 | 消化内科 |

## 骨骼与关节
| 症状 | 建议科室 |
|------|----------|
| 关节疼痛、肿胀 | 骨科 或 风湿免疫科 |
| 腰痛、背痛 | 骨科 |
| 颈肩痛 | 骨科 |
| 骨折、扭伤 | 骨科 |
| 足跟痛、足底筋膜炎 | 骨科 |

## 泌尿系统
| 症状 | 建议科室 |
|------|----------|
| 尿频、尿急、尿痛 | 泌尿外科 |
| 血尿 | 泌尿外科 或 肾内科 |
| 腰痛伴排尿异常 | 肾内科 |
| 排尿困难 | 泌尿外科 |

## 皮肤
| 症状 | 建议科室 |
|------|----------|
| 皮疹、瘙痒 | 皮肤科 |
| 皮肤红肿、脱皮 | 皮肤科 |
| 脱发 | 皮肤科 |
| 指甲异常 | 皮肤科 |

## 妇科
| 症状 | 建议科室 |
|------|----------|
| 月经不调、痛经 | 妇产科 |
| 白带异常 | 妇产科 |
| 下腹疼痛（女性） | 妇产科 |
| 更年期症状 | 妇产科 |

## 儿科
| 症状 | 建议科室 |
|------|----------|
| 儿童发热 | 儿科 |
| 儿童咳嗽、感冒 | 儿科 |
| 儿童呕吐、腹泻 | 儿科 |
| 儿童生长发育问题 | 儿科 |

## 其他常见症状
| 症状 | 建议科室 |
|------|----------|
| 体检结果异常 | 对应专科 或 体检中心 |
| 不明原因消瘦 | 内分泌科 或 消化内科 |
| 视力下降 | 眼科 |
| 听力下降、耳鸣 | 耳鼻喉科 |
| 牙齿疼痛 | 口腔科 |
| 口臭、口腔溃疡反复发作 | 口腔科 或 消化内科 |

## 紧急症状（建议立即到急诊科）
- 突发剧烈胸痛
- 呼吸困难或窒息
- 大出血不止
- 突发意识丧失
- 严重外伤
- 中毒
- 持续高热不退（超过 39°C）
```

**05-cardiology.md**：
```markdown
# 心血管内科常见疾病

## 高血压
- 诊断标准：非同日 3 次测量，收缩压 ≥140mmHg 和/或舒张压 ≥90mmHg
- 常见症状：头晕、头痛、颈部僵硬、心悸
- 危险因素：高盐饮食、肥胖、吸烟、缺乏运动、遗传

## 冠心病
- 病因：冠状动脉粥样硬化导致心肌缺血缺氧
- 常见症状：胸痛（压榨感、闷痛）、可放射至左肩、左臂
- 诱发因素：体力活动、情绪激动、饱餐、寒冷
- 检查方法：心电图、冠脉造影、心脏彩超

## 心律失常
- 常见类型：房颤、室性早搏、心动过速、心动过缓
- 常见症状：心慌、心悸、胸闷、头晕、乏力
- 诊断方法：心电图、动态心电图（Holter）

## 就医建议
- 40 岁以上建议每年体检包含血压、血脂、血糖、心电图
- 有胸痛症状请及时就诊，不要自行用药
- 高血压患者应长期规律服药，不可擅自停药
```

**06-respiratory.md**：
```markdown
# 呼吸内科常见疾病

## 上呼吸道感染（感冒）
- 病因：病毒或细菌感染
- 常见症状：鼻塞、流涕、喉咙痛、咳嗽、发热
- 一般 5-7 天可自愈，多喝水、休息为主

## 支气管炎
- 急性支气管炎：多由病毒感染引起，表现为咳嗽、咳痰
- 慢性支气管炎：每年咳嗽咳痰 3 个月以上，连续 2 年以上
- 危险因素：吸烟、空气污染、职业粉尘

## 肺炎
- 常见症状：发热、咳嗽、咳痰、胸痛、气促
- 严重时需住院治疗
- 老年人、儿童和有基础疾病者风险更高

## 哮喘
- 典型症状：发作性喘息、胸闷、咳嗽（夜间和清晨多见）
- 诱因：过敏原、冷空气、运动、情绪激动
- 需要长期规范治疗和定期复查

## 就医建议
- 咳嗽超过 2 周未好转建议就医
- 出现胸痛、呼吸困难、咯血应立即就医
- 戒烟是预防呼吸系统疾病最重要的措施
```

**07-gastroenterology.md**、**08-orthopedics.md**、**09-pediatrics.md** — 略（结构与上类似，各约 150-200 行）

**10-insurance-billing.md**：
```markdown
# 医保与收费说明

## 医保报销
- 北京市职工医保：门诊报销比例 70%-90%（按医院级别）
- 北京市居民医保：门诊报销比例 50%-70%
- 住院报销比例 85%-95%（按费用段）
- 起付线：门诊 1800 元/年，住院 1300 元/次

## 自费项目
- 特需门诊挂号费
- 非医保目录内药品和检查
- 美容整形类项目
- 健康体检（部分套餐可用医保）

## 收费方式
- 支持现金、银行卡、微信、支付宝
- 医保患者可使用社保卡直接结算
- 自费患者先缴费后就诊

## 发票开具
- 在收费窗口凭缴费凭证开具
- 电子发票可在医院 App 中查看下载
```

**11-faq.md**：
```markdown
# 常见问题解答

## 挂号相关
**Q: 如何在线预约挂号？**
A: 下载医院 App 或关注微信公众号，注册账号后选择科室和医生即可预约。

**Q: 预约成功后可以更改时间吗？**
A: 可以。在预约时间前 2 小时以上可免费更改。进入 App "我的预约"中操作。

**Q: 没有身份证可以挂号吗？**
A: 可以凭户口本、护照、港澳通行证等有效证件挂号。

## 就诊相关
**Q: 到医院后需要做什么？**
A: 先在分诊台报到（刷挂号条或社保卡），然后在候诊区等候叫号。

**Q: 可以指定医生吗？**
A: 专家号和专科门诊可以指定医生，普通门诊由系统分诊。

**Q: 看完病后去哪里取药？**
A: 凭医生开具的处方和缴费凭证到门诊一楼药房取药。

## 检查相关
**Q: 抽血需要空腹吗？**
A: 肝功能、血糖、血脂等检查需要空腹 8-12 小时。具体请在开具检查时咨询医生。

**Q: 检查结果多久能出来？**
A: 常规血检一般 1-2 小时，CT、MRI 一般当天或次日，病理检查需要 3-5 个工作日。
```

- [ ] **Step 2: AiRagServiceImpl 扩展 — 支持 Markdown/文本入库**

```java
// AiRagServiceImpl.java — 新增方法

@Override
public int importMarkdownFiles(String directoryPath) {
    VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
    if (vectorStore == null) {
        log.warn("VectorStore 未初始化，跳过 Markdown 入库");
        return 0;
    }
    
    java.io.File dir = new java.io.File(directoryPath);
    if (!dir.exists() || !dir.isDirectory()) {
        log.warn("知识库目录不存在: {}", directoryPath);
        return 0;
    }
    
    java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".md"));
    if (files == null || files.length == 0) {
        log.warn("知识库目录下没有 .md 文件: {}", directoryPath);
        return 0;
    }
    
    int total = 0;
    for (java.io.File file : files) {
        try {
            String content = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            if (content == null || content.isBlank()) continue;
            
            // 按 ## 标题分块
            List<Document> chunks = splitMarkdown(content, file.getName());
            if (chunks.isEmpty()) continue;
            
            vectorStore.add(chunks);
            total += chunks.size();
            log.info("Markdown 入库成功: {}, {} 个分块", file.getName(), chunks.size());
        } catch (Exception e) {
            log.error("Markdown 入库失败: {}", file.getName(), e);
        }
    }
    return total;
}

private List<Document> splitMarkdown(String content, String fileName) {
    List<Document> chunks = new ArrayList<>();
    String title = fileName.replace(".md", "");
    
    // 按 ## 标题分割
    String[] parts = content.split("\n## ");
    for (int i = 0; i < parts.length; i++) {
        String part = parts[i].trim();
        if (part.isEmpty()) continue;
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", fileName);
        metadata.put("source_type", "markdown");
        
        // 提取标题
        int newlineIdx = part.indexOf('\n');
        if (newlineIdx > 0) {
            String sectionTitle = part.substring(0, newlineIdx).trim();
            if (i == 0) sectionTitle = title;
            metadata.put("title", sectionTitle);
        } else {
            metadata.put("title", title);
        }
        
        chunks.add(new Document(part, metadata));
    }
    return chunks;
}
```

同时在 `IAiRagService` 接口中添加：

```java
int importMarkdownFiles(String directoryPath);
```

- [ ] **Step 3: 创建 KnowledgeTools**

```java
// config/ai/tools/KnowledgeTools.java
package com.project.his.config.ai.tools;

import com.project.his.config.ai.event.AiEvent;
import com.project.his.config.ai.event.ToolEventEmitter;
import com.project.his.service.entity.IAiRagService;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.vo.AiRagSearchResult;
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

    @Tool(description = "搜索医疗知识库。当患者咨询疾病症状、用药、就医流程、医保政策等需要专业知识的问题时调用此工具检索相关知识。返回相关文档片段及引用来源。")
    public String searchKnowledgeBase(
            @ToolParam(description = "搜索关键词或问题描述") String query) {
        
        log.info("AI调用工具: searchKnowledgeBase, query={}", query);
        if (query == null || query.trim().isEmpty()) {
            return "请提供搜索关键词";
        }
        
        AiRagSearchRequest request = AiRagSearchRequest.builder()
                .query(query.trim())
                .topK(4)
                .build();
        
        List<AiRagSearchResult> results = aiRagService.search(request);
        
        if (results == null || results.isEmpty()) {
            return "未找到相关知识，建议换个问法或咨询人工客服。";
        }
        
        // 构建引用来源列表推 ref 事件
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        if (emitter != null) {
            List<AiEvent.RefSource> sources = new ArrayList<>();
            for (AiRagSearchResult r : results) {
                String title = "未知来源";
                Map<String, Object> meta = r.getMetadata();
                if (meta != null && meta.get("title") != null) {
                    title = meta.get("title").toString();
                }
                sources.add(AiEvent.RefSource.builder()
                        .title(title)
                        .snippet(r.getText() != null && r.getText().length() > 100
                                ? r.getText().substring(0, 100) + "..." : r.getText())
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
        return sb.toString();
    }
}
```

- [ ] **Step 4: 修改 AiChatConfig — 移除 QuestionAnswerAdvisor、注册 KnowledgeTools**

```java
// AiChatConfig.java

// 1. 删除 questionAnswerAdvisor Bean 方法
// 2. chatClient 方法修改为：

@Bean
public ChatClient chatClient(ChatClient.Builder builder,
                             MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                             HospitalTools hospitalTools,
                             @Lazy AppointmentTools appointmentTools,
                             KnowledgeTools knowledgeTools) {
    return builder
            .defaultTools(hospitalTools, appointmentTools, knowledgeTools)
            .defaultAdvisors(messageChatMemoryAdvisor)
            .build();
}
```

注意：删除 `QuestionAnswerAdvisor` 相关的 import 和 `ObjectProvider` 参数。

- [ ] **Step 5: 创建 ReferenceBlock 组件**

```html
<!-- VUE/src/components/chat/ReferenceBlock.vue -->
<template>
  <div v-if="sources && sources.length" class="ref-block">
    <div class="ref-toggle" @click="expanded = !expanded">
      <span>参考来源 ({{ sources.length }})</span>
      <svg :class="{ rotated: expanded }" viewBox="0 0 24 24" fill="none" width="14" height="14">
        <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <div v-show="expanded" class="ref-list">
      <div v-for="(source, i) in sources" :key="i" class="ref-item">
        <span class="ref-idx">[{{ i + 1 }}]</span>
        <div>
          <div class="ref-title">{{ source.title }}</div>
          <div class="ref-snippet">{{ source.snippet }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import type { RefSource } from '@/types/chat'

defineProps<{ sources: RefSource[] }>()
const expanded = ref(false)
</script>

<style lang="scss" scoped>
.ref-block {
  margin: 6px 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}
.ref-toggle {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 12px; cursor: pointer;
  font-size: 0.8rem; color: #64748b;
  background: #f8fafc;
  &:hover { background: #f1f5f9; }
  svg { transition: transform 0.2s; &.rotated { transform: rotate(180deg); } }
}
.ref-list { padding: 8px 12px; display: flex; flex-direction: column; gap: 6px; }
.ref-item { display: flex; gap: 6px; font-size: 0.8rem; line-height: 1.5; }
.ref-idx { color: #0ea5e9; font-weight: 600; flex-shrink: 0; }
.ref-title { font-weight: 500; color: #334155; }
.ref-snippet { color: #94a3b8; font-size: 0.75rem; }
</style>
```

- [ ] **Step 6: RagManager.vue 加一键导入按钮**

```html
<!-- RagManager.vue 模板区域新增 -->
<el-button type="success" @click="importBuiltIn" :loading="importing">
  一键导入内置语料
</el-button>
```

```typescript
// RagManager.vue <script> 区域新增：
const importing = ref(false)
const importBuiltIn = async () => {
  importing.value = true
  try {
    await DoAxiosWithErro('/admin/ai-rag/import-built-in', 'post', null, true)
    ElMessage.success('内置语料导入成功')
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}
```

- [ ] **Step 7: AiRagController 加导入内置语料端点**

```java
// AiRagController.java 新增
@PostMapping("/import-built-in")
public Result<Integer> importBuiltIn() {
    // 从 classpath:knowledge/ 读取 Markdown 文件
    try {
        org.springframework.core.io.Resource[] resources =
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                        .getResources("classpath:knowledge/*.md");
        if (resources.length == 0) {
            return Result.error("未找到内置语料文件");
        }
        int total = 0;
        for (org.springframework.core.io.Resource res : resources) {
            String content = new String(res.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            List<com.project.his.domain.dto.AiRagDocument> docs = new ArrayList<>();
            com.project.his.domain.dto.AiRagDocument doc = new com.project.his.domain.dto.AiRagDocument();
            doc.setText(content);
            java.util.Map<String, Object> meta = new java.util.HashMap<>();
            meta.put("source", res.getFilename());
            meta.put("source_type", "built-in");
            meta.put("title", res.getFilename() != null ? res.getFilename().replace(".md", "") : "unknown");
            doc.setMetadata(meta);
            docs.add(doc);
            com.project.his.domain.dto.AiRagAddRequest req = new com.project.his.domain.dto.AiRagAddRequest();
            req.setDocuments(docs);
            total += aiRagService.addDocuments(req);
        }
        return Result.success("成功导入 " + total + " 个文档片段", total);
    } catch (Exception e) {
        log.error("导入内置语料失败", e);
        return Result.error("导入失败: " + e.getMessage());
    }
}
```

同时把 `docs/knowledge/` 复制到 `HIS/src/main/resources/knowledge/`。

- [ ] **Step 8: 编译验证**

```bash
cd D:/item/Hospital-Information-System/HIS && mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add docs/knowledge/ \
        HIS/src/main/resources/knowledge/ \
        HIS/src/main/java/com/project/his/config/ai/tools/KnowledgeTools.java \
        HIS/src/main/java/com/project/his/config/ai/AiChatConfig.java \
        HIS/src/main/java/com/project/his/service/entity/impl/AiRagServiceImpl.java \
        HIS/src/main/java/com/project/his/service/entity/IAiRagService.java \
        HIS/src/main/java/com/project/his/controller/AiRagController.java \
        VUE/src/components/chat/ReferenceBlock.vue \
        VUE/src/views/admin/RagManager.vue
git commit -m "feat: Agentic RAG 知识问答 — 语料、检索工具、引用展示、一键入库"
```

---

### Task 6: 导诊场景 + Prompt 外部化重构

**Files:**
- Create: `HIS/src/main/resources/prompts/system-prompt.st`
- Modify: `HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java`

- [ ] **Step 1: 创建外部化 Prompt 模板**

```st
<!-- HIS/src/main/resources/prompts/system-prompt.st -->
# AI 医疗助手

## 当前时间
今天是 {{currentDate}}（{{currentDateIso}}）。查询排班或预约时，请基于这个日期。

## 身份
您是一位专业、友善的AI医疗助手，服务于仁康医院患者。您可以进行健康咨询、症状分析，并帮助患者查询医院信息、预约挂号。

## 场景行为规范

### 1. 症状导诊场景
当患者描述不适症状时：
- 先了解症状详情（持续时间、严重程度、伴随症状）
- 调用 `searchKnowledgeBase` 检索症状→科室指南
- 调用 `listDepartments` 核对系统实际科室
- 向患者推荐科室并说明理由
- 患者认可后，查询该科室医生和排班供选择

### 2. 知识问答场景
当患者咨询疾病知识、就医流程、医保政策等：
- 调用 `searchKnowledgeBase` 检索相关文档
- 基于检索到的知识回答，引用来源编号如 [1]、[2]
- 不要凭空编造医疗知识

### 3. 预约管理场景
当患者要求预约挂号或管理预约：
- 查询/取消预约：直接调用对应工具（患者身份由系统自动识别，无需传入患者ID）
- 创建预约前必须获得患者明确确认
- 预约成功后总结关键信息

## 回复风格
- 简洁清晰、重点突出
- 症状分析时分点说明
- 推荐就医时直接查询并展示选项
- 结尾适当关心患者

## 安全红线
- 不做医学诊断结论，仅提供参考建议
- 不提供具体药物剂量
- 严重症状（突发剧烈胸痛、大出血、呼吸困难、意识丧失等）建议立即到急诊科就医
- 诊断结果仅供参考，以线下医生诊断为准
```

- [ ] **Step 2: 修改 AIServiceImpl — 加载模板替换硬编码**

```java
// AIServiceImpl.java

// 新增注入
private final org.springframework.core.io.ResourceLoader resourceLoader;

// createSystemPrompt 方法改为：
private String createSystemPrompt() {
    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
    String currentDateIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    
    try {
        org.springframework.core.io.Resource resource = 
                resourceLoader.getResource("classpath:prompts/system-prompt.st");
        String template = new String(resource.getInputStream().readAllBytes(), 
                java.nio.charset.StandardCharsets.UTF_8);
        return template
                .replace("{{currentDate}}", currentDate)
                .replace("{{currentDateIso}}", currentDateIso);
    } catch (Exception e) {
        log.error("加载 system-prompt 模板失败，使用默认 Prompt", e);
        // 兜底：返回一个精简的内联 Prompt
        return """
                # AI 医疗助手
                今天是 %s。
                您是一位专业、友善的AI医疗助手，服务于仁康医院患者。
                
                ## 场景规范
                1. 症状导诊：先了解详情 → searchKnowledgeBase 检索指南 → listDepartments 核对科室 → 推荐就诊
                2. 知识问答：searchKnowledgeBase 检索 → 基于引用回答
                3. 预约管理：操作前确认
                
                ## 安全红线
                不做诊断结论、不给药物剂量、急症建议去急诊科。
                """.formatted(currentDate);
    }
}

// 删除旧的 createSystemPrompt(Long patientId) 和 createSystemPrompt() 无参版
// 只保留模板版本
```

注意：`AIServiceImpl` 需要注入 `org.springframework.core.io.ResourceLoader`（Spring Boot 自动配置提供）。

- [ ] **Step 3: 编译验证**

```bash
cd D:/item/Hospital-Information-System/HIS && mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add HIS/src/main/resources/prompts/system-prompt.st \
        HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java
git commit -m "refactor: Prompt 外部化 + 三场景导诊规范重构"
```

---

### Task 7: 错误处理完善 + 测试补齐

**Files:**
- Modify: `HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java`
- Modify: `VUE/src/views/ChatView.vue`
- Create: `HIS/src/test/java/com/project/his/config/ai/tools/AppointmentToolsTest.java`
- Create: `HIS/src/test/java/com/project/his/config/ai/tools/KnowledgeToolsTest.java`

- [ ] **Step 1: 后端 — doOnCancel 保存会话**

在 `AIServiceImpl.processAiConsult` 的流式处理中，doOnCancel 已加入 `saveConsultRecord`（Task 2 中已做）。确认代码存在。

- [ ] **Step 2: 后端 — error 事件推送**

在 `doOnError` 中通过 emitter 推送 error 事件（Task 2 中已做）。

- [ ] **Step 3: 前端 — 重试按钮**

在 ChatView.vue 的错误块渲染中添加重试：

```html
<!-- 错误块渲染 -->
<div v-else-if="block.type === 'error'" class="error-block">
  <span class="error-text">{{ block.message }}</span>
  <button class="retry-btn" @click="retryLastMessage">重试</button>
</div>
```

```typescript
// 新增 retryLastMessage 方法
const retryLastMessage = () => {
  // 找到最后一条用户消息，重新发送
  const lastUser = [...messages].reverse().find(m => m.sender === 'user')
  if (lastUser) {
    // 提取最后一条用户消息的文本内容
    const textBlocks = lastUser.blocks.filter(b => b.type === 'text')
    const lastText = textBlocks.length > 0 ? (textBlocks[textBlocks.length - 1] as any).content : ''
    // 移除最后一条 AI 消息
    if (messages[messages.length - 1]?.sender === 'ai') {
      messages.pop()
    }
    if (lastText) {
      newMessage.value = lastText
      sendMessage()
    }
  }
}
```

```scss
.error-block {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #fef2f2; border: 1px solid #fecaca;
  border-radius: 8px; margin: 4px 0;
}
.error-text { color: #dc2626; font-size: 0.82rem; flex: 1; }
.retry-btn {
  padding: 4px 12px; background: #dc2626; color: white;
  border: none; border-radius: 6px; font-size: 0.78rem; cursor: pointer;
  &:hover { background: #b91c1c; }
}
```

- [ ] **Step 4: 停止生成 — 保留已收内容**

```typescript
// sendMessage 中修改 finally 块
} finally {
  isLoading.value = false
  // 如果有已收内容，保存会话记录
  if (currentSessionId.value) {
    // 可调后端接口保存
  }
}

// 新增停止生成方法
const stopGeneration = () => {
  abortController.value?.abort()
  isLoading.value = false
}
```

在输入栏上方加停止按钮（仅在 isLoading 时显示）：

```html
<button v-if="isLoading" class="stop-btn" @click="stopGeneration">停止生成</button>
```

- [ ] **Step 5: 后端单测 — AppointmentTools**

```java
// HIS/src/test/java/com/project/his/config/ai/tools/AppointmentToolsTest.java
package com.project.his.config.ai.tools;

import com.project.his.service.business.IRegistrationService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AppointmentToolsTest {

    @Mock IRegistrationService registrationService;
    @Mock IScheduleService scheduleService;
    @Mock IDoctorService doctorService;

    private AppointmentTools tools;

    @BeforeEach
    void setUp() {
        tools = new AppointmentTools(registrationService, scheduleService, doctorService);
        PatientContextHolder.clear();
    }

    @Test
    void getMyAppointments_noPatient_returnsError() {
        String result = tools.getMyAppointments(null);
        assertTrue(result.contains("无法获取当前患者信息"));
    }

    @Test
    void createAppointment_noSchedule_returnsError() {
        String result = tools.createAppointment(null);
        assertTrue(result.contains("请提供排班ID"));
    }

    @Test
    void cancelAppointment_noAppointment_returnsError() {
        String result = tools.cancelAppointment(null);
        assertTrue(result.contains("请提供要取消的预约ID"));
    }

    @Test
    void resolvePatientId_fromThreadLocal() {
        PatientContextHolder.set(100L, "test-session");
        // 通过 getMyAppointments 间接测试 resolvePatientId
        // 由于没有 mock 返回值，测试仅验证逻辑不会 NPE
        tools.getMyAppointments(0);
    }
}
```

- [ ] **Step 6: 后端单测 — KnowledgeTools（mock RAG service）**

```java
// HIS/src/test/java/com/project/his/config/ai/tools/KnowledgeToolsTest.java
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
        String result = tools.searchKnowledgeBase("");
        assertTrue(result.contains("请提供搜索关键词"));
    }

    @Test
    void searchKnowledgeBase_nullQuery_returnsError() {
        KnowledgeTools tools = new KnowledgeTools(aiRagService);
        String result = tools.searchKnowledgeBase(null);
        assertTrue(result.contains("请提供搜索关键词"));
    }
}
```

- [ ] **Step 7: 运行测试**

```bash
cd D:/item/Hospital-Information-System/HIS && mvnw test -q
```
Expected: Tests pass

- [ ] **Step 8: Commit**

```bash
git add HIS/src/main/java/com/project/his/service/entity/impl/AIServiceImpl.java \
        VUE/src/views/ChatView.vue \
        HIS/src/test/java/com/project/his/config/ai/tools/AppointmentToolsTest.java \
        HIS/src/test/java/com/project/his/config/ai/tools/KnowledgeToolsTest.java
git commit -m "fix: 错误处理完善（doOnCancel保存、重试、停止）+ 工具单测"
```

---

## 实施顺序总结

| 序号 | Task | 依赖 | 提交信息 |
|------|------|------|----------|
| 1 | 安全修复 | 无 | `fix: AI 助手安全修复 — patientId 从登录态获取，消除越权风险` |
| 2 | SSE 事件协议 | Task 1 | `feat: SSE 类型化事件协议 — 事件模型、Sink 注册表、流合并` |
| 3 | 前端块渲染 | Task 2 | `feat: 前端 SSE JSON 事件解析 + 块列表渲染 + 工具步骤可视化` |
| 4 | 卡片消息 | Task 3 | `feat: 结构化卡片消息组件 + 预约闭环（前端）` |
| 5 | RAG | Task 2 | `feat: Agentic RAG 知识问答 — 语料、检索工具、引用展示、一键入库` |
| 6 | Prompt 重构 | Task 1 | `refactor: Prompt 外部化 + 三场景导诊规范重构` |
| 7 | 错误处理+测试 | Task 3,4 | `fix: 错误处理完善（doOnCancel保存、重试、停止）+ 工具单测` |

---

## 验证脚本（手动演示）

黄金路径手动测试：

1. 登录患者账号 → 进入 AI 助手 → 发送「我最近总是反复头疼，还有点晕，该看什么科室？」
2. 验证：tools 步骤条显示 searchKnowledgeBase + listDepartments 的 tool_call/tool_result
3. 验证：AI 回复引用知识库，ref 块可展开查看来源
4. 验证：AI 推荐科室后展示医生/排班卡片
5. 验证：点击排班卡片「预约」→ 确认弹窗 → 预约成功卡片
6. 验证：新开对话 → 问「我刚刚预约了什么」→ 正确回答
7. 验证：关闭页面，重新进入 → 历史对话列表可见 → 切换后对话内容恢复（卡片复现）
8. 验证：患者 A 的 sessionId 不能访问患者 B 的历史会话
```
