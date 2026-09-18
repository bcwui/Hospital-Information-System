package com.project.his.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("prescription")
public class Prescription implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "prescription_id", type = IdType.AUTO)
    private Long prescriptionId;

    private Long diagnosisId;

    private Long doctorId;

    private Long patientId;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
