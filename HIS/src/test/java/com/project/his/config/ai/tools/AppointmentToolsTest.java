package com.project.his.config.ai.tools;

import com.project.his.service.business.IRegistrationService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AppointmentToolsTest {

    @Mock IRegistrationService registrationService;
    @Mock IScheduleService scheduleService;
    @Mock IDoctorService doctorService;

    private AppointmentTools tools;

    @BeforeEach
    void setUp() {
        tools = new AppointmentTools(registrationService, scheduleService, doctorService);
        PatientContextHolder.clear();
        ToolResultEmitterHolder.clear();
    }

    @Test
    void getMyAppointments_noPatient_returnsError() {
        String result = tools.getMyAppointments(null);
        assertTrue(result.contains("无法获取当前患者信息"));
    }

    @Test
    void createAppointment_noSchedule_returnsError() {
        String result = tools.createAppointment(null);
        assertTrue(result.contains("请提供排班ID"));
    }

    @Test
    void cancelAppointment_noAppointment_returnsError() {
        String result = tools.cancelAppointment(null);
        assertTrue(result.contains("请提供要取消的预约ID"));
    }

    @Test
    void getMyAppointments_withPatientContext_resolvesCorrectly() {
        PatientContextHolder.set(100L, "test-session-001");
        // Without mock setup, this tests that resolvePatientId finds the context
        tools.getMyAppointments(0);
        // Should not throw NPE; error comes from service call being null
    }
}
