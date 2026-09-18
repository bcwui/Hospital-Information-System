package com.project.his.service.entity;

import com.project.his.domain.po.Bill;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IBillService extends IService<Bill> {

    List<Bill> getByPatientId(Long patientId);

    List<Bill> getByStatus(Integer status);

}
