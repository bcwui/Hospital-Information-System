package com.project.his.config.ai.tools;

import com.project.his.domain.po.Appointment;
import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.Schedule;
import com.project.his.domain.vo.AppointmentVO;
import com.project.his.service.business.IRegistrationService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预约挂号工具（需要鉴权）
 * 用于AI助手帮助患者进行预约相关操作
 */
@Slf4j
@Component
public class AppointmentTools {

    private final IRegistrationService registrationService;
    private final IScheduleService scheduleService;
    private final IDoctorService doctorService;

    public AppointmentTools(@Lazy IRegistrationService registrationService,
                            IScheduleService scheduleService,
                            IDoctorService doctorService) {
        this.registrationService = registrationService;
        this.scheduleService = scheduleService;
        this.doctorService = doctorService;
    }

    /**
     * 查询当前患者的预约记录
     */
    @Tool(description = "查询当前患者的预约挂号记录，可按状态筛选：0-待就诊，1-已就诊，2-已取消")
    public String getMyAppointments(
            @ToolParam(description = "预约状态筛选，可选值：0-待就诊，1-已就诊，2-已取消，不传则查询全部") Integer status) {
        Long patientId = PatientContext.getPatientId();
        log.info("AI调用工具: getMyAppointments, patientId={}, status={}", patientId, status);

        if (patientId == null) {
            return "无法获取当前患者信息，请确保已登录";
        }

        try {
            List<AppointmentVO> appointments = registrationService.getPatientAppointmentVOs(patientId, status);
            if (appointments == null || appointments.isEmpty()) {
                String statusDesc = getStatusDesc(status);
                return "您当前没有" + statusDesc + "的预约记录";
            }

            StringBuilder sb = new StringBuilder("您的预约记录：\n");
            for (AppointmentVO vo : appointments) {
                sb.append("- 预约ID: ").append(vo.getAppointmentId())
                        .append("，医生: ").append(vo.getDoctorName())
                        .append("，日期: ").append(vo.getAppointmentDate())
                        .append(" ").append(vo.getTimeSlot())
                        .append("，状态: ").append(getStatusDesc(vo.getStatus()))
                        .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("查询预约记录失败", e);
            return "查询预约记录失败：" + e.getMessage();
        }
    }

    /**
     * 为当前患者创建预约
     */
    @Tool(description = "为当前患者创建预约挂号，需要提供排班ID")
    public String createAppointment(
            @ToolParam(description = "排班ID，可通过查询医生排班获取") Long scheduleId) {
        Long patientId = PatientContext.getPatientId();
        log.info("AI调用工具: createAppointment, patientId={}, scheduleId={}", patientId, scheduleId);

        if (patientId == null) {
            return "无法获取当前患者信息，请确保已登录";
        }

        if (scheduleId == null) {
            return "请提供排班ID";
        }

        // 检查排班是否存在且有余号
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            return "排班不存在，请确认排班ID是否正确";
        }

        int remaining = schedule.getMaxPatients() - schedule.getCurrentPatients();
        if (remaining <= 0) {
            return "该排班已满，无法预约";
        }

        try {
            AppointmentVO vo = registrationService.createAppointmentVO(patientId, scheduleId, 0);
            Doctor doctor = doctorService.getById(schedule.getDoctorId());
            String doctorName = doctor != null ? doctor.getName() : "未知医生";

            return "预约成功！\n" +
                    "预约ID: " + vo.getAppointmentId() + "\n" +
                    "医生: " + doctorName + "\n" +
                    "日期: " + vo.getAppointmentDate() + " " + vo.getTimeSlot() + "\n" +
                    "请按时就诊，如需取消请提前操作。";
        } catch (Exception e) {
            log.error("创建预约失败", e);
            return "预约失败：" + e.getMessage();
        }
    }

    /**
     * 取消当前患者的预约
     */
    @Tool(description = "取消当前患者的预约挂号")
    public String cancelAppointment(
            @ToolParam(description = "要取消的预约ID") Long appointmentId) {
        Long patientId = PatientContext.getPatientId();
        log.info("AI调用工具: cancelAppointment, patientId={}, appointmentId={}", patientId, appointmentId);

        if (patientId == null) {
            return "无法获取当前患者信息，请确保已登录";
        }

        if (appointmentId == null) {
            return "请提供要取消的预约ID";
        }

        try {
            boolean result = registrationService.cancelAppointment(appointmentId, patientId);
            if (result) {
                return "预约已成功取消（预约ID: " + appointmentId + "）";
            } else {
                return "取消预约失败，请确认预约ID是否正确，且预约状态为待就诊";
            }
        } catch (Exception e) {
            log.error("取消预约失败", e);
            return "取消预约失败：" + e.getMessage();
        }
    }

    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "待就诊";
            case 1 -> "已就诊";
            case 2 -> "已取消";
            default -> "未知状态";
        };
    }
}
