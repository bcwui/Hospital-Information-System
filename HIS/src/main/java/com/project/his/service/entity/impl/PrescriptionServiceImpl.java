package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.Prescription;
import com.project.his.mapper.PrescriptionMapper;
import com.project.his.service.entity.IPrescriptionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription> implements IPrescriptionService {

    @Override
    public List<Prescription> getByPatientId(Long patientId) {
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getPatientId, patientId)
                .orderByDesc(Prescription::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public List<Prescription> getByDoctorId(Long doctorId) {
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getDoctorId, doctorId)
                .orderByDesc(Prescription::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public List<Prescription> getByDiagnosisId(Long diagnosisId) {
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getDiagnosisId, diagnosisId)
                .orderByDesc(Prescription::getCreateTime);
        return list(queryWrapper);
    }

}
