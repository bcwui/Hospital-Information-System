package com.graduation.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * RAG 检索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRagSearchResult {
    /**
     * 文档ID
     */
    private String id;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
