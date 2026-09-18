package com.project.his.service.entity;

import com.project.his.domain.po.Prescription;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IPrescriptionService extends IService<Prescription> {

    List<Prescription> getByPatientId(Long patientId);

    List<Prescription> getByDoctorId(Long doctorId);

    List<Prescription> getByDiagnosisId(Long diagnosisId);

}
