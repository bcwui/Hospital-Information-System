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
public class BillVO {

    private Long billId;

    private Long appointmentId;

    private Long patientId;

    private String patientName;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal unpaidAmount;

    private Integer status;

    private String statusDesc;

    private String paymentMethod;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private List<BillItemVO> items;

}
