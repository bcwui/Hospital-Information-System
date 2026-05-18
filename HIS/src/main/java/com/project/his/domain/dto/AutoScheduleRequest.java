package com.project.his.domain.dto;

import lombok.Data;

/**
 * 自动排班请求DTO
 */
@Data
public class AutoScheduleRequest {

    /**
     * 开始日期 (格式: yyyy-MM-dd)
     */
    private String startDate;

    /**
     * 结束日期 (格式: yyyy-MM-dd)
     */
    private String endDate;

    /**
     * 门诊ID (可选，为空时表示给所有门诊排班)
     */
    private Long clinicId;

    /**
     * 科室ID (可选，为空时表示给所有科室排班)
     */
    private Long deptId;
}
