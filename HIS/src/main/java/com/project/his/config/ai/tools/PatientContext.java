package com.project.his.config.ai.tools;

/**
 * 患者上下文 — 基于 Reactor Context，彻底消除 ThreadLocal。
 * 双重保障：优先通过 sessionId 从 Redis 反查；若不可用则直接从 Context 读取。
 */
public class PatientContext {

    /** Reactor Context 中存放 AI 会话 ID 的 key */
    public static final String SESSION_ID_KEY = "AI_CONSULT_SESSION_ID";

    /** Reactor Context 中直接存放 patientId 的 key（兜底） */
    public static final String PATIENT_ID_KEY = "AI_CONSULT_PATIENT_ID";

    private PatientContext() {}
}
