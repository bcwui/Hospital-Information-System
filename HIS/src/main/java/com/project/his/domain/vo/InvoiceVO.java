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
public class InvoiceVO {

    private Long billId;

    private String patientName;

    private String doctorName;

    private String deptName;

    private BigDecimal totalAmount;

    private String paymentMethod;

    private String statusDesc;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private List<BillItemVO> items;

}
