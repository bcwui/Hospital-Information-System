package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.BillItem;
import com.project.his.mapper.BillItemMapper;
import com.project.his.service.entity.IBillItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillItemServiceImpl extends ServiceImpl<BillItemMapper, BillItem> implements IBillItemService {

    @Override
    public List<BillItem> getByBillId(Long billId) {
        LambdaQueryWrapper<BillItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BillItem::getBillId, billId);
        return list(queryWrapper);
    }

}
