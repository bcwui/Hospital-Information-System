package com.project.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDetailDTO {

    private Long drugId;

    private Integer quantity;

    private String dosage;

    private String usageMethod;

    private String frequency;

    private Integer days;

    private String remark;

}
