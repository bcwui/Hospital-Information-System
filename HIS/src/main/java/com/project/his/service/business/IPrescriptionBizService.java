package com.project.his.service.business;

import com.project.his.domain.dto.PrescriptionSubmitDTO;
import com.project.his.domain.vo.PrescriptionVO;

import java.util.List;

public interface IPrescriptionBizService {

    PrescriptionVO submitPrescription(Long doctorId, PrescriptionSubmitDTO dto);

    PrescriptionVO getPrescriptionDetail(Long prescriptionId);

    List<PrescriptionVO> getPatientPrescriptions(Long patientId);

    List<PrescriptionVO> getDoctorPrescriptions(Long doctorId);

    void updateStatus(Long prescriptionId, Integer status);

}
