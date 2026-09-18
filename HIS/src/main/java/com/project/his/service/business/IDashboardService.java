package com.project.his.service.business;

import com.project.his.domain.vo.*;

import java.util.List;

public interface IDashboardService {

    DashboardTodayVO getTodayStats();

    List<DashboardTrendVO> getAppointmentTrend(int days);

    List<DashboardTrendVO> getRevenueTrend(int days);

    List<DashboardDeptVO> getDeptDistribution(int days);

    List<DashboardDoctorWorkloadVO> getDoctorWorkload(int days, int limit);

    DashboardAIVO getAIStats();

}
