package com.project.his.service.business;

import com.project.his.domain.dto.BillDTO;
import com.project.his.domain.vo.BillVO;
import com.project.his.domain.vo.InvoiceVO;

import java.util.List;

public interface IBillBizService {

    void createRegistrationBillItem(Long appointmentId, Long patientId);

    void createDrugBillItems(Long prescriptionId, Long patientId);

    void createExaminationBillItem(Long appointmentId, Long patientId, String examName, java.math.BigDecimal price);

    BillVO getBillDetail(Long billId);

    List<BillVO> getPatientBills(Long patientId);

    List<BillVO> getAllBills();

    void payBill(Long billId, BillDTO dto);

    void markAsPaid(Long billId, String paymentMethod);

    InvoiceVO getInvoice(Long billId);

}
