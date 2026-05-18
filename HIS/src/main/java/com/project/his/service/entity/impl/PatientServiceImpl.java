package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.Patient;
import com.project.his.mapper.PatientMapper;
import com.project.his.service.entity.IPatientService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 患者信息表 服务实现类
 * </p>
 *
 */
@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements IPatientService {

    @Override
    public Patient getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<Patient>()
                .eq(Patient::getUserId, userId)
                .last("LIMIT 1"));
    }
}
