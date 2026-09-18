package com.project.his.service.entity;

import com.project.his.domain.po.BillItem;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IBillItemService extends IService<BillItem> {

    List<BillItem> getByBillId(Long billId);

}
