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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    public int importBuiltInKnowledge() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.error("VectorStore 未初始化，无法导入内置语料（请检查 PostgreSQL + pgvector 连接）");
            return -1;
        }
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:knowledge/*.md");
            if (resources.length == 0) {
                log.warn("classpath:knowledge/ 下未找到 .md 语料文件");
                return 0;
            }
            int total = 0;
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                if (content == null || content.isBlank()) continue;
                List<Document> chunks = splitMarkdown(content, filename != null ? filename : "unknown.md");
                if (chunks.isEmpty()) continue;
                vectorStore.add(chunks);
                total += chunks.size();
                log.info("语料入库成功: {}, {} 个分块", filename, chunks.size());
            }
            log.info("内置语料全部入库完成，共 {} 个分块", total);
            return total;
        } catch (Exception e) {
            log.error("导入内置语料失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /* 保留旧方法兼容（如果有其他地方调用） */
    @Override
    public int importMarkdownFiles(String directoryPath) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("VectorStore 未初始化，跳过 Markdown 入库");
            return 0;
        }
        java.io.File dir = new java.io.File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("知识库目录不存在: {}", directoryPath);
            return 0;
        }
        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".md"));
        if (files == null || files.length == 0) {
            log.warn("知识库目录下没有 .md 文件: {}", directoryPath);
            return 0;
        }
        int total = 0;
        for (java.io.File file : files) {
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                if (content == null || content.isBlank()) continue;
                List<Document> chunks = splitMarkdown(content, file.getName());
                if (chunks.isEmpty()) continue;
                vectorStore.add(chunks);
                total += chunks.size();
                log.info("Markdown 入库成功: {}, {} 个分块", file.getName(), chunks.size());
            } catch (Exception e) {
                log.error("Markdown 入库失败: {}", file.getName(), e);
            }
        }
        return total;
    }

    private List<Document> splitMarkdown(String content, String fileName) {
        List<Document> chunks = new ArrayList<>();
        String title = fileName.replace(".md", "");
        String[] parts = content.split("\n## ");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", fileName);
            metadata.put("source_type", "markdown");
            int newlineIdx = part.indexOf('\n');
            if (newlineIdx > 0) {
                metadata.put("title", (i == 0 ? title : part.substring(0, newlineIdx).trim()));
            } else {
                metadata.put("title", title);
            }
            chunks.add(new Document(part, metadata));
        }
        return chunks;
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
