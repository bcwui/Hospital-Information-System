package com.project.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionVO {

    private Long prescriptionId;

    private Long diagnosisId;

    private Long doctorId;

    private String doctorName;

    private Long patientId;

    private String patientName;

    private Integer status;

    private String statusDesc;

    private BigDecimal totalAmount;

    private List<PrescriptionDetailVO> details;

    private LocalDateTime createTime;

}
