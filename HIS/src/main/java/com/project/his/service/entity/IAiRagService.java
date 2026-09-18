package com.project.his.service.entity;

import com.project.his.domain.dto.AiRagAddRequest;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.vo.AiRagSearchResult;
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
     * @return 入库的文档片段数量，-2 表示已上传过
     */
    int uploadPdf(MultipartFile file);

    /**
     * PDF文档上传并入库（支持强制重传）
     * @param file PDF文件
     * @param force 是否强制重新上传
     * @return 入库的文档片段数量，-2 表示已上传过
     */
    int uploadPdf(MultipartFile file, boolean force);

    /**
     * 从 classpath:knowledge/ 导入内置 Markdown 语料
     * @param force 是否强制重新导入（忽略已导入标记）
     * @return 入库的文档片段总数，-1 表示 VectorStore 未初始化
     */
    int importBuiltInKnowledge(boolean force);

    /**
     * 从目录批量导入 Markdown 文件
     * @param directoryPath 目录路径
     * @return 入库的文档片段总数
     */
    int importMarkdownFiles(String directoryPath);

    /**
     * 相似检索
     */
    List<AiRagSearchResult> search(AiRagSearchRequest request);
}
