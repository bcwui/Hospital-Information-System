package com.project.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.project.his.common.Result;
import com.project.his.domain.dto.DiagnosisDTO;
import com.project.his.domain.po.Clinic;
import com.project.his.domain.po.Department;
import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.User;
import com.project.his.domain.vo.DiagnosisVO;
import com.project.his.domain.vo.DoctorProfileVO;
import com.project.his.exception.BusinessException;
import com.project.his.service.business.IMedicalService;
import com.project.his.service.entity.IClinicService;
import com.project.his.service.entity.IDepartmentService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description 医疗服务模块控制器
 * @create 2025-03-30 16:12
 */
@Slf4j
@RestController
@RequestMapping("/medical")
@RequiredArgsConstructor
public class MedicalServiceController {

    private final IMedicalService medicalService;
    private final IDoctorService doctorService;
    private final IUserService userService;
    private final IClinicService clinicService;
    private final IDepartmentService departmentService;

    /**
     * 获取当前登录医生的信息
     *
     * @return 医生信息
     */
    @SaCheckRole("doctor")
    @GetMapping("/doctor/profile")
    public Result<DoctorProfileVO> getCurrentDoctorInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Doctor doctor = doctorService.getDoctorByUserId(userId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }

        DoctorProfileVO vo = new DoctorProfileVO();
        vo.setDoctorId(doctor.getDoctorId());
        vo.setUserId(user.getId());
        vo.setName(doctor.getName());
        vo.setTitle(doctor.getTitle());
        vo.setIntroduction(doctor.getIntroduction());
        vo.setClinicId(doctor.getClinicId());
        vo.setAvatar(user.getAvatar());

        // 获取门诊和科室信息
        if (doctor.getClinicId() != null) {
            Clinic clinic = clinicService.getById(doctor.getClinicId());
            if (clinic != null) {
                vo.setClinicName(clinic.getClinicName());
                vo.setDeptId(clinic.getDeptId());

                if (clinic.getDeptId() != null) {
                    Department dept = departmentService.getById(clinic.getDeptId());
                    if (dept != null) {
                        vo.setDeptName(dept.getDeptName());
                    }
                }
            }
        }

        return Result.success(vo);
    }

    /**
     * 获取患者的诊断记录列表
     *
     * @param patientId 患者ID
     * @return 诊断记录列表
     */
    @SaCheckRole("patient")
    @GetMapping("/patient/{patientId}/diagnoses")
    public Result<List<DiagnosisVO>> getPatientDiagnoses(@PathVariable Long patientId) {
        log.info("获取患者的诊断记录列表, patientId: {}", patientId);
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();

            // 验证是否为管理员或当前患者
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(patientId)) {
                return Result.error("无权访问该患者的诊断记录");
            }

            List<DiagnosisVO> diagnoses = medicalService.getPatientDiagnoses(patientId);
            return Result.success("获取患者诊断记录成功", diagnoses);
        } catch (BusinessException e) {
            log.error("获取患者诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取患者诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }

    /**
     * 获取医生的诊断记录列表
     *
     * @param doctorId 医生ID
     * @return 诊断记录列表
     */
    @SaCheckRole("doctor")
    @GetMapping("/doctor/{doctorId}/diagnoses")
    public Result<List<DiagnosisVO>> getDoctorDiagnoses(@PathVariable Long doctorId) {
        log.info("获取医生的诊断记录列表, doctorId: {}", doctorId);
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();

            // 验证是否为管理员或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentDoctor(doctorId)) {
                return Result.error("无权访问该医生的诊断记录");
            }

            List<DiagnosisVO> diagnoses = medicalService.getDoctorDiagnoses(doctorId);
            return Result.success("获取医生诊断记录成功", diagnoses);
        } catch (BusinessException e) {
            log.error("获取医生诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取医生诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }

    /**
     * 获取诊断详情
     *
     * @param diagId 诊断ID
     * @return 诊断详情
     */
    @SaCheckRole(value = {"doctor", "patient"}, mode = SaMode.OR)
    @GetMapping("/diagnoses/{diagId}")
    public Result<DiagnosisVO> getDiagnosisDetail(@PathVariable Long diagId) {
        log.info("获取诊断详情, diagId: {}", diagId);
        try {
            DiagnosisVO diagnosis = medicalService.getDiagnosisDetail(diagId);

            // 获取当前登录用户
            User user = medicalService.getCurrentUser();

            // 验证是否为管理员、当前患者或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(diagnosis.getPatientId()) && !medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                return Result.error("无权访问该诊断记录");
            }

            return Result.success("获取诊断详情成功", diagnosis);
        } catch (BusinessException e) {
            log.error("获取诊断详情业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取诊断详情异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }

    /**
     * 创建诊断记录
     *
     * @param dto 诊断记录DTO
     * @return 诊断记录详情
     */
    @SaCheckRole("doctor")
    @PostMapping("/diagnoses")
    public Result<DiagnosisVO> createDiagnosis(@RequestBody DiagnosisDTO dto) {
        log.info("接收到创建诊断记录请求");
        try {
            // 获取当前登录用户
            User user = medicalService.getCurrentUser();

            // 验证当前医生身份
            if (!medicalService.isCurrentDoctor(dto.getDoctorId())) {
                return Result.error("无权创建该诊断记录");
            }

            DiagnosisVO diagnosis = medicalService.createDiagnosis(dto);
            return Result.success("创建诊断记录成功", diagnosis);
        } catch (BusinessException e) {
            log.error("创建诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }

    /**
     * 根据预约ID获取诊断记录
     *
     * @param appointmentId 预约ID
     * @return 诊断记录详情，如果不存在则返回null
     */
    @SaCheckRole(value = {"doctor", "patient"}, mode = SaMode.OR)
    @GetMapping("/appointment/{appointmentId}/diagnosis")
    public Result<DiagnosisVO> getDiagnosisByAppointmentId(@PathVariable Long appointmentId) {
        log.info("接收到根据预约ID获取诊断记录请求, appointmentId: {}", appointmentId);
        try {
            DiagnosisVO diagnosis = medicalService.getDiagnosisByAppointmentId(appointmentId);

            if (diagnosis == null) {
                return Result.success("该预约尚未有诊断记录", null);
            }

            // 获取当前登录用户
            User user = medicalService.getCurrentUser();

            // 验证是否为管理员、当前患者或当前医生
            if (user.getRole() != 2 && !medicalService.isCurrentPatient(diagnosis.getPatientId()) && !medicalService.isCurrentDoctor(diagnosis.getDoctorId())) {
                return Result.error("无权访问该诊断记录");
            }

            return Result.success("获取诊断记录成功", diagnosis);
        } catch (BusinessException e) {
            log.error("获取诊断记录业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取诊断记录异常", e);
            return Result.error("服务器异常，请稍后重试");
        }
    }
}
