package com.project.his.config.ai.tools;

import com.project.his.config.ai.event.ToolEventEmitter;
import com.project.his.domain.po.Clinic;
import com.project.his.domain.po.Department;
import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.Schedule;
import com.project.his.service.entity.IClinicService;
import com.project.his.service.entity.IDepartmentService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 医院信息查询工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalTools {

    private final IDepartmentService departmentService;
    private final IClinicService clinicService;
    private final IDoctorService doctorService;
    private final IScheduleService scheduleService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Tool(description = "查询医院所有科室列表，返回科室名称和ID")
    public String listDepartments() {
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        if (emitter != null) {
            emitter.emitToolCall("listDepartments", "正在查询科室列表");
        }
        log.info("AI调用工具: listDepartments");
        List<Department> departments = departmentService.getDepartmentList(true);
        if (departments == null || departments.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("listDepartments", true);
            return "当前没有可用的科室信息";
        }
        StringBuilder sb = new StringBuilder("医院科室列表：\n");
        for (Department dept : departments) {
            sb.append("- ").append(dept.getDeptName())
                    .append(" (ID: ").append(dept.getDeptId()).append(")\n");
        }
        if (emitter != null) emitter.emitToolResult("listDepartments", true);
        return sb.toString();
    }

    @Tool(description = "根据科室ID查询该科室下的所有门诊列表")
    public String listClinicsByDepartment(
            @ToolParam(description = "科室ID") Long deptId) {
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        String deptName = departmentService.getDepartmentName(deptId);
        String label = deptName != null ? "正在查询" + deptName + "的门诊列表" : "正在查询科室(ID:" + deptId + ")的门诊列表";
        if (emitter != null) emitter.emitToolCall("listClinicsByDepartment", label);

        log.info("AI调用工具: listClinicsByDepartment, deptId={}", deptId);
        if (deptId == null) {
            if (emitter != null) emitter.emitToolResult("listClinicsByDepartment", false);
            return "请提供科室ID";
        }
        if (deptName == null) {
            if (emitter != null) emitter.emitToolResult("listClinicsByDepartment", false);
            return "未找到该科室";
        }
        List<Clinic> clinics = clinicService.getClinicsByDeptId(deptId, true);
        if (clinics == null || clinics.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("listClinicsByDepartment", true);
            return deptName + "科室下暂无门诊";
        }
        StringBuilder sb = new StringBuilder(deptName).append("科室的门诊列表：\n");
        for (Clinic clinic : clinics) {
            sb.append("- ").append(clinic.getClinicName())
                    .append(" (ID: ").append(clinic.getClinicId()).append(")\n");
        }
        if (emitter != null) emitter.emitToolResult("listClinicsByDepartment", true);
        return sb.toString();
    }

    @Tool(description = "根据门诊名称模糊搜索门诊")
    public String searchClinics(
            @ToolParam(description = "门诊名称关键词") String name) {
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        String label = "正在搜索门诊: " + (name != null ? name : "");
        if (emitter != null) emitter.emitToolCall("searchClinics", label);

        log.info("AI调用工具: searchClinics, name={}", name);
        if (name == null || name.trim().isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchClinics", false);
            return "请提供门诊名称关键词";
        }
        List<Clinic> clinics = clinicService.getClinicsByName(name.trim());
        if (clinics == null || clinics.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchClinics", true);
            return "未找到包含 \"" + name + "\" 的门诊";
        }
        StringBuilder sb = new StringBuilder("搜索到的门诊：\n");
        for (Clinic clinic : clinics) {
            sb.append("- ").append(clinic.getClinicName())
                    .append(" (ID: ").append(clinic.getClinicId()).append(")\n");
        }
        if (emitter != null) emitter.emitToolResult("searchClinics", true);
        return sb.toString();
    }

    @Tool(description = "根据门诊ID查询该门诊的所有医生")
    public String listDoctorsByClinic(
            @ToolParam(description = "门诊ID") Long clinicId) {
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        String clinicName = clinicService.getClinicName(clinicId);
        String label = clinicName != null ? "正在查询" + clinicName + "的医生列表" : "正在查询门诊(ID:" + clinicId + ")的医生列表";
        if (emitter != null) emitter.emitToolCall("listDoctorsByClinic", label);

        log.info("AI调用工具: listDoctorsByClinic, clinicId={}", clinicId);
        if (clinicId == null) {
            if (emitter != null) emitter.emitToolResult("listDoctorsByClinic", false);
            return "请提供门诊ID";
        }
        if (clinicName == null) {
            if (emitter != null) emitter.emitToolResult("listDoctorsByClinic", false);
            return "未找到该门诊";
        }
        List<Doctor> doctors = doctorService.getDoctorsByClinicId(clinicId);
        if (doctors == null || doctors.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("listDoctorsByClinic", true);
            return clinicName + "门诊暂无坐诊医生";
        }
        StringBuilder sb = new StringBuilder(clinicName).append("的医生列表：\n");
        for (Doctor doctor : doctors) {
            sb.append("- ").append(doctor.getName())
                    .append("，").append(doctor.getTitle())
                    .append(" (ID: ").append(doctor.getDoctorId()).append(")\n");
            if (doctor.getIntroduction() != null && !doctor.getIntroduction().isEmpty()) {
                sb.append("  简介：").append(doctor.getIntroduction()).append("\n");
            }
        }
        if (emitter != null) emitter.emitToolResult("listDoctorsByClinic", true);
        return sb.toString();
    }

    @Tool(description = "根据医生姓名模糊搜索医生")
    public String searchDoctors(
            @ToolParam(description = "医生姓名关键词") String name) {
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        String label = "正在搜索医生: " + (name != null ? name : "");
        if (emitter != null) emitter.emitToolCall("searchDoctors", label);

        log.info("AI调用工具: searchDoctors, name={}", name);
        if (name == null || name.trim().isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchDoctors", false);
            return "请提供医生姓名关键词";
        }
        List<Doctor> doctors = doctorService.getDoctorsByName(name.trim());
        if (doctors == null || doctors.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("searchDoctors", true);
            return "未找到姓名包含 \"" + name + "\" 的医生";
        }
        StringBuilder sb = new StringBuilder("搜索到的医生：\n");
        for (Doctor doctor : doctors) {
            String clinicName = clinicService.getClinicName(doctor.getClinicId());
            sb.append("- ").append(doctor.getName())
                    .append("，").append(doctor.getTitle())
                    .append("，").append(clinicName != null ? clinicName : "未知门诊")
                    .append(" (ID: ").append(doctor.getDoctorId()).append(")\n");
        }
        if (emitter != null) emitter.emitToolResult("searchDoctors", true);
        return sb.toString();
    }

    @Tool(description = "查询指定医生在某个日期范围内的排班情况")
    public String getDoctorSchedules(
            @ToolParam(description = "医生ID") Long doctorId,
            @ToolParam(description = "开始日期，格式：yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd") String endDate) {
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        Doctor doctor = doctorService.getById(doctorId);
        String doctorName = doctor != null ? doctor.getName() : "ID:" + doctorId;
        String label = "正在查询" + doctorName + "医生的排班";
        if (emitter != null) emitter.emitToolCall("getDoctorSchedules", label);

        log.info("AI调用工具: getDoctorSchedules, doctorId={}, startDate={}, endDate={}", doctorId, startDate, endDate);
        if (doctorId == null) {
            if (emitter != null) emitter.emitToolResult("getDoctorSchedules", false);
            return "请提供医生ID";
        }
        if (doctor == null) {
            if (emitter != null) emitter.emitToolResult("getDoctorSchedules", false);
            return "未找到该医生";
        }

        LocalDate start = parseDate(startDate, LocalDate.now());
        LocalDate end = parseDate(endDate, start.plusDays(7));

        List<Schedule> schedules = scheduleService.getSchedulesByDoctorAndDateRange(doctorId, start, end);
        if (schedules == null || schedules.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("getDoctorSchedules", true);
            return doctor.getName() + "医生在 " + start + " 至 " + end + " 期间暂无排班";
        }

        StringBuilder sb = new StringBuilder(doctor.getName())
                .append("医生的排班（").append(start).append(" 至 ").append(end).append("）：\n");
        for (Schedule schedule : schedules) {
            int remaining = schedule.getMaxPatients() - schedule.getCurrentPatients();
            sb.append("- 排班ID: ").append(schedule.getScheduleId())
                    .append("，").append(schedule.getScheduleDate())
                    .append(" ").append(schedule.getTimeSlot())
                    .append("，剩余名额：").append(remaining)
                    .append(remaining > 0 ? "（可预约）" : "（已满）")
                    .append("\n");
        }
        if (emitter != null) emitter.emitToolResult("getDoctorSchedules", true);
        return sb.toString();
    }

    @Tool(description = "查询指定日期范围内所有有剩余名额的排班")
    public String getAvailableSchedules(
            @ToolParam(description = "开始日期，格式：yyyy-MM-dd，默认为今天") String startDate,
            @ToolParam(description = "结束日期，格式：yyyy-MM-dd，默认为7天后") String endDate) {
        ToolEventEmitter emitter = ToolResultEmitterHolder.get();
        LocalDate start = parseDate(startDate, LocalDate.now());
        LocalDate end = parseDate(endDate, start.plusDays(7));
        String label = "正在查询 " + start + " 至 " + end + " 的可预约排班";
        if (emitter != null) emitter.emitToolCall("getAvailableSchedules", label);

        log.info("AI调用工具: getAvailableSchedules, startDate={}, endDate={}", startDate, endDate);

        List<Schedule> schedules = scheduleService.getAvailableSchedules(start, end);
        if (schedules == null || schedules.isEmpty()) {
            if (emitter != null) emitter.emitToolResult("getAvailableSchedules", true);
            return start + " 至 " + end + " 期间暂无可预约的排班";
        }

        StringBuilder sb = new StringBuilder("可预约的排班（").append(start).append(" 至 ").append(end).append("）：\n");
        schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDoctorId))
                .forEach((docId, docSchedules) -> {
                    Doctor doctor = doctorService.getById(docId);
                    String doctorName = doctor != null ? doctor.getName() : "未知医生";
                    sb.append("\n【").append(doctorName).append("】\n");
                    for (Schedule s : docSchedules) {
                        int remaining = s.getMaxPatients() - s.getCurrentPatients();
                        sb.append("  - 排班ID: ").append(s.getScheduleId())
                                .append("，").append(s.getScheduleDate())
                                .append(" ").append(s.getTimeSlot())
                                .append("，剩余名额：").append(remaining).append("\n");
                    }
                });
        if (emitter != null) emitter.emitToolResult("getAvailableSchedules", true);
        return sb.toString();
    }

    private LocalDate parseDate(String dateStr, LocalDate defaultValue) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return defaultValue;
        }
    }
}
