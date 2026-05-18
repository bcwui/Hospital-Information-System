package com.project.his.service.entity.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.his.domain.po.Appointment;
import com.project.his.mapper.AppointmentMapper;
import com.project.his.service.entity.IAppointmentService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 预约挂号表 服务实现类
 * </p>
 *
 */
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {

}
