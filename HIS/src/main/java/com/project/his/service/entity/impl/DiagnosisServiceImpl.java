package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.Diagnosis;
import com.project.his.mapper.DiagnosisMapper;
import com.project.his.service.entity.IDiagnosisService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 医生诊断记录表 服务实现类
 * </p>
 *
 */
@Service
public class DiagnosisServiceImpl extends ServiceImpl<DiagnosisMapper, Diagnosis> implements IDiagnosisService {

    @Override
    public List<Diagnosis> getDiagnosesByPatientId(Long patientId) {
        if (patientId == null) {
            return null;
        }
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getPatientId, patientId);
        queryWrapper.orderByDesc(Diagnosis::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public List<Diagnosis> getDiagnosesByDoctorId(Long doctorId) {
        if (doctorId == null) {
            return null;
        }
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getDoctorId, doctorId);
        queryWrapper.orderByDesc(Diagnosis::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public List<Diagnosis> getDiagnosesByPatientAndDoctor(Long patientId, Long doctorId) {
        if (patientId == null || doctorId == null) {
            return null;
        }
        
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Diagnosis::getPatientId, patientId)
                .eq(Diagnosis::getDoctorId, doctorId)
                .orderByDesc(Diagnosis::getCreateTime);
        
        return list(queryWrapper);
    }

    @Override
    public Diagnosis getDiagnosisDetail(Long diagId) {
        if (diagId == null) {
            return null;
        }
        return getById(diagId);
    }
}
