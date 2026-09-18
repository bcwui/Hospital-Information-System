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
@TableName("prescription_detail")
public class PrescriptionDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "detail_id", type = IdType.AUTO)
    private Long detailId;

    private Long prescriptionId;

    private Long drugId;

    private java.math.BigDecimal unitPrice;

    private Integer quantity;

    private String dosage;

    private String usageMethod;

    private String frequency;

    private Integer days;

    private String remark;

    private LocalDateTime createTime;

}
