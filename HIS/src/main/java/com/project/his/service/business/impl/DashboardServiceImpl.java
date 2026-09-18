package com.project.his.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.po.*;
import com.project.his.domain.vo.*;
import com.project.his.service.business.IDashboardService;
import com.project.his.service.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final IAppointmentService appointmentService;
    private final IBillService billService;
    private final IPatientService patientService;
    private final IAIService aiService;
    private final IDoctorService doctorService;
    private final IDepartmentService departmentService;
    private final IClinicService clinicService;

    @Override
    public DashboardTodayVO getTodayStats() {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<Appointment> aptQuery = new LambdaQueryWrapper<>();
        aptQuery.eq(Appointment::getAppointmentDate, today)
                .eq(Appointment::getStatus, 0);
        long apptCount = appointmentService.count(aptQuery);

        LambdaQueryWrapper<Bill> billQuery = new LambdaQueryWrapper<>();
        billQuery.eq(Bill::getStatus, 2);
        billQuery.apply("DATE(pay_time) = {0}", today);
        BigDecimal revenue = billService.list(billQuery).stream()
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long newPatients = patientService.count(new LambdaQueryWrapper<Patient>()
                .apply("DATE(create_time) = {0}", today));

        long aiCount = aiService.count(new LambdaQueryWrapper<AiConsultRecord>()
                .apply("DATE(create_time) = {0}", today));

        return DashboardTodayVO.builder()
                .appointmentCount(apptCount)
                .revenue(revenue)
                .newPatients(newPatients)
                .aiConsultCount(aiCount)
                .build();
    }

    @Override
    public List<DashboardTrendVO> getAppointmentTrend(int days) {
        List<DashboardTrendVO> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LambdaQueryWrapper<Appointment> query = new LambdaQueryWrapper<>();
            query.eq(Appointment::getAppointmentDate, date);
            long count = appointmentService.count(query);
            result.add(DashboardTrendVO.builder().date(date.toString()).count(count).build());
        }
        return result;
    }

    @Override
    public List<DashboardTrendVO> getRevenueTrend(int days) {
        List<DashboardTrendVO> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LambdaQueryWrapper<Bill> query = new LambdaQueryWrapper<>();
            query.eq(Bill::getStatus, 2);
            query.apply("DATE(pay_time) = {0}", date);
            BigDecimal amount = billService.list(query).stream()
                    .map(Bill::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(DashboardTrendVO.builder().date(date.toString()).count(0).amount(amount).build());
        }
        return result;
    }

    @Override
    public List<DashboardDeptVO> getDeptDistribution(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Appointment> appointments = appointmentService.list(new LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getAppointmentDate, startDate));

        Map<Long, Long> deptCountMap = new HashMap<>();
        for (Appointment apt : appointments) {
            Doctor doctor = doctorService.getById(apt.getDoctorId());
            if (doctor == null || doctor.getClinicId() == null) continue;
            Clinic clinic = clinicService.getById(doctor.getClinicId());
            if (clinic == null || clinic.getDeptId() == null) continue;
            deptCountMap.merge(clinic.getDeptId(), 1L, Long::sum);
        }

        List<DashboardDeptVO> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : deptCountMap.entrySet()) {
            Department dept = departmentService.getById(entry.getKey());
            result.add(DashboardDeptVO.builder()
                    .deptName(dept != null ? dept.getDeptName() : "未知科室")
                    .count(entry.getValue())
                    .build());
        }
        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        return result;
    }

    @Override
    public List<DashboardDoctorWorkloadVO> getDoctorWorkload(int days, int limit) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Appointment> appointments = appointmentService.list(new LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getAppointmentDate, startDate));

        Map<Long, Long> doctorCountMap = new HashMap<>();
        for (Appointment apt : appointments) {
            doctorCountMap.merge(apt.getDoctorId(), 1L, Long::sum);
        }

        return doctorCountMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(entry -> {
                    Doctor doctor = doctorService.getById(entry.getKey());
                    return DashboardDoctorWorkloadVO.builder()
                            .doctorName(doctor != null ? doctor.getName() : "未知医生")
                            .count(entry.getValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public DashboardAIVO getAIStats() {
        long totalSessions = aiService.count();
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<AiConsultRecord> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.apply("DATE(create_time) = {0}", today);
        long todaySessions = aiService.count(todayQuery);

        double avgMessages = 0;
        if (totalSessions > 0) {
            List<AiConsultRecord> records = aiService.list();
            long totalMessages = records.stream()
                    .filter(r -> r.getConversation() != null && !r.getConversation().isEmpty())
                    .count();
            avgMessages = (double) totalMessages / totalSessions;
        }

        return DashboardAIVO.builder()
                .totalSessions(totalSessions)
                .todaySessions(todaySessions)
                .avgMessagesPerSession(Math.round(avgMessages * 10.0) / 10.0)
                .build();
    }
}
