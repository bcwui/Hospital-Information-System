package com.project.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugDTO {

    private Long drugId;

    private String drugName;

    private String specification;

    private String manufacturer;

    private String unit;

    private BigDecimal unitPrice;

    private Integer stockQuantity;

    private String description;

    private Integer isActive;

}
