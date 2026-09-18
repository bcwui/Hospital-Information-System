package com.project.his.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("drug")
public class Drug implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "drug_id", type = IdType.AUTO)
    private Long drugId;

    private String drugName;

    private String specification;

    private String manufacturer;

    private String unit;

    private BigDecimal unitPrice;

    private Integer stockQuantity;

    private String description;

    private Integer isActive;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
