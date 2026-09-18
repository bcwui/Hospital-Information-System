package com.project.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.project.his.common.Result;
import com.project.his.domain.vo.*;
import com.project.his.service.business.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckRole("admin")
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping("/today")
    public Result<DashboardTodayVO> getTodayStats() {
        return Result.success(dashboardService.getTodayStats());
    }

    @GetMapping("/appointment-trend")
    public Result<List<DashboardTrendVO>> getAppointmentTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(dashboardService.getAppointmentTrend(days));
    }

    @GetMapping("/revenue-trend")
    public Result<List<DashboardTrendVO>> getRevenueTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(dashboardService.getRevenueTrend(days));
    }

    @GetMapping("/dept-distribution")
    public Result<List<DashboardDeptVO>> getDeptDistribution(@RequestParam(defaultValue = "30") int days) {
        return Result.success(dashboardService.getDeptDistribution(days));
    }

    @GetMapping("/doctor-workload")
    public Result<List<DashboardDoctorWorkloadVO>> getDoctorWorkload(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(dashboardService.getDoctorWorkload(days, limit));
    }

    @GetMapping("/ai-stats")
    public Result<DashboardAIVO> getAIStats() {
        return Result.success(dashboardService.getAIStats());
    }
}
