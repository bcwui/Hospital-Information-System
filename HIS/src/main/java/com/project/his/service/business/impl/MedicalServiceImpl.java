package com.project.his.service.business.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.project.his.domain.po.Diagnosis;
import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.Patient;
import com.project.his.domain.po.User;
import com.project.his.domain.vo.DiagnosisVO;
import com.project.his.exception.BusinessException;
import com.project.his.service.business.IMedicalService;
import com.project.his.service.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.dto.DiagnosisDTO;
import com.project.his.domain.po.Appointment;

/**
 * @description 医疗服务实现类
 * @create 2025-04-02 22:51
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalServiceImpl implements IMedicalService {

    private final IDiagnosisService diagnosisService;

    private final IPatientService patientService;

    private final IDoctorService doctorService;

    private final IUserService userService;

    private final IAppointmentService appointmentService;

    @Override
    public List<DiagnosisVO> getPatientDiagnoses(Long patientId) {
        log.info("获取患者的诊断记录列表, patientId: {}", patientId);

        if (patientId == null) {
            throw new BusinessException("患者ID不能为空");
        }

        List<Diagnosis> diagnoses = diagnosisService.getDiagnosesByPatientId(patientId);
        if (diagnoses == null || diagnoses.isEmpty()) {
            return new ArrayList<>();
        }

        return convertToDiagnosisVOList(diagnoses);
    }

    @Override
    public List<DiagnosisVO> getDoctorDiagnoses(Long doctorId) {
        log.info("获取医生的诊断记录列表, doctorId: {}", doctorId);

        if (doctorId == null) {
            throw new BusinessException("医生ID不能为空");
        }

        List<Diagnosis> diagnoses = diagnosisService.getDiagnosesByDoctorId(doctorId);
        if (diagnoses == null || diagnoses.isEmpty()) {
            return new ArrayList<>();
        }

        return convertToDiagnosisVOList(diagnoses);
    }

    @Override
    public DiagnosisVO getDiagnosisDetail(Long diagId) {
        log.info("获取诊断详情, diagId: {}", diagId);

        if (diagId == null) {
            throw new BusinessException("诊断ID不能为空");
        }

        Diagnosis diagnosis = diagnosisService.getById(diagId);
        if (diagnosis == null) {
            throw new BusinessException("诊断记录不存在");
        }

        return convertToDiagnosisVO(diagnosis);
    }

    /**
     * 将诊断记录转换为VO对象
     * @param diagnosis 诊断记录
     * @return 诊断记录VO
     */
    private DiagnosisVO convertToDiagnosisVO(Diagnosis diagnosis) {
        if (diagnosis == null) {
            return null;
        }

        DiagnosisVO vo = new DiagnosisVO();
        BeanUtils.copyProperties(diagnosis, vo);

        // 获取患者信息
        Patient patient = patientService.getById(diagnosis.getPatientId());
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }

        // 获取医生信息
        Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
        if (doctor != null) {
            vo.setDoctorName(doctor.getName());
            vo.setDoctorTitle(doctor.getTitle());
        }

        return vo;
    }

    /**
     * 将诊断记录列表转换为VO对象列表
     * @param diagnoses 诊断记录列表
     * @return 诊断记录VO列表
     */
    private List<DiagnosisVO> convertToDiagnosisVOList(List<Diagnosis> diagnoses) {
        if (diagnoses == null || diagnoses.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有患者ID和医生ID
        List<Long> patientIds = diagnoses.stream()
                .map(Diagnosis::getPatientId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> doctorIds = diagnoses.stream()
                .map(Diagnosis::getDoctorId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询患者和医生信息
        Map<Long, Patient> patientMap = patientService.listByIds(patientIds).stream()
                .collect(Collectors.toMap(Patient::getPatientId, patient -> patient));

        Map<Long, Doctor> doctorMap = doctorService.listByIds(doctorIds).stream()
                .collect(Collectors.toMap(Doctor::getDoctorId, doctor -> doctor));

        // 转换为VO对象
        return diagnoses.stream().map(diagnosis -> {
            DiagnosisVO vo = new DiagnosisVO();
            BeanUtils.copyProperties(diagnosis, vo);

            // 设置患者信息
            Patient patient = patientMap.get(diagnosis.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }

            // 设置医生信息
            Doctor doctor = doctorMap.get(diagnosis.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDoctorTitle(doctor.getTitle());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public User getCurrentUser() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            return userService.getById(userId);
        } catch (Exception e) {
            log.error("获取当前用户异常", e);
            throw new BusinessException("获取当前用户失败");
        }
    }

    @Override
    public boolean isCurrentPatient(Long patientId) {
        try {
            User user = getCurrentUser();

            // 如果是患者角色，检查patientId是否对应
            if (user.getRole() == 0) {
                Long currentPatientId = getPatientIdByUserId(user.getId());
                return patientId.equals(currentPatientId);
            }

            return false;
        } catch (Exception e) {
            log.error("判断是否为当前患者异常", e);
            return false;
        }
    }

    @Override
    public boolean isCurrentDoctor(Long doctorId) {
        try {
            User user = getCurrentUser();

            // 如果是医生角色，检查doctorId是否对应
            if (user.getRole() == 1) {
                Long currentDoctorId = getDoctorIdByUserId(user.getId());
                return doctorId.equals(currentDoctorId);
            }

            return false;
        } catch (Exception e) {
            log.error("判断是否为当前医生异常", e);
            return false;
        }
    }

    @Override
    public Long getPatientIdByUserId(Long userId) {
        log.info("根据用户ID获取患者ID, userId: {}", userId);

        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        Patient patient = patientService.getByUserId(userId);
        if (patient == null) {
            throw new BusinessException("未找到患者信息");
        }

        return patient.getPatientId();
    }

    @Override
    public Long getDoctorIdByUserId(Long userId) {
        log.info("根据用户ID获取医生ID, userId: {}", userId);

        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        Doctor doctor = doctorService.getDoctorByUserId(userId);
        if (doctor == null) {
            throw new BusinessException("未找到医生信息");
        }

        return doctor.getDoctorId();
    }

    @Override
    public DiagnosisVO createDiagnosis(DiagnosisDTO dto) {
        log.info("创建诊断记录, appointmentId: {}, doctorId: {}, patientId: {}",
                dto.getAppointmentId(), dto.getDoctorId(), dto.getPatientId());

        // 参数验证
        if (dto.getAppointmentId() == null) {
            throw new BusinessException("预约ID不能为空");
        }
        if (dto.getDoctorId() == null) {
            throw new BusinessException("医生ID不能为空");
        }
        if (dto.getPatientId() == null) {
            throw new BusinessException("患者ID不能为空");
        }

        // 检查是否已存在该预约的诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getAppointmentId, dto.getAppointmentId());
        Diagnosis existingDiagnosis = diagnosisService.getOne(queryWrapper);

        if (existingDiagnosis != null) {
            throw new BusinessException("该预约已存在诊断记录，诊断记录一旦创建不可修改");
        }

        // 校验预约是否存在且状态是否正确
        try {
            Appointment appointment = appointmentService.getById(dto.getAppointmentId());
            if (appointment == null) {
                throw new BusinessException("预约记录不存在");
            }
            if (!appointment.getDoctorId().equals(dto.getDoctorId())) {
                throw new BusinessException("无权为其他医生的预约创建诊断记录");
            }
            if (!appointment.getPatientId().equals(dto.getPatientId())) {
                throw new BusinessException("预约患者信息不匹配");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("校验预约信息异常", e);
            throw new BusinessException("校验预约信息失败");
        }

        // 创建诊断记录
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setAppointmentId(dto.getAppointmentId());
        diagnosis.setDoctorId(dto.getDoctorId());
        diagnosis.setPatientId(dto.getPatientId());
        diagnosis.setDiagnosisResult(dto.getDiagnosisResult());
        diagnosis.setExamination(dto.getExamination());
        diagnosis.setPrescription(dto.getPrescription());
        diagnosis.setAdvice(dto.getAdvice());

        LocalDateTime now = LocalDateTime.now();
        diagnosis.setCreateTime(now);
        diagnosis.setUpdateTime(now);

        // 保存诊断记录
        boolean success = diagnosisService.save(diagnosis);
        if (!success) {
            throw new BusinessException("创建诊断记录失败");
        }

        log.info("诊断记录创建成功, diagId: {}", diagnosis.getDiagId());

        // 更新预约状态为已就诊
        updateAppointmentStatusToCompleted(dto.getAppointmentId());

        // 返回诊断记录VO
        return convertToDiagnosisVO(diagnosis);
    }

    @Override
    public DiagnosisVO getDiagnosisByAppointmentId(Long appointmentId) {
        log.info("根据预约ID获取诊断记录, appointmentId: {}", appointmentId);

        if (appointmentId == null) {
            throw new BusinessException("预约ID不能为空");
        }

        // 查询诊断记录
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getAppointmentId, appointmentId);
        Diagnosis diagnosis = diagnosisService.getOne(queryWrapper);

        if (diagnosis == null) {
            log.info("未找到预约ID: {}的诊断记录", appointmentId);
            return null;
        }

        return convertToDiagnosisVO(diagnosis);
    }

    /**
     * 更新预约状态为已完成
     * @param appointmentId 预约ID
     */
    private void updateAppointmentStatusToCompleted(Long appointmentId) {
        try {
            if (appointmentId == null) {
                log.warn("预约ID为空，无法更新状态");
                return;
            }

            // 获取预约记录
            Appointment appointment = appointmentService.getById(appointmentId);
            if (appointment == null) {
                log.warn("未找到预约记录: {}", appointmentId);
                return;
            }

            // 更新预约状态为已完成(1)
            appointment.setStatus(1);
            appointment.setUpdateTime(LocalDateTime.now());

            boolean result = appointmentService.updateById(appointment);
            if (result) {
                log.info("预约状态已更新为已完成, appointmentId: {}", appointmentId);
            } else {
                log.error("预约状态更新失败, appointmentId: {}", appointmentId);
                throw new BusinessException("更新预约状态失败");
            }
        } catch (Exception e) {
            log.error("更新预约状态异常", e);
            throw new BusinessException("更新预约状态失败: " + e.getMessage());
        }
    }
}
