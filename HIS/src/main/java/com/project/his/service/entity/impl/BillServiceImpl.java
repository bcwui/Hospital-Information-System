package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.Bill;
import com.project.his.mapper.BillMapper;
import com.project.his.service.entity.IBillService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements IBillService {

    @Override
    public List<Bill> getByPatientId(Long patientId) {
        LambdaQueryWrapper<Bill> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Bill::getPatientId, patientId)
                .orderByDesc(Bill::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public List<Bill> getByStatus(Integer status) {
        LambdaQueryWrapper<Bill> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Bill::getStatus, status)
                .orderByDesc(Bill::getCreateTime);
        return list(queryWrapper);
    }

}
