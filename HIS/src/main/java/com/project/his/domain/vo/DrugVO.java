package com.project.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugVO {

    private Long drugId;

    private String drugName;

    private String specification;

    private String manufacturer;

    private String unit;

    private BigDecimal unitPrice;

    private Integer stockQuantity;

    private String description;

    private Integer isActive;

    private String stockStatus;

    private LocalDateTime createTime;

}
