package com.project.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * RAG 文档输入
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRagDocument {
    /**
     * 文本内容
     */
    private String text;

    /**
     * 元数据（可选）
     */
    private Map<String, Object> metadata;
}
