package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.PrescriptionDetail;
import com.project.his.mapper.PrescriptionDetailMapper;
import com.project.his.service.entity.IPrescriptionDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescriptionDetailServiceImpl extends ServiceImpl<PrescriptionDetailMapper, PrescriptionDetail> implements IPrescriptionDetailService {

    @Override
    public List<PrescriptionDetail> getByPrescriptionId(Long prescriptionId) {
        LambdaQueryWrapper<PrescriptionDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PrescriptionDetail::getPrescriptionId, prescriptionId);
        return list(queryWrapper);
    }

}
