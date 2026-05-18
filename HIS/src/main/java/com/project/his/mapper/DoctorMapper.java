package com.project.his.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.his.domain.po.Doctor;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 医生信息表 Mapper 接口
 * </p>
 *
 */
@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

}
