package com.project.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.project.his.common.Result;
import com.project.his.domain.dto.AiRagAddRequest;
import com.project.his.domain.dto.AiRagSearchRequest;
import com.project.his.domain.vo.AiRagSearchResult;
import com.project.his.service.entity.IAiRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * RAG 管理接口
 */
@Slf4j
@RestController
@RequestMapping("/ai/rag")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class AiRagController {

    private final IAiRagService aiRagService;

    /**
     * 文档入库
     */
    @PostMapping("/documents")
    public Result<Integer> addDocuments(@RequestBody AiRagAddRequest request) {
        int count = aiRagService.addDocuments(request);
        return Result.success("文档入库完成", count);
    }

    /**
     * PDF文档上传并入库
     * @param force 是否强制重新上传（忽略已上传标记）
     */
    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Integer> uploadPdf(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "false") boolean force) {
        log.info("接收到PDF上传请求: {}", file.getOriginalFilename());
        int count = aiRagService.uploadPdf(file, force);
        if (count == -2) {
            return Result.error("文件「" + file.getOriginalFilename() + "」已上传过，请勿重复上传");
        }
        if (count > 0) {
            return Result.success("PDF入库成功，共" + count + "个文档片段", count);
        } else {
            return Result.error("PDF入库失败，请检查文件格式");
        }
    }

    /**
     * 一键导入内置 Markdown 语料
     * @param force 是否强制重新导入（忽略已导入标记）
     */
    @PostMapping("/import-built-in")
    public Result<Integer> importBuiltIn(@RequestParam(defaultValue = "false") boolean force) {
        try {
            int total = aiRagService.importBuiltInKnowledge(force);
            if (total == -1) {
                return Result.error("VectorStore 未初始化，请检查 PostgreSQL + pgvector 是否正常运行");
            }
            if (total == 0) {
                return Result.error("导入失败：0 个文档片段入库，请检查语料文件是否存在");
            }
            return Result.success(total);
        } catch (Exception e) {
            log.error("导入内置语料失败", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 相似检索
     */
    @PostMapping("/search")
    public Result<List<AiRagSearchResult>> search(@RequestBody AiRagSearchRequest request) {
        List<AiRagSearchResult> results = aiRagService.search(request);
        return Result.success("检索完成", results);
    }
}
