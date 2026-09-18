package com.project.his.service.entity;

import com.project.his.domain.po.Drug;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IDrugService extends IService<Drug> {

    List<Drug> searchByName(String name);

    List<Drug> getActiveDrugs();

}
