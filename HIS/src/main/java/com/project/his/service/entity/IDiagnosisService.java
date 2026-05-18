package com.project.his.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.his.domain.po.Diagnosis;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 医生诊断记录表 服务类
 * </p>
 *
 */
public interface IDiagnosisService extends IService<Diagnosis> {

    /**
     * 根据患者ID获取诊断记录列表
     * @param patientId 患者ID
     * @return 诊断记录列表
     */
    List<Diagnosis> getDiagnosesByPatientId(Long patientId);
    
    /**
     * 根据医生ID获取诊断记录列表
     * @param doctorId 医生ID
     * @return 诊断记录列表
     */
    List<Diagnosis> getDiagnosesByDoctorId(Long doctorId);

    /**
     * 获取患者和医生之间的诊断记录
     * @param patientId 患者ID
     * @param doctorId 医生ID
     * @return 诊断记录列表
     */
    List<Diagnosis> getDiagnosesByPatientAndDoctor(Long patientId, Long doctorId);
    
    /**
     * 获取诊断详情
     * @param diagId 诊断ID
     * @return 诊断记录
     */
    Diagnosis getDiagnosisDetail(Long diagId);
}
