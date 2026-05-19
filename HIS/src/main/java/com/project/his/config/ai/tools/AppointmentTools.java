package com.project.his.config.ai.tools;

import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.Schedule;
import com.project.his.domain.vo.AppointmentVO;
import com.project.his.service.business.IRegistrationService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预约挂号工具（需要鉴权）
 * <p>
 * 患者ID获取：LLM 从系统提示词中读取并作为参数传入，同时结合
 * {@link PatientContextHolder}（ThreadLocal + ConcurrentHashMap）做交叉校验。
 * 全部方法为同步方法，确保在 Spring AI 1.0.3 streaming 模式下可靠执行。
 */
@Slf4j
@Component
public class AppointmentTools {

    private final IRegistrationService registrationService;
    private final IScheduleService scheduleService;
    private final IDoctorService doctorService;

    public AppointmentTools(IRegistrationService registrationService,
                            IScheduleService scheduleService,
                            IDoctorService doctorService) {
        this.registrationService = registrationService;
        this.scheduleService = scheduleService;
        this.doctorService = doctorService;
    }

    /**
     * 解析患者ID。优先使用 LLM 传入的 patientId，
     * 其次通过 ThreadLocal / ConcurrentHashMap 兜底。
     * <p>
     * 不再依赖 ThreadLocal 中的 sessionId 做 Redis 反查——因为 Tool 在 boundedElastic
     * 线程执行，ThreadLocal 为空，拿不到 sessionId。
     */
    private Long resolvePatientId(Long llmPatientId) {
        // 1. LLM 传入的 patientId（最优先，来自系统提示词，跨线程可靠）
        if (llmPatientId != null && llmPatientId > 0) {
            log.info("通过 LLM 参数获取 patientId={}", llmPatientId);
            return llmPatientId;
        }

        // 2. ThreadLocal（同线程场景，备用）
        Long patientId = PatientContextHolder.getPatientId();
        if (patientId != null && patientId > 0) {
            log.info("通过 ThreadLocal 获取 patientId={}", patientId);
            return patientId;
        }

        // 3. ConcurrentHashMap 跨线程解析（不需要知道 sessionId）
        patientId = PatientContextHolder.resolveFromRegistry();
        if (patientId != null && patientId > 0) {
            log.info("通过 SessionRegistry 跨线程解析 patientId={}", patientId);
            return patientId;
        }

        log.error("无法获取 patientId：LLM参数={}, ThreadLocal和Registry均不可用", llmPatientId);
        return null;
    }

    /**
     * 查询当前患者的预约记录
     */
    @Tool(description = "查询当前患者的预约挂号记录，可按状态筛选：0-待就诊，1-已就诊，2-已取消")
    public String getMyAppointments(
            @ToolParam(description = "当前患者ID（从系统提示词中获取）") Long patientId,
            @ToolParam(description = "预约状态筛选，可选值：0-待就诊，1-已就诊，2-已取消，不传则查询全部") Integer status) {

        Long resolvedId = resolvePatientId(patientId);
        if (resolvedId == null) {
            return "无法获取当前患者信息，请确保已登录。如果问题持续，请刷新页面重新进入。";
        }

        log.info("AI调用工具: getMyAppointments, patientId={}, status={}", resolvedId, status);

        try {
            List<AppointmentVO> appointments = registrationService.getPatientAppointmentVOs(resolvedId, status);
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
    @Tool(description = "为当前患者创建预约挂号。patientId 从系统提示词获取，scheduleId 从排班查询结果获取。")
    public String createAppointment(
            @ToolParam(description = "当前患者ID（从系统提示词中获取）") Long patientId,
            @ToolParam(description = "排班ID，可通过查询医生排班获取") Long scheduleId) {

        if (scheduleId == null) {
            return "请提供排班ID";
        }

        Long resolvedId = resolvePatientId(patientId);
        if (resolvedId == null) {
            return "无法获取当前患者信息，请确保已登录。如果问题持续，请刷新页面重新进入。";
        }

        log.info("AI调用工具: createAppointment, patientId={}, scheduleId={}", resolvedId, scheduleId);

        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            return "排班不存在，请确认排班ID是否正确";
        }

        int remaining = schedule.getMaxPatients() - schedule.getCurrentPatients();
        if (remaining <= 0) {
            return "该排班已满，无法预约";
        }

        try {
            AppointmentVO vo = registrationService.createAppointmentVO(resolvedId, scheduleId, 0);
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
    @Tool(description = "取消当前患者的预约挂号。patientId 从系统提示词获取，appointmentId 从预约记录中获取。")
    public String cancelAppointment(
            @ToolParam(description = "当前患者ID（从系统提示词中获取）") Long patientId,
            @ToolParam(description = "要取消的预约ID") Long appointmentId) {

        if (appointmentId == null) {
            return "请提供要取消的预约ID";
        }

        Long resolvedId = resolvePatientId(patientId);
        if (resolvedId == null) {
            return "无法获取当前患者信息，请确保已登录。如果问题持续，请刷新页面重新进入。";
        }

        log.info("AI调用工具: cancelAppointment, patientId={}, appointmentId={}", resolvedId, appointmentId);

        try {
            boolean result = registrationService.cancelAppointment(appointmentId, resolvedId);
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
