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
public class DashboardTodayVO {

    private long appointmentCount;

    private BigDecimal revenue;

    private long newPatients;

    private long aiConsultCount;

}
