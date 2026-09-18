package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.Drug;
import com.project.his.mapper.DrugMapper;
import com.project.his.service.entity.IDrugService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DrugServiceImpl extends ServiceImpl<DrugMapper, Drug> implements IDrugService {

    @Override
    public List<Drug> searchByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Drug> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Drug::getDrugName, name)
                .eq(Drug::getIsActive, 1)
                .orderByAsc(Drug::getDrugName);
        return list(queryWrapper);
    }

    @Override
    public List<Drug> getActiveDrugs() {
        LambdaQueryWrapper<Drug> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Drug::getIsActive, 1)
                .orderByAsc(Drug::getDrugName);
        return list(queryWrapper);
    }

}
