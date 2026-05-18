package com.project.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRagSearchRequest {
    /**
     * 检索问题
     */
    private String query;

    /**
     * 检索条数（可选）
     */
    private Integer topK;

    /**
     * 相似度阈值（可选）
     */
    private Double similarityThreshold;

    /**
     * 过滤表达式（可选）
     */
    private String filterExpression;
}
