package com.project.his.config.ai.tools;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 患者上下文持有者 — 使用阿里 TransmittableThreadLocal。
 * <p>
 * TTL 在线程池（包括 Reactor boundedElastic）场景下自动传递上下文，
 * 配合 {@code Schedulers.setExecutorServiceDecorator} 实现。
 * ConcurrentHashMap 作为兜底。
 */
public class PatientContextHolder {

    private static final TransmittableThreadLocal<Long> PATIENT_ID = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<String> SESSION_ID = new TransmittableThreadLocal<>();

    /** 跨线程共享的 sessionId → patientId 映射（兜底） */
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
     * 跨线程解析 patientId。
     * 优先通过 TTL 传递的 sessionId 查 Registry，TTL 已在线程切换时自动复制。
     */
    public static Long resolveFromRegistry() {
        String sessionId = SESSION_ID.get();
        if (sessionId != null) {
            Long patientId = SESSION_REGISTRY.get(sessionId);
            if (patientId != null) {
                return patientId;
            }
        }
        return null;
    }

    public static void removeSession(String sessionId) {
        if (sessionId != null) {
            SESSION_REGISTRY.remove(sessionId);
        }
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
