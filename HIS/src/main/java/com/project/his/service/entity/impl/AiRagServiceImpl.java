package com.project.his.service.entity.impl;

import com.project.his.config.ai.AiProperties;
import com.project.his.domain.dto.AiRagAddRequest;
import com.project.his.domain.dto.AiRagDocument;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.vo.AiRagSearchResult;
import com.project.his.service.entity.IAiRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRagServiceImpl implements IAiRagService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final AiProperties aiProperties;

    @Override
    public int addDocuments(AiRagAddRequest request) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("VectorStore 未初始化，跳过文档入库");
            return 0;
        }
        if (request == null || CollectionUtils.isEmpty(request.getDocuments())) {
            return 0;
        }
        List<Document> documents = new ArrayList<>();
        for (AiRagDocument input : request.getDocuments()) {
            if (input == null || !StringUtils.hasText(input.getText())) {
                continue;
            }
            Map<String, Object> metadata = input.getMetadata() == null ? Collections.emptyMap() : input.getMetadata();
            documents.add(new Document(input.getText(), metadata));
        }
        if (documents.isEmpty()) {
            return 0;
        }
        try {
            vectorStore.add(documents);
            return documents.size();
        } catch (Exception e) {
            log.error("文档入库失败: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int uploadPdf(MultipartFile file) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("VectorStore 未初始化，跳过PDF入库");
            return 0;
        }
        if (file == null || file.isEmpty()) {
            log.warn("PDF文件为空");
            return 0;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            log.warn("不是PDF文件: {}", originalFilename);
            return 0;
        }

        try {
            // 使用Spring AI的PDF阅读器解析文档
            InputStreamResource resource = new InputStreamResource(file.getInputStream());
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<Document> documents = pdfReader.get();

            if (documents == null || documents.isEmpty()) {
                log.warn("PDF解析结果为空: {}", originalFilename);
                return 0;
            }

            // 为每个文档片段添加来源元数据
            List<Document> enrichedDocuments = new ArrayList<>(documents.size());
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                Map<String, Object> metadata = new HashMap<>();
                // 复制原有元数据，但过滤掉 null 值（VectorStore 不允许 null）
                if (doc.getMetadata() != null) {
                    for (Map.Entry<String, Object> entry : doc.getMetadata().entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            metadata.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                metadata.put("source", originalFilename);
                metadata.put("source_type", "pdf");
                metadata.put("page_index", i);
                enrichedDocuments.add(new Document(doc.getText(), metadata));
            }

            vectorStore.add(enrichedDocuments);
            log.info("PDF入库成功: {}, 共{}个文档片段", originalFilename, enrichedDocuments.size());
            return enrichedDocuments.size();

        } catch (IOException e) {
            log.error("PDF文件读取失败: {}", e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("PDF解析入库失败: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public List<AiRagSearchResult> search(AiRagSearchRequest request) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("VectorStore 未初始化，返回空结果");
            return List.of();
        }
        if (request == null || !StringUtils.hasText(request.getQuery())) {
            return List.of();
        }
        SearchRequest.Builder builder = SearchRequest.builder().query(request.getQuery());
        Integer topK = request.getTopK() != null ? request.getTopK() : aiProperties.getRag().getTopK();
        Double threshold = request.getSimilarityThreshold() != null
                ? request.getSimilarityThreshold()
                : aiProperties.getRag().getSimilarityThreshold();
        builder.topK(topK);
        builder.similarityThreshold(threshold);
        if (StringUtils.hasText(request.getFilterExpression())) {
            builder.filterExpression(request.getFilterExpression());
        } else if (StringUtils.hasText(aiProperties.getRag().getFilterExpression())) {
            builder.filterExpression(aiProperties.getRag().getFilterExpression());
        }

        List<Document> documents;
        try {
            documents = vectorStore.similaritySearch(builder.build());
        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage());
            return List.of();
        }
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<AiRagSearchResult> results = new ArrayList<>(documents.size());
        for (Document document : documents) {
            if (document == null) {
                continue;
            }
            results.add(AiRagSearchResult.builder()
                    .id(document.getId())
                    .text(document.getText())
                    .metadata(document.getMetadata())
                    .build());
        }
        return results;
    }
}
