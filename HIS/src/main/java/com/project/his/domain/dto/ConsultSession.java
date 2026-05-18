package com.graduation.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 问诊会话DTO，用于管理整个对话过程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultSession {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 完整对话历史记录
     */
    @Builder.Default
    private List<MessageRecord> messageHistory = new ArrayList<>();

    /**
     * 版本号，用于乐观锁
     */
    private Integer version;
} 