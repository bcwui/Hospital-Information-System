package com.project.his.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.his.domain.po.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 医院科室表 Mapper 接口
 * </p>
 *
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

}
