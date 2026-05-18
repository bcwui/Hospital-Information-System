package com.project.his.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.his.domain.po.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 医生排班表 Mapper 接口
 * </p>
 *
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

}
