package com.project.his.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * AI 问诊记录表
 * </p>
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_consult_record")
public class AiConsultRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID（主键）
     */
    @TableId(value = "session_id", type = IdType.INPUT)
    private String sessionId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * AI 对话内容(JSON)
     */
    private String conversation;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
