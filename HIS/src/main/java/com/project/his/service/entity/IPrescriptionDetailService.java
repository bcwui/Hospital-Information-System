package com.project.his.service.entity;

import com.project.his.domain.po.PrescriptionDetail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IPrescriptionDetailService extends IService<PrescriptionDetail> {

    List<PrescriptionDetail> getByPrescriptionId(Long prescriptionId);

}
