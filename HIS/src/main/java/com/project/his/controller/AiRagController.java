package com.graduation.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.graduation.his.common.Result;
import com.graduation.his.domain.dto.AiRagAddRequest;
import com.graduation.his.domain.dto.AiRagSearchRequest;
import com.graduation.his.domain.vo.AiRagSearchResult;
import com.graduation.his.service.entity.IAiRagService;
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
     */
    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Integer> uploadPdf(@RequestParam("file") MultipartFile file) {
        log.info("接收到PDF上传请求: {}", file.getOriginalFilename());
        int count = aiRagService.uploadPdf(file);
        if (count > 0) {
            return Result.success("PDF入库成功，共" + count + "个文档片段", count);
        } else {
            return Result.error("PDF入库失败，请检查文件格式");
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
