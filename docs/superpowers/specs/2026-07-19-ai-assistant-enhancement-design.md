# AI 助手完善设计文档

日期：2026-07-19
状态：已确认（用户逐节批准）
定位：毕设/求职作品——方案取舍以「技术亮点可讲、演示完整不翻车」为先

## 1. 背景与目标

HIS 现有 AI 助手（Spring Boot 3.4.3 + Spring AI 1.0.3 + 智谱 GLM-4.5，Vue 3 前端）已具备 9 个工具的 Function Calling、SSE 流式输出、Redis 会话记忆 + MySQL 持久化，覆盖患者端诊前咨询与预约挂号。

本次完善目标：

1. **RAG 知识问答**：激活空转的 PgVector 基础设施，实现 Agentic RAG 带引用来源
2. **智能导诊闭环**：症状 → 科室推荐 → 医生/排班卡片 → 一键预约的完整演示路径
3. **交互体验**：工具调用过程可视化、结构化卡片消息
4. **工程化**：安全修复（越权、并发串号）、Prompt/工具重构

不做（YAGNI）：多 Agent 编排、LLM 生成 UI（Generative UI）、语音/图片多模态、CI 级 AI 评估集、token 级精确计费。

## 2. 现状短板（本设计对应解决）

| 短板 | 对应设计 |
|---|---|
| patientId 由前端传入且未校验归属，可越权 | §4 安全修复 |
| SESSION_REGISTRY `size()==1` 兜底在并发下可能串用户 | §4 安全修复 |
| SSE 无事件类型，无 done/error 信号，靠连接关闭猜结束 | §5 事件协议 |
| RAG 已配 QuestionAnswerAdvisor 但知识库为空、患者端无入口 | §7 RAG |
| 250 行 system prompt 硬编码工具说明，与自动 tool schema 冗余 | §8 Prompt 重构 |
| 流中断无恢复，用户关页面丢对话 | §9 错误处理 |

## 3. 总体架构：单 Agent 渐进增强

保持现有 ChatClient 单 Agent 架构，不引入编排框架：

- RAG 封装为工具（Agentic RAG），LLM 自主决定何时检索
- SSE 升级为类型化 JSON 事件协议，一次改造支撑可视化/卡片/引用/错误处理
- 卡片数据由工具真实查询结果驱动（零幻觉），LLM 只负责文字
- 预约等确定性操作由卡片按钮直接走现有 REST API，不经过 LLM
- Prompt 内做轻量「场景引导」（导诊/问答/预约三种模式的行为规范），不拆真实多 Agent

## 4. 安全修复

**patientId 链路收紧**（消除越权）：

- Controller 不再信任 `AiConsultRequest.patientId`，改为从 Sa-Token 登录态（`StpUtil.getLoginId()`）解析当前用户对应的 patientId
- `AppointmentTools` 三个工具签名移除 `patientId` 参数，统一从 `PatientContextHolder` 上下文解析——LLM 从根上无法传入他人 ID
- system prompt 不再注入 patientId
- 历史会话查询/继续对话接口校验 sessionId 归属当前患者（`ai_consult_record.patientId` 比对）

**SESSION_REGISTRY 修复**：

- 删除 `resolveFromRegistry()` 中 `size()==1` 直接返回的捷径
- 注册表条目在流 complete/error/cancel 时清理，另加时间戳兜底清扫

## 5. SSE 类型化事件协议

端点不变（`POST /appointment/ai-consult/stream`），每条 SSE `data:` 载荷升级为 JSON 事件：

| type | 载荷 | 用途 |
|---|---|---|
| `text` | `content` 文本增量 | LLM 回复 |
| `tool_call` | `name`、`label`（中文描述）、`args` 摘要 | 前端显示「正在查询排班…」 |
| `tool_result` | `name`、`status` | 步骤标记完成 ✓ |
| `card` | `cardType` + `data` | 医生/排班/预约结果卡片 |
| `ref` | `sources[]`（title、snippet） | RAG 引用来源 |
| `done` | `sessionId` | 明确结束信号 |
| `error` | `message` | 明确错误信号 |

**后端实现**：

- 每次请求创建 Reactor `Sinks.Many<AiEvent>` 事件通道，以 sessionId 注册到 ConcurrentHashMap 注册表（复用 `PatientContextHolder` 已验证的跨线程解析机制）
- 用装饰器包装所有 ToolCallback：执行前推 `tool_call`，执行后推 `tool_result`；对可卡片化工具（医生/排班/预约类）额外推 `card` 事件，载荷为工具真实返回数据
- `KnowledgeTools` 检索后推 `ref` 事件
- 事件流与 ChatClient 文本流（映射为 `text` 事件）merge 后序列化输出；流终止时清理注册表

**前端实现**：

- assistant 消息数据模型从单段 markdown 升级为**块列表**：`text` / `toolStep` / `card` / `ref` 块，按事件类型追加渲染
- 卡片组件：医生卡片、排班卡片（号源余量 + 时间 + 预约按钮）、预约成功卡片、参考来源折叠块
- 会话持久化 JSON 存块结构，重开历史会话可复现卡片

