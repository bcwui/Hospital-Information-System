package com.project.his.config.ai.tools;

import com.project.his.common.Constants;
import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.Schedule;
import com.project.his.domain.vo.AppointmentVO;
import com.project.his.service.business.IRegistrationService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IScheduleService;
import com.project.his.utils.redis.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 预约挂号工具（需要鉴权）
 * <p>
 * 患者ID获取方式：从 Reactor Context 中读取 sessionId → 通过 Redis 反查 patientId。
 * 彻底消除 ThreadLocal，线程安全且适配 WebFlux 响应式架构。
 */
@Slf4j
@Component
public class AppointmentTools {

    private final IRegistrationService registrationService;
    private final IScheduleService scheduleService;
    private final IDoctorService doctorService;
    private final IRedisService redisService;

    public AppointmentTools(@Lazy IRegistrationService registrationService,
                            IScheduleService scheduleService,
                            IDoctorService doctorService,
                            IRedisService redisService) {
        this.registrationService = registrationService;
        this.scheduleService = scheduleService;
        this.doctorService = doctorService;
        this.redisService = redisService;
    }

    /**
     * 从 Reactor Context 获取 sessionId → Redis 反查 patientId（双重保障）。
     * 优先走 Redis，Redis 不可用时用 Context 中直接存放的 patientId 兜底。
     */
    private Mono<Long> resolvePatientId() {
        return Mono.deferContextual(ctx -> {
            // 第一优先：通过 sessionId 从 Redis 反查
            if (ctx.hasKey(PatientContext.SESSION_ID_KEY)) {
                String sessionId = ctx.get(PatientContext.SESSION_ID_KEY);
                String redisKey = Constants.RedisKey.AI_CONSULT_SESSION + sessionId;
                RMap<String, Object> sessionMap = redisService.getMap(redisKey);
                if (sessionMap != null && !sessionMap.isEmpty()) {
                    Object pid = sessionMap.get("patientId");
                    if (pid != null) {
                        Long patientId = pid instanceof Number
                                ? ((Number) pid).longValue()
                                : Long.parseLong(pid.toString());
                        log.info("通过 Redis 反查 patientId={}, sessionId={}", patientId, sessionId);
                        return Mono.just(patientId);
                    }
                }
                log.warn("Redis 反查失败, sessionId={}, 回退到 Context 直接值", sessionId);
            }

            // 兜底：直接从 Context 读取（兼容 Spring AI 不传播 Context 的场景）
            if (ctx.hasKey(PatientContext.PATIENT_ID_KEY)) {
                Long patientId = ctx.get(PatientContext.PATIENT_ID_KEY);
                if (patientId != null && patientId > 0) {
                    log.info("通过 Context 兜底获取 patientId={}", patientId);
                    return Mono.just(patientId);
                }
            }

            log.error("无法获取 patientId：Context 中既无 sessionId 也无 patientId");
            return Mono.empty();
        });
    }

    /**
     * 查询当前患者的预约记录
     */
    @Tool(description = "查询当前患者的预约挂号记录，可按状态筛选：0-待就诊，1-已就诊，2-已取消")
    public Mono<String> getMyAppointments(
            @ToolParam(description = "预约状态筛选，可选值：0-待就诊，1-已就诊，2-已取消，不传则查询全部") Integer status) {

        return resolvePatientId()
                .flatMap(patientId -> {
                    log.info("AI调用工具: getMyAppointments, patientId={}, status={}", patientId, status);

                    try {
                        List<AppointmentVO> appointments = registrationService.getPatientAppointmentVOs(patientId, status);
                        if (appointments == null || appointments.isEmpty()) {
                            String statusDesc = getStatusDesc(status);
                            return Mono.just("您当前没有" + statusDesc + "的预约记录");
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
                        return Mono.just(sb.toString());
                    } catch (Exception e) {
                        log.error("查询预约记录失败", e);
                        return Mono.just("查询预约记录失败：" + e.getMessage());
                    }
                })
                .switchIfEmpty(Mono.just("无法获取当前患者信息，请确保已登录"));
    }

    /**
     * 为当前患者创建预约
     */
    @Tool(description = "为当前患者创建预约挂号，需要提供排班ID（患者身份自动识别）")
    public Mono<String> createAppointment(
            @ToolParam(description = "排班ID，可通过查询医生排班获取") Long scheduleId) {

        if (scheduleId == null) {
            return Mono.just("请提供排班ID");
        }

        return resolvePatientId()
                .flatMap(patientId -> {
                    log.info("AI调用工具: createAppointment, patientId={}, scheduleId={}", patientId, scheduleId);

                    Schedule schedule = scheduleService.getById(scheduleId);
                    if (schedule == null) {
                        return Mono.just("排班不存在，请确认排班ID是否正确");
                    }

                    int remaining = schedule.getMaxPatients() - schedule.getCurrentPatients();
                    if (remaining <= 0) {
                        return Mono.just("该排班已满，无法预约");
                    }

                    try {
                        AppointmentVO vo = registrationService.createAppointmentVO(patientId, scheduleId, 0);
                        Doctor doctor = doctorService.getById(schedule.getDoctorId());
                        String doctorName = doctor != null ? doctor.getName() : "未知医生";

                        return Mono.just("预约成功！\n" +
                                "预约ID: " + vo.getAppointmentId() + "\n" +
                                "医生: " + doctorName + "\n" +
                                "日期: " + vo.getAppointmentDate() + " " + vo.getTimeSlot() + "\n" +
                                "请按时就诊，如需取消请提前操作。");
                    } catch (Exception e) {
                        log.error("创建预约失败", e);
                        return Mono.just("预约失败：" + e.getMessage());
                    }
                })
                .switchIfEmpty(Mono.just("无法获取当前患者信息，请确保已登录"));
    }

    /**
     * 取消当前患者的预约
     */
    @Tool(description = "取消当前患者的预约挂号，需要提供预约ID（患者身份自动识别）")
    public Mono<String> cancelAppointment(
            @ToolParam(description = "要取消的预约ID") Long appointmentId) {

        if (appointmentId == null) {
            return Mono.just("请提供要取消的预约ID");
        }

        return resolvePatientId()
                .flatMap(patientId -> {
                    log.info("AI调用工具: cancelAppointment, patientId={}, appointmentId={}", patientId, appointmentId);

                    try {
                        boolean result = registrationService.cancelAppointment(appointmentId, patientId);
                        if (result) {
                            return Mono.just("预约已成功取消（预约ID: " + appointmentId + "）");
                        } else {
                            return Mono.just("取消预约失败，请确认预约ID是否正确，且预约状态为待就诊");
                        }
                    } catch (Exception e) {
                        log.error("取消预约失败", e);
                        return Mono.just("取消预约失败：" + e.getMessage());
                    }
                })
                .switchIfEmpty(Mono.just("无法获取当前患者信息，请确保已登录"));
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
