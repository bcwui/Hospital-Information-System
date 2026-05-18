package com.graduation.his.service.entity;

import com.graduation.his.domain.dto.AiRagAddRequest;
import com.graduation.his.domain.dto.AiRagSearchRequest;
import com.graduation.his.domain.vo.AiRagSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG 服务接口
 */
public interface IAiRagService {
    /**
     * 文档入库
     */
    int addDocuments(AiRagAddRequest request);

    /**
     * PDF文档上传并入库
     * @param file PDF文件
     * @return 入库的文档片段数量
     */
    int uploadPdf(MultipartFile file);

    /**
     * 相似检索
     */
    List<AiRagSearchResult> search(AiRagSearchRequest request);
}
