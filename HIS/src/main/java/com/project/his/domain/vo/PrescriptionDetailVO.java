package com.project.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDetailVO {

    private Long detailId;

    private Long drugId;

    private String drugName;

    private String specification;

    private String unit;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    private String dosage;

    private String usageMethod;

    private String frequency;

    private Integer days;

    private String remark;

}
