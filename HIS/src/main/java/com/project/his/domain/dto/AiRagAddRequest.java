package com.project.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 文档入库请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRagAddRequest {
    /**
     * 待入库文档列表
     */
    private List<AiRagDocument> documents;
}
