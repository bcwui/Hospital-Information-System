package com.graduation.his.service.business;

import com.graduation.his.domain.po.User;
import com.graduation.his.domain.vo.DiagnosisVO;
import com.graduation.his.domain.dto.DiagnosisDTO;

import java.util.List;

/**
 * @author hua
 * @description 医疗服务接口
 * @create 2025-04-02 22:51
 */
public interface IMedicalService {

    /**
     * 获取患者的诊断记录列表
     * @param patientId 患者ID
     * @return 诊断记录VO列表
     */
    List<DiagnosisVO> getPatientDiagnoses(Long patientId);

    /**
     * 获取医生的诊断记录列表
     * @param doctorId 医生ID
     * @return 诊断记录VO列表
     */
    List<DiagnosisVO> getDoctorDiagnoses(Long doctorId);

    /**
     * 获取诊断详情
     * @param diagId 诊断ID
     * @return 诊断记录VO
     */
    DiagnosisVO getDiagnosisDetail(Long diagId);

    /**
     * 根据用户ID获取患者ID
     * @param userId 用户ID
     * @return 患者ID
     */
    Long getPatientIdByUserId(Long userId);

    /**
     * 根据用户ID获取医生ID
     * @param userId 用户ID
     * @return 医生ID
     */
    Long getDoctorIdByUserId(Long userId);

    /**
     * 获取当前登录用户
     * @return 当前用户
     */
    User getCurrentUser();

    /**
     * 判断当前用户是否为指定患者
     * @param patientId 患者ID
     * @return 是否为当前患者
     */
    boolean isCurrentPatient(Long patientId);

    /**
     * 判断当前用户是否为指定医生
     * @param doctorId 医生ID
     * @return 是否为当前医生
     */
    boolean isCurrentDoctor(Long doctorId);

    /**
     * 创建诊断记录
     *
     * @param dto 诊断记录数据传输对象
     * @return 诊断记录对象
     */
    DiagnosisVO createDiagnosis(DiagnosisDTO dto);

    /**
     * 根据预约ID获取诊断记录
     *
     * @param appointmentId 预约ID
     * @return 诊断记录，如果不存在则返回null
     */
    DiagnosisVO getDiagnosisByAppointmentId(Long appointmentId);
}
