package com.project.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医生个人信息VO
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfileVO {

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 医生姓名
     */
    private String name;

    /**
     * 职称
     */
    private String title;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 门诊ID
     */
    private Long clinicId;

    /**
     * 门诊名称
     */
    private String clinicName;

    /**
     * 科室ID
     */
    private Long deptId;

    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 头像URL
     */
    private String avatar;
}
