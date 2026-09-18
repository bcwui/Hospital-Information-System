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
@TableName("bill")
public class Bill implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "bill_id", type = IdType.AUTO)
    private Long billId;

    private Long appointmentId;

    private Long patientId;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private Integer status;

    private String paymentMethod;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime updateTime;

}
