package com.project.his.config.ai.tools;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 患者上下文持有者 — 三层保障机制。
 * <p>
 * 1. ThreadLocal：同线程直接读取
 * 2. ConcurrentHashMap：跨线程解析（不需要知道 sessionId）
 * <p>
 * 在 AIServiceImpl 中，每次 AI 调用前设置（写入 ThreadLocal + Map），调用后清除。
 */
public class PatientContextHolder {

    private static final ThreadLocal<Long> PATIENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();

    /** 跨线程共享的 sessionId → patientId 映射 */
    private static final ConcurrentMap<String, Long> SESSION_REGISTRY = new ConcurrentHashMap<>();

    public static void set(Long patientId, String sessionId) {
        PATIENT_ID.set(patientId);
        SESSION_ID.set(sessionId);
        if (sessionId != null && patientId != null) {
            SESSION_REGISTRY.put(sessionId, patientId);
        }
    }

    public static Long getPatientId() {
        return PATIENT_ID.get();
    }

    public static String getSessionId() {
        return SESSION_ID.get();
    }

    /**
     * 跨线程解析 patientId——不需要事先知道 sessionId。
     * <p>
     * 策略：
     * 1. ThreadLocal sessionId → 查 Registry（同线程最优）
     * 2. Registry 仅有一个条目 → 直接返回（单用户场景）
     * 3. 多个条目且无法区分 → 返回 null
     */
    public static Long resolveFromRegistry() {
        // 同线程优先
        String sessionId = SESSION_ID.get();
        if (sessionId != null) {
            Long patientId = SESSION_REGISTRY.get(sessionId);
            if (patientId != null) {
                return patientId;
            }
        }

        // 跨线程兜底：只有一个活跃会话时可直接确定
        if (SESSION_REGISTRY.size() == 1) {
            return SESSION_REGISTRY.values().iterator().next();
        }

        return null;
    }

    public static void clear() {
        String sessionId = SESSION_ID.get();
        PATIENT_ID.remove();
        SESSION_ID.remove();
        if (sessionId != null) {
            SESSION_REGISTRY.remove(sessionId);
        }
    }

    private PatientContextHolder() {}
}
