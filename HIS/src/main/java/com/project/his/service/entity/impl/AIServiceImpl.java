package com.project.his.service.entity.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.his.common.Constants;
import com.project.his.config.ai.tools.PatientContext;
import com.project.his.domain.dto.*;
import com.project.his.domain.po.AiConsultRecord;
import com.project.his.mapper.AiConsultRecordMapper;
import com.project.his.service.entity.IAIService;
import com.project.his.utils.redis.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * AI 问诊记录表 服务实现类
 * </p>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl extends ServiceImpl<AiConsultRecordMapper, AiConsultRecord> implements IAIService {

    private final IRedisService redisService;

    @Lazy
    private final ChatClient chatClient;

    private final ChatMemoryRepository chatMemoryRepository;

    private static final String META_CREATE_TIME = "createTime";

    private static final DateTimeFormatter META_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    
    /**
     * 获取Redis中的会话Map（带分布式锁）
     */
    private RMap<String, Object> getSessionMap(String sessionId) {
        String redisKey = Constants.RedisKey.AI_CONSULT_SESSION + sessionId;
        RMap<String, Object> sessionMap = redisService.getMap(redisKey);
        
        // 重置过期时间
        sessionMap.expire(Duration.ofHours(Constants.AIConstants.SESSION_EXPIRE_HOURS));
        
        return sessionMap;
    }
    
    /**
     * 获取分布式锁
     */
    private RLock getSessionLock(String sessionId) {
        String lockKey = Constants.RedisKey.AI_CONSULT_LOCK + sessionId;
        return redisService.getLock(lockKey);
    }
    
    /**
     * 将会话保存到Redis（带分布式锁）
     */
    private void saveSessionToRedis(ConsultSession session) {
        if (session == null || session.getSessionId() == null) {
            return;
        }

        RLock lock = getSessionLock(session.getSessionId());
        try {
            // 尝试获取锁，等待5秒，10秒后自动释放
            if (lock.tryLock(Constants.AIConstants.LOCK_WAIT_TIME, Constants.AIConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    RMap<String, Object> sessionMap = getSessionMap(session.getSessionId());
                    sessionMap.put("sessionId", session.getSessionId());
                    sessionMap.put("patientId", session.getPatientId());
                    Integer version = session.getVersion() == null ? 0 : session.getVersion();
                    sessionMap.put("version", version + 1); // 增加版本号
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取会话锁失败: {}", session.getSessionId());
            }
        } catch (InterruptedException e) {
            log.error("保存会话到Redis时被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 从Redis获取会话（带分布式锁）
     */
    private ConsultSession getSessionFromRedis(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        RLock lock = getSessionLock(sessionId);
        try {
            if (lock.tryLock(Constants.AIConstants.LOCK_WAIT_TIME, Constants.AIConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    RMap<String, Object> sessionMap = getSessionMap(sessionId);
                    if (sessionMap.isEmpty()) {
                        return null;
                    }

                    return ConsultSession.builder()
                            .sessionId(sessionId)
                            .patientId((Long) sessionMap.get("patientId"))
                            .messageHistory(new ArrayList<>())
                            .version((Integer) sessionMap.getOrDefault("version", 0))
                            .build();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取会话锁失败: {}", sessionId);
                return null;
            }
        } catch (InterruptedException e) {
            log.error("从Redis获取会话时被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    @Deprecated
    public SseEmitter createSseConnection(AiConsultConnectionRequest request) {
        throw new UnsupportedOperationException("SSE连接模式已废弃，请使用 /appointment/ai-consult/stream");
    }
    
    @Override
    public Flux<String> processAiConsult(AiConsultRequest request) {
        String sessionId = request.getSessionId();
        Long patientId = request.getPatientId();
        String question = request.getQuestion();

        // 验证必要参数
        if (patientId == null) {
            throw new IllegalArgumentException("患者ID不能为空");
        }
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("问题内容不能为空");
        }

        // 获取或创建会话
        ConsultSession session;
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            session = ConsultSession.builder()
                    .sessionId(sessionId)
                    .patientId(patientId)
                    .version(0) // 初始化版本号
                    .messageHistory(new ArrayList<>())
                    .build();
        } else {
            session = getSessionFromRedis(sessionId);
            if (session == null) {
                session = getConsultSession(sessionId);
                if (session == null) {
                    throw new IllegalArgumentException("无效的会话ID");
                }
            }

            // 验证会话关联的患者ID是否匹配
            if (!patientId.equals(session.getPatientId())) {
                throw new IllegalArgumentException("会话关联的患者ID与请求不匹配");
            }
        }

        if (session.getVersion() == null) {
            session.setVersion(0);
        }

        saveSessionToRedis(session);
        request.setSessionId(sessionId);

        final String finalSessionId = sessionId;
        final LocalDateTime userTime = LocalDateTime.now();

        // 双重保障注入 Reactor Context：
        // 1. sessionId → Tool 通过 Redis 反查 patientId
        // 2. patientId → Redis 不可用时直接兜底
        return chatClient.prompt()
                .system(createSystemPrompt(patientId))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .user(question)
                .stream()
                .content()
                .contextWrite(ctx -> ctx
                        .put(PatientContext.SESSION_ID_KEY, finalSessionId)
                        .put(PatientContext.PATIENT_ID_KEY, patientId))
                .doOnError(error -> {
                    log.error("处理AI问诊请求异常: {}", error.getMessage());
                    updateMemoryCreateTimes(finalSessionId, userTime, null);
                })
                .doOnComplete(() -> {
                    updateMemoryCreateTimes(finalSessionId, userTime, LocalDateTime.now());
                    saveConsultRecord(finalSessionId);
                })
                .doOnCancel(() -> log.info("AI问诊流被取消, sessionId: {}", finalSessionId));
    }
    
    /**
     * 构建系统提示词（包含当前患者ID，确保AI在Tool调用时能传递正确的患者ID）
     */
    private String createSystemPrompt(Long patientId) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        String currentDateIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        String basePrompt = """
                # AI 医疗助手

                ## 当前时间
                今天是 %s（%s）。查询排班或预约时，请基于这个日期。

                ## 当前患者
                当前对话的患者的唯一标识ID为：%d。
                调用预约操作类工具（getMyAppointments / createAppointment / cancelAppointment）时，
                系统会自动识别患者身份，你无需手动传递患者ID。

                ## 身份
                您是一位专业、友善的AI医疗助手，服务于本院患者。您可以进行健康咨询、症状分析，并帮助患者查询医院信息、预约挂号。

                ## 工具能力
                您拥有以下工具来帮助患者：

                **信息查询类**
                - listDepartments: 获取医院科室列表
                - listClinicsByDepartment: 按科室查门诊
                - searchClinics: 搜索门诊
                - listDoctorsByClinic: 按门诊查医生
                - searchDoctors: 搜索医生
                - getDoctorSchedules: 查医生排班
                - getAvailableSchedules: 查可预约排班

                **预约操作类（自动识别患者身份）**
                - getMyAppointments: 查患者预约，可按状态筛选
                - createAppointment: 创建预约，只需排班ID
                - cancelAppointment: 取消预约，只需预约ID

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
                """.formatted(currentDate, currentDateIso, patientId, currentDateIso);

        return basePrompt;
    }

    /**
     * 构建系统提示词（通用版，不含患者ID，用于历史记录存储场景）
     * 实际对话中会使用 {@link #createSystemPrompt(Long)} 包含正确的患者ID
     */
    private String createSystemPrompt() {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        String currentDateIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        return """
                # AI 医疗助手（历史记录）

                ## 当前时间
                会话发生于 %s（%s）。

                ## 身份
                您是一位专业、友善的AI医疗助手，服务于本院患者。您可以进行健康咨询、症状分析，并帮助患者查询医院信息、预约挂号。

                ## 安全提醒
                - 不提供具体药物剂量
                - 严重症状建议立即就医
                - 诊断结果仅供参考，以线下医生诊断为准
                """.formatted(currentDate, currentDateIso);
    }

    /**
     * 从记忆中构建消息历史（保证结构与旧接口一致）
     */
    private List<MessageRecord> buildMessageHistory(String sessionId) {
        List<MessageRecord> history = new ArrayList<>();
        history.add(MessageRecord.builder()
                .role("system")
                .content(createSystemPrompt())
                .createTime(LocalDateTime.now())
                .build());

        List<Message> messages = chatMemoryRepository.findByConversationId(sessionId);
        if (messages == null || messages.isEmpty()) {
            return history;
        }

        for (Message message : messages) {
            if (message == null || message.getMessageType() == MessageType.SYSTEM) {
                continue;
            }
            history.add(MessageRecord.builder()
                    .role(resolveRole(message.getMessageType()))
                    .content(message.getText() == null ? "" : message.getText())
                    .createTime(resolveCreateTime(message))
                    .build());
        }
        return history;
    }

    private String resolveRole(MessageType type) {
        if (type == MessageType.ASSISTANT) {
            return "assistant";
        }
        if (type == MessageType.USER) {
            return "user";
        }
        return "system";
    }

    private LocalDateTime resolveCreateTime(Message message) {
        if (message == null || message.getMetadata() == null) {
            return LocalDateTime.now();
        }
        Object value = message.getMetadata().get(META_CREATE_TIME);
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof String text) {
            try {
                return LocalDateTime.parse(text, META_TIME_FORMATTER);
            } catch (Exception ignored) {
                return LocalDateTime.now();
            }
        }
        if (value instanceof Long epochMillis) {
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), DEFAULT_ZONE);
        }
        return LocalDateTime.now();
    }

    private void updateMemoryCreateTimes(String sessionId, LocalDateTime userTime, LocalDateTime assistantTime) {
        List<Message> messages = chatMemoryRepository.findByConversationId(sessionId);
        if (messages == null || messages.isEmpty()) {
            return;
        }
        int lastUserIndex = findLastIndex(messages, MessageType.USER);
        int lastAssistantIndex = findLastIndex(messages, MessageType.ASSISTANT);
        List<Message> updated = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message == null) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            if (message.getMetadata() != null && !message.getMetadata().isEmpty()) {
                metadata.putAll(message.getMetadata());
            }
            if (!metadata.containsKey(META_CREATE_TIME)) {
                LocalDateTime time = LocalDateTime.now();
                if (i == lastUserIndex && userTime != null) {
                    time = userTime;
                } else if (i == lastAssistantIndex && assistantTime != null) {
                    time = assistantTime;
                }
                metadata.put(META_CREATE_TIME, time.format(META_TIME_FORMATTER));
            }
            updated.add(copyMessage(message, metadata));
        }
        if (!updated.isEmpty()) {
            chatMemoryRepository.saveAll(sessionId, updated);
        }
    }

    private int findLastIndex(List<Message> messages, MessageType targetType) {
        if (messages == null || targetType == null) {
            return -1;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message != null && targetType == message.getMessageType()) {
                return i;
            }
        }
        return -1;
    }

    private Message copyMessage(Message message, Map<String, Object> metadata) {
        Objects.requireNonNull(message, "message不能为空");
        String content = message.getText() == null ? "" : message.getText();
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        MessageType type = message.getMessageType();
        if (type == null) {
            return UserMessage.builder().text(content).metadata(safeMetadata).build();
        }
        if (type == MessageType.SYSTEM) {
            return SystemMessage.builder().text(content).metadata(safeMetadata).build();
        }
        if (type == MessageType.ASSISTANT) {
            return new AssistantMessage(content, safeMetadata);
        }
        if (type == MessageType.TOOL) {
            return new AssistantMessage(content, safeMetadata);
        }
        return UserMessage.builder().text(content).metadata(safeMetadata).build();
    }
    
    @Deprecated
    private void sendMessageEvent(String sessionId, String role, String content) {
        // 旧协议已废弃
    }
    
    @Deprecated
    private void sendCompleteEvent(String sessionId) {
        // 旧协议已废弃
    }
    
    @Deprecated
    private void sendErrorEvent(String sessionId, String errorMessage) {
        // 旧协议已废弃
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConsultRecord(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        try {
            // 从Redis获取会话
            ConsultSession session = getSessionFromRedis(sessionId);
            if (session == null) {
                log.error("保存会话记录失败: 会话不存在, sessionId: {}", sessionId);
                return false;
            }

            session.setMessageHistory(buildMessageHistory(sessionId));

            // 将对话历史转换为JSON字符串
            String conversationJson = JSON.toJSONString(session.getMessageHistory());

            // 查询现有记录
            AiConsultRecord record = getById(sessionId);

            boolean saved;
            if (record == null) {
                // 创建新记录
                record = new AiConsultRecord();
                record.setSessionId(sessionId);
                record.setPatientId(session.getPatientId());
                record.setConversation(conversationJson);
                record.setCreateTime(LocalDateTime.now());
                saved = save(record);
            } else {
                // 更新现有记录
                record.setConversation(conversationJson);
                record.setUpdateTime(LocalDateTime.now());
                saved = updateById(record);
            }

            if (!saved) {
                log.error("保存会话记录失败: 数据库操作失败, sessionId: {}", sessionId);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("保存对话记录异常: {}", e.getMessage());
            throw new RuntimeException("保存对话记录失败", e);
        }
    }

    @Override
    public AiConsultRecord getLatestByPatientId(Long patientId) {
        if (patientId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(AiConsultRecord::getPatientId, patientId)
                .orderByDesc(AiConsultRecord::getUpdateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public ConsultSession getConsultSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        try {
            // 1. 先从 Redis 获取
            ConsultSession session = getSessionFromRedis(sessionId);
            if (session != null) {
                session.setMessageHistory(buildMessageHistory(sessionId));
                return session;
            }

            // 2. Redis 没有，从数据库获取
            AiConsultRecord record = getById(sessionId);
            if (record == null) {
                return null;
            }

            // 3. 构建 ConsultSession
            session = ConsultSession.builder()
                    .sessionId(record.getSessionId())
                    .patientId(record.getPatientId())
                    .version(0)
                    .messageHistory(new ArrayList<>())
                    .build();

            // 4. 解析历史消息（如果有）
            if (record.getConversation() != null && !record.getConversation().isEmpty()) {
                try {
                    List<MessageRecord> history = JSON.parseArray(record.getConversation(), MessageRecord.class);
                    if (history != null) {
                        session.setMessageHistory(history);
                    }
                } catch (Exception e) {
                    log.warn("解析会话历史失败: {}", e.getMessage());
                }
            }

            // 5. 重新缓存到 Redis
            saveSessionToRedis(session);

            log.info("从数据库恢复会话: sessionId={}", sessionId);
            return session;
        } catch (Exception e) {
            log.error("获取对话会话异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<AiConsultRecord> listSessionsByPatientId(Long patientId) {
        if (patientId == null) {
            return List.of();
        }
        return lambdaQuery()
                .eq(AiConsultRecord::getPatientId, patientId)
                .orderByDesc(AiConsultRecord::getUpdateTime)
                .list();
    }
}
