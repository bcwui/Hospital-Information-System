package com.project.his.service.entity.impl;

import com.project.his.common.Constants;
import com.project.his.config.ai.AiProperties;
import com.project.his.domain.dto.AiRagAddRequest;
import com.project.his.domain.dto.AiRagDocument;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.vo.AiRagSearchResult;
import com.project.his.service.entity.IAiRagService;
import com.project.his.utils.redis.IRedisService;
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
 *
 * 两阶段去重：
 * 第1层 — 整文件去重：Redis Set 记录已入库文件名，同文件直接跳过
 * 第2层 — Chunk 去重：向量相似度 ≥ 0.95 视为重复，跳过该分块不重复入库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRagServiceImpl implements IAiRagService {

    /** Chunk 去重相似度阈值：≥0.95 视为重复片段 */
    private static final double CHUNK_DEDUP_THRESHOLD = 0.95;

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final AiProperties aiProperties;
    private final IRedisService redisService;

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
        return addWithChunkDedup(vectorStore, documents);
    }

    // ==================== PDF 上传 ====================

    @Override
    public int uploadPdf(MultipartFile file) {
        return uploadPdf(file, false);
    }

    @Override
    public int uploadPdf(MultipartFile file, boolean force) {
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

        // 第1层：文件级去重
        if (!force && redisService.isSetMember(Constants.RedisKey.RAG_PDF_UPLOADED, originalFilename)) {
            log.info("PDF文件级去重命中: {}, 跳过入库", originalFilename);
            return -2;
        }

        try {
            InputStreamResource resource = new InputStreamResource(file.getInputStream());
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<Document> documents = pdfReader.get();

            if (documents == null || documents.isEmpty()) {
                log.warn("PDF解析结果为空: {}", originalFilename);
                return 0;
            }
            // PDF分块流水线：逐页清洗+分块
            List<Document> allChunks = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                String pageText = documents.get(i).getText();
                if (pageText == null || pageText.isBlank()) continue;
                List<Document> pageChunks = chunkPdfText(pageText, originalFilename, i);
                allChunks.addAll(pageChunks);
            }
            log.info("PDF分块完成: {}, 原始页数={}, 分块数={}", originalFilename, documents.size(), allChunks.size());

            // 第2层：Chunk 级去重入库
            int inserted = addWithChunkDedup(vectorStore, allChunks);
            redisService.addToSet(Constants.RedisKey.RAG_PDF_UPLOADED, originalFilename);
            log.info("PDF入库完成: {}, 总chunk={}, 实际入库={}, 重复跳过={}",
                    originalFilename, allChunks.size(), inserted, allChunks.size() - inserted);
            return inserted;
        } catch (IOException e) {
            log.error("PDF文件读取失败: {}", e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("PDF解析入库失败: {}", e.getMessage());
            return 0;
        }
    }

    // ==================== 内置语料导入 ====================

    @Override
    public int importBuiltInKnowledge(boolean force) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.error("VectorStore 未初始化，无法导入内置语料（请检查 PostgreSQL + pgvector 连接）");
            return -1;
        }

        // 第1层：文件级去重
        if (!force && redisService.isExists(Constants.RedisKey.RAG_IMPORTED)) {
            Integer cached = redisService.getValue(Constants.RedisKey.RAG_IMPORTED);
            if (cached != null) {
                log.info("内置语料文件级去重命中（已入库 {} 个chunk），跳过导入", cached);
                return cached;
            }
        }

        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:knowledge/*.md");
            if (resources.length == 0) {
                log.warn("classpath:knowledge/ 下未找到 .md 语料文件");
                return 0;
            }
            int totalInserted = 0;
            int totalSkipped = 0;
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                if (content == null || content.isBlank()) continue;
                List<Document> chunks = splitMarkdown(content, filename != null ? filename : "unknown.md");
                if (chunks.isEmpty()) continue;

                int inserted = addWithChunkDedup(vectorStore, chunks);
                int skipped = chunks.size() - inserted;
                totalInserted += inserted;
                totalSkipped += skipped;
                log.info("语料入库: {}, chunk={}, 入库={}, 去重跳过={}",
                        filename, chunks.size(), inserted, skipped);
            }
            log.info("内置语料全部完成: 总chunk={}, 入库={}, 去重跳过={}",
                    totalInserted + totalSkipped, totalInserted, totalSkipped);
            redisService.setValue(Constants.RedisKey.RAG_IMPORTED, totalInserted);
            return totalInserted;
        } catch (Exception e) {
            log.error("导入内置语料失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ==================== 旧方法兼容 ====================

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
                total += addWithChunkDedup(vectorStore, chunks);
            } catch (Exception e) {
                log.error("Markdown 入库失败: {}", file.getName(), e);
            }
        }
        return total;
    }

    // ==================== 检索 ====================

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
            if (document == null) continue;
            results.add(AiRagSearchResult.builder()
                    .id(document.getId())
                    .text(document.getText())
                    .metadata(document.getMetadata())
                    .build());
        }
        return results;
    }

    // ==================== 私有工具方法 ====================

    /** 分块公共参数 */
    private static final int MAX_CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;
    private static final int MIN_CHUNK_SIZE = 120;

    /** 句尾断点正则 */
    private static final java.util.regex.Pattern SENTENCE_END = java.util.regex.Pattern.compile("[。！？；]");

    // ==================== Markdown 分块流水线 ====================

    /**
     * Markdown 分块流水线：清洗 → 标题粗切 → 超长细分 → 短句合并 → 重叠窗口
     */
    private List<Document> splitMarkdown(String content, String fileName) {
        if (content == null || content.isBlank()) return List.of();

        // 1. 前置清洗
        String cleaned = cleanMarkdown(content);

        // 2. 按 #/##/### 标题粗切
        List<Document> chunks = splitByHeaders(cleaned, fileName);
        if (chunks.isEmpty()) {
            chunks.add(new Document(cleaned, baseMetadata(fileName, "markdown")));
        }

        // 3. 超长兜底 + 短句合并 + 重叠
        return postProcess(chunks);
    }

    /** 清洗 Markdown：去标记符号、剔除代码块、合并连续空行 */
    private String cleanMarkdown(String text) {
        // 剔除代码块（```...```）
        text = text.replaceAll("(?s)```.*?```", " ");
        // 去除行内代码 `...`
        text = text.replaceAll("`[^`]+`", "");
        // 去除常见 md 标记符号（保留文字）
        text = text.replaceAll("(?m)^[#]+\\s*", "");      // 标题 # 号
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1"); // 加粗 **text**
        text = text.replaceAll("\\*(.+?)\\*", "$1");       // 斜体 *text*
        text = text.replaceAll("~~(.+?)~~", "$1");         // 删除线
        text = text.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1"); // 链接 [text](url) → text
        text = text.replaceAll("!\\[[^]]*]\\([^)]+\\)", ""); // 图片 ![alt](url)
        text = text.replaceAll("(?m)^>\\s*", "");          // 引用 >
        text = text.replaceAll("(?m)^[-*+]\\s", "");       // 无序列表
        text = text.replaceAll("(?m)^\\d+\\.\\s", "");     // 有序列表
        // 合并连续空行
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }

    /** 按 #/##/### 多级标题分层切割 */
    private List<Document> splitByHeaders(String text, String fileName) {
        List<Document> chunks = new ArrayList<>();
        // 用正则匹配所有标题行（#/##/### 开头）
        String[] sections = text.split("(?=\n#{1,3}\\s)");
        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty()) continue;
            String title = extractTitle(trimmed);
            Map<String, Object> meta = baseMetadata(fileName, "markdown");
            meta.put("title", title);
            chunks.add(new Document(trimmed, meta));
        }
        return chunks;
    }

    // ==================== PDF 分块流水线 ====================

    /**
     * PDF 文本分块流水线：清洗 → 段落+序号粗切 → 超长细分 → 短句合并 → 重叠窗口
     */
    private List<Document> chunkPdfText(String pageText, String fileName, int pageIndex) {
        if (pageText == null || pageText.isBlank()) return List.of();

        // 1. 前置清洗
        String cleaned = cleanPdfText(pageText);

        // 2. 按双换行自然段切分 + 序号模式分层
        List<Document> chunks = splitByParagraphs(cleaned, fileName);

        // 3. 超长兜底 + 短句合并 + 重叠
        List<Document> processed = postProcess(chunks);

        // 补充页码信息
        for (Document d : processed) {
            d.getMetadata().put("page_index", pageIndex);
        }
        return processed;
    }

    /** 清洗 PDF 文本：去页眉页码模式、合并空行、基础表格转文本 */
    private String cleanPdfText(String text) {
        // 去纯数字页码行（独立成行的数字）
        text = text.replaceAll("(?m)^\\s*\\d{1,4}\\s*$", "");
        // 去 "第X页" 模式
        text = text.replaceAll("第\\s*\\d+\\s*页", "");
        // 去页眉常见模式 "XXX  |  第X页"
        text = text.replaceAll("(?m)^.{2,30}\\s*\\|\\s*\\d{1,4}\\s*$", "");
        // 合并连续空行
        text = text.replaceAll("\n{3,}", "\n\n");
        // 表格基础处理：多空格列 → 逗号分隔
        text = text.replaceAll("(?m)\\s{4,}", "，");
        return text.trim();
    }

    /** 按双换行自然段切分，同时匹配序号层级 */
    private List<Document> splitByParagraphs(String text, String fileName) {
        List<Document> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        // 序号模式：1. / (1) / 一、 / 第X章
        java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile(
                "^(?:\\d+[.)]\\s*|\\(\\d+\\)\\s*|[一二三四五六七八九十]+[、，]\\s*|第[一二三四五六七八九十]+[章节]\\s*)");

        for (String para : paragraphs) {
            String trimmed = para.replace("\n", "").trim();
            if (trimmed.isEmpty()) continue;

            Map<String, Object> meta = baseMetadata(fileName, "pdf");
            // 提取序号作为标题标识
            java.util.regex.Matcher m = numberPattern.matcher(trimmed);
            if (m.find()) {
                meta.put("title", m.group().trim());
            }
            chunks.add(new Document(trimmed, meta));
        }
        return chunks;
    }

    // ==================== 公共后处理 ====================

    /**
     * 后处理流水线：超长细分 → 短句合并 → 添加重叠窗口
     */
    private List<Document> postProcess(List<Document> chunks) {
        // 3a. 超长兜底：＞500字按句尾标点滑动细分
        List<Document> sized = new ArrayList<>();
        for (Document doc : chunks) {
            if (doc.getText().length() > MAX_CHUNK_SIZE) {
                sized.addAll(splitOversize(doc));
            } else {
                sized.add(doc);
            }
        }

        // 3b. 短句合并：＜120字拼下一段
        List<Document> merged = mergeShortChunks(sized);

        // 3c. 添加重叠窗口
        return addOverlap(merged);
    }

    /** 超长段按句尾标点滑动细分，带重叠窗口 */
    private List<Document> splitOversize(Document doc) {
        List<Document> result = new ArrayList<>();
        String text = doc.getText();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                // 向前找最近的句尾标点
                int cut = end;
                while (cut > start + MAX_CHUNK_SIZE / 2) {
                    if (SENTENCE_END.matcher(text.substring(cut - 1, cut)).find()) {
                        end = cut;
                        break;
                    }
                    cut--;
                }
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                result.add(new Document(chunk, new HashMap<>(doc.getMetadata())));
            }
            // 滑动窗口：下一段从 end - OVERLAP 开始
            start = end - OVERLAP;
            if (start >= text.length()) break;
            if (start < 0) start = 0;
            // 防止在已经覆盖全文后无限循环
            if (start + MAX_CHUNK_SIZE >= text.length()) {
                String remaining = text.substring(start).trim();
                if (!remaining.isEmpty()) {
                    result.add(new Document(remaining, new HashMap<>(doc.getMetadata())));
                }
                break;
            }
        }
        return result;
    }

    /** 短句合并：＜120字自动拼接下一段 */
    private List<Document> mergeShortChunks(List<Document> chunks) {
        if (chunks.size() <= 1) return chunks;
        List<Document> result = new ArrayList<>();
        Document pending = null;
        for (Document current : chunks) {
            if (pending == null) {
                pending = current;
            } else if (pending.getText().length() < MIN_CHUNK_SIZE) {
                // 拼接到 pending
                String mergedText = pending.getText() + "\n" + current.getText();
                Map<String, Object> mergedMeta = new HashMap<>(pending.getMetadata());
                // 若当前有更具体的标题则更新
                if (current.getMetadata().get("title") != null
                        && !current.getMetadata().get("title").toString().isEmpty()) {
                    mergedMeta.put("title", current.getMetadata().get("title"));
                }
                pending = new Document(mergedText, mergedMeta);
            } else {
                result.add(pending);
                pending = current;
            }
        }
        if (pending != null) result.add(pending);
        return result;
    }

    /** 为每个 chunk 添加重叠窗口（前一个 chunk 的尾部 100 字作为上下文前缀） */
    private List<Document> addOverlap(List<Document> chunks) {
        if (chunks.size() <= 1) return chunks;
        List<Document> result = new ArrayList<>();
        result.add(chunks.get(0)); // 第一个不用加
        for (int i = 1; i < chunks.size(); i++) {
            Document prev = chunks.get(i - 1);
            Document curr = chunks.get(i);
            String prevTail = prev.getText();
            if (prevTail.length() > OVERLAP) {
                prevTail = prevTail.substring(prevTail.length() - OVERLAP);
            }
            // 仅在当前 chunk 不以重叠内容开头时才添加（避免重复拼接相同前缀）
            String currText = curr.getText();
            if (currText.length() > prevTail.length() && !currText.startsWith(prevTail)) {
                currText = prevTail + currText;
            }
            result.add(new Document(currText, curr.getMetadata()));
        }
        return result;
    }

    // ==================== 通用工具 ====================

    private Map<String, Object> baseMetadata(String fileName, String sourceType) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("source", fileName);
        meta.put("source_type", sourceType);
        return meta;
    }

    /** 从文本首行提取标题 */
    private String extractTitle(String text) {
        int newlineIdx = text.indexOf('\n');
        return (newlineIdx > 0) ? text.substring(0, newlineIdx).trim() : text.substring(0, Math.min(80, text.length())).trim();
    }


    /**
     * Chunk 级去重入库：对每个分块做向量相似度检索（阈值 0.95），严格重复才跳过。
     *
     * @return 实际入库的文档数
     */
    private int addWithChunkDedup(VectorStore vectorStore, List<Document> documents) {
        if (documents == null || documents.isEmpty()) return 0;

        List<Document> toInsert = new ArrayList<>();
        int skipped = 0;

        for (Document doc : documents) {
            if (doc.getText() == null || doc.getText().isBlank()) {
                skipped++;
                continue;
            }
            try {
                SearchRequest sr = SearchRequest.builder()
                        .query(doc.getText())
                        .topK(1)
                        .similarityThreshold(CHUNK_DEDUP_THRESHOLD)
                        .build();
                List<Document> existing = vectorStore.similaritySearch(sr);
                if (existing != null && !existing.isEmpty()) {
                    skipped++;
                    continue;
                }
            } catch (Exception e) {
                // 检索失败不阻塞入库，保守处理：允许插入
                log.warn("Chunk去重检索异常，放行入库: {}", e.getMessage());
            }
            toInsert.add(doc);
        }

        if (!toInsert.isEmpty()) {
            try {
                vectorStore.add(toInsert);
            } catch (Exception e) {
                log.error("Chunk入库失败: {}", e.getMessage());
                return 0;
            }
        }
        return toInsert.size();
    }
}
