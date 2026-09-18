package com.project.his.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.project.his.domain.po.Appointment;
import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.Patient;
import com.project.his.domain.po.Schedule;
import com.project.his.domain.po.User;
import com.project.his.service.business.INotificationBizService;
import com.project.his.service.entity.IAppointmentService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IPatientService;
import com.project.his.service.entity.IScheduleService;
import com.project.his.service.entity.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderTask {

    private final IAppointmentService appointmentService;
    private final IPatientService patientService;
    private final IDoctorService doctorService;
    private final IUserService userService;
    private final IScheduleService scheduleService;
    private final INotificationBizService notificationBizService;

    /** 每天凌晨1点，将过期未就诊的预约自动取消并释放排班名额 */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cancelOverdueAppointments() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始扫描过期预约: date <= {}", yesterday);

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.le(Appointment::getAppointmentDate, yesterday)
                .eq(Appointment::getStatus, 0);
        List<Appointment> overdueAppointments = appointmentService.list(queryWrapper);

        if (overdueAppointments.isEmpty()) {
            log.info("无过期预约需要处理");
            return;
        }

        int cancelled = 0;
        for (Appointment apt : overdueAppointments) {
            try {
                // 更新预约状态为已取消
                apt.setStatus(2);
                appointmentService.updateById(apt);

                // 释放排班名额
                if (apt.getScheduleId() != null) {
                    LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(Schedule::getScheduleId, apt.getScheduleId())
                            .gt(Schedule::getCurrentPatients, 0)
                            .setSql("current_patients = current_patients - 1");
                    scheduleService.update(updateWrapper);
                }

                // 通知患者
                Patient patient = patientService.getById(apt.getPatientId());
                if (patient != null) {
                    User user = userService.getById(patient.getUserId());
                    if (user != null) {
                        String doctorName = "";
                        Doctor doctor = doctorService.getById(apt.getDoctorId());
                        if (doctor != null) doctorName = doctor.getName();
                        notificationBizService.sendAppointmentNotification(
                                user.getId(), "预约已取消",
                                "您" + apt.getAppointmentDate() + "预约的" + doctorName
                                        + "医生（" + apt.getTimeSlot() + "）因超时未就诊已自动取消。",
                                apt.getAppointmentId());
                    }
                }
                cancelled++;
                log.info("过期预约已自动取消: appointmentId={}, date={}, timeSlot={}",
                        apt.getAppointmentId(), apt.getAppointmentDate(), apt.getTimeSlot());
            } catch (Exception e) {
                log.warn("取消过期预约失败: appointmentId={}, error={}",
                        apt.getAppointmentId(), e.getMessage());
            }
        }
        log.info("过期预约处理完成: 总数={}, 已取消={}", overdueAppointments.size(), cancelled);
    }

    @Scheduled(cron = "0 0 18 * * ?")
    public void sendAppointmentReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("开始发送次日预约提醒: date={}", tomorrow);

        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getAppointmentDate, tomorrow)
                .eq(Appointment::getStatus, 0);
        List<Appointment> appointments = appointmentService.list(queryWrapper);

        for (Appointment apt : appointments) {
            try {
                Patient patient = patientService.getById(apt.getPatientId());
                Doctor doctor = doctorService.getById(apt.getDoctorId());
                if (patient == null) continue;
                User user = userService.getById(patient.getUserId());
                if (user == null) continue;

                String doctorName = doctor != null ? doctor.getName() : "医生";
                String title = "预约提醒";
                String content = "您明天(" + tomorrow + ")有一个预约，医生：" + doctorName
                        + "，时间段：" + apt.getTimeSlot() + "，请按时就诊。";
                notificationBizService.sendAppointmentNotification(user.getId(), title, content, apt.getAppointmentId());
                log.info("预约提醒已发送: userId={}, appointmentId={}", user.getId(), apt.getAppointmentId());
            } catch (Exception e) {
                log.warn("发送预约提醒失败: appointmentId={}, error={}", apt.getAppointmentId(), e.getMessage());
            }
        }
        log.info("次日预约提醒发送完成: count={}", appointments.size());
    }
}
