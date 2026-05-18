package com.project.his.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.his.domain.dto.AiConsultRequest;
import com.project.his.domain.dto.ConsultSession;
import com.project.his.domain.po.AiConsultRecord;
import reactor.core.publisher.Flux;

/**
 * <p>
 * AI 问诊记录表 服务类
 * </p>
 *
 */
public interface IAIService extends IService<AiConsultRecord> {

    /**
     * 处理AI问诊请求，并以流式方式返回AI回复内容
     * @param request 问诊请求
     * @return AI回复流
     */
    Flux<String> processAiConsult(AiConsultRequest request);
    
    /**
     * 保存AI问诊对话记录到数据库（仅在会话结束时调用）
     * @param sessionId 会话ID
     * @return 是否保存成功
     */
    boolean saveConsultRecord(String sessionId);
    
    /**
     * 获取历史对话会话（从Redis获取）
     * @param sessionId 会话ID
     * @return 对话会话详情
     */
    ConsultSession getConsultSession(String sessionId);

    /**
     * 获取患者最近的问诊记录
     * @param patientId 患者ID
     * @return 问诊记录，如果不存在返回null
     */
    AiConsultRecord getLatestByPatientId(Long patientId);

    /**
     * 获取患者的所有会话列表
     * @param patientId 患者ID
     * @return 会话列表（按更新时间倒序）
     */
    java.util.List<AiConsultRecord> listSessionsByPatientId(Long patientId);

}