## 6. 结构化卡片与预约闭环

- 排班卡片「预约」按钮 → 前端确认弹窗 → 直接调用现有预约 REST 接口 → 成功后在对话流插入预约成功卡片
- 预约结果**不回传 LLM**：用户后续若问「我约了什么」，LLM 会调 `getMyAppointments` 查库，天然保证最终一致，无需状态同步机制
- 取消预约同理可从「我的预约」卡片操作

## 7. RAG 知识问答（Agentic RAG）

**语料**（由 Claude 生成，存 `docs/knowledge/`，约 10 篇 Markdown）：

- 就医流程指南、各科室介绍、常见症状→科室对照指南（导诊核心语料）、挂号/退号规则、医保与收费说明、检查检验注意事项、就诊 FAQ

**入库**：

- 扩展 `AiRagServiceImpl` 支持 Markdown/文本入库（现仅 PDF），按标题分块，metadata 记录 title/source
- 管理端 `RagManager.vue` 增加「一键导入内置语料」按钮（演示友好）

**检索**：

- 新增 `KnowledgeTools.searchKnowledgeBase(query)` 工具：PgVector 相似检索 top-4，返回片段（含来源）给 LLM 组织回答，同时推 `ref` 事件
- **移除**全局 `QuestionAnswerAdvisor`（避免每轮强制检索浪费 token、干扰预约类对话）

## 8. 导诊场景与 Prompt 重构

**Prompt 外部化**：system prompt 从 `AIServiceImpl` 内 250 行字符串迁移到 `resources/prompts/system-prompt.st`（Spring AI `PromptTemplate`，日期等为模板变量），精简至约 60 行：

- 身份设定、当前日期
- 三场景行为规范：
  - **症状导诊**：引导描述症状 → `searchKnowledgeBase` 查症状-科室指南 → `listDepartments` 核对系统实际科室 → 推荐科室并说明理由 → 查医生排班
  - **知识问答**：检索知识库、基于引用回答
  - **预约管理**：操作前确认
- 安全红线：不做诊断结论、不给药物剂量、急症提示立即就医
- 回复风格
- 删除所有工具用法说明（Spring AI 自动生成 tool schema）

**工具分组**：`HospitalTools`（信息查询类）、`AppointmentTools`（预约操作类，签名去 patientId）、`KnowledgeTools`（RAG 检索，新增）

## 9. 错误处理

- 后端：流异常推 `error` 事件；`doOnComplete` / `doOnError` / **`doOnCancel`** 三处都保存会话记录（修复用户中途关页面丢对话）
- 前端：`error` 事件 → 失败气泡 + 重试按钮（重发上一条用户消息）；停止生成（AbortController）保留已收内容并落库

## 10. 测试策略

- 后端单测：工具方法逻辑（mock service 层）、越权请求被拒（他人 sessionId/预约）、事件 JSON 序列化
- AI 对话质量：固定演示脚本手动验证（不上 CI 评估集）
- 前端：Playwright 走黄金路径——症状咨询 → 科室推荐 → 排班卡片 → 一键预约成功；并验证历史会话卡片复现

## 11. 实施顺序

每步完成后系统保持可运行：

1. 安全修复（先做，避免工具签名返工）
2. SSE 事件协议改造（后端事件流 + 前端块渲染）
3. 工具调用可视化
4. 卡片消息 + 预约闭环
5. RAG：语料生成 → Markdown 入库 → 检索工具 → 引用展示
6. 导诊场景 prompt + Prompt 外部化重构
7. 错误处理完善 + 测试补齐

## 12. 涉及的关键文件

**后端（`HIS/`）**：

- `config/ai/AiChatConfig.java` — 移除 QuestionAnswerAdvisor、注册 KnowledgeTools、接入工具装饰器
- `config/ai/tools/HospitalTools.java`、`AppointmentTools.java` — 签名调整、卡片数据
- `config/ai/tools/KnowledgeTools.java`（新增）— RAG 检索工具
- `config/ai/tools/PatientContextHolder.java` — 删捷径、清理机制
- 事件模型与 Sink 注册表（新增，如 `config/ai/event/`）
- `service/entity/impl/AIServiceImpl.java` — 流合并、prompt 模板加载、doOnCancel 保存
- `service/entity/impl/AiRagServiceImpl.java` — Markdown 入库
- `controller/RegistrationController.java` — patientId 从登录态解析、sessionId 归属校验
- `resources/prompts/system-prompt.st`（新增）

**前端（`VUE/`）**：

- `src/views/ChatView.vue` — SSE JSON 事件解析、块列表渲染、重试/停止
- 卡片组件（新增，如 `src/components/chat/`）— 医生卡/排班卡/预约卡/引用块
- `src/views/admin/RagManager.vue` — 一键导入内置语料

**语料**：`docs/knowledge/*.md`（新增约 10 篇）
