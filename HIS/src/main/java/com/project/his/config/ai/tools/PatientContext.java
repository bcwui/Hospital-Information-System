package com.project.his.config.ai.tools;

/**
 * 患者上下文，用于在AI Tool中获取当前会话的患者信息
 * 使用ThreadLocal保证线程安全
 */
public class PatientContext {

    private static final ThreadLocal<Long> PATIENT_ID = new ThreadLocal<>();

    /**
     * 设置当前会话的患者ID
     */
    public static void setPatientId(Long patientId) {
        PATIENT_ID.set(patientId);
    }

    /**
     * 获取当前会话的患者ID
     */
    public static Long getPatientId() {
        return PATIENT_ID.get();
    }

    /**
     * 清除上下文（在请求结束时调用）
     */
    public static void clear() {
        PATIENT_ID.remove();
    }
}
