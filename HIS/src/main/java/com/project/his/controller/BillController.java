package com.project.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.project.his.common.Result;
import com.project.his.domain.dto.BillDTO;
import com.project.his.domain.po.Patient;
import com.project.his.domain.vo.BillVO;
import com.project.his.domain.vo.InvoiceVO;
import com.project.his.exception.BusinessException;
import com.project.his.service.business.IBillBizService;
import com.project.his.service.entity.IPatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BillController {

    private final IBillBizService billBizService;
    private final IPatientService patientService;

    // ========== Patient 接口 ==========

    @SaCheckRole("patient")
    @GetMapping("/patient/bills")
    public Result<List<BillVO>> getMyBills() {
        Long userId = StpUtil.getLoginIdAsLong();
        Patient patient = patientService.getByUserId(userId);
        if (patient == null) throw new BusinessException("患者信息不存在");
        return Result.success(billBizService.getPatientBills(patient.getPatientId()));
    }

    @SaCheckRole("patient")
    @GetMapping("/patient/bills/{billId}")
    public Result<BillVO> getBillDetail(@PathVariable Long billId) {
        return Result.success(billBizService.getBillDetail(billId));
    }

    @SaCheckRole("patient")
    @PostMapping("/patient/bills/{billId}/pay")
    public Result<Boolean> payBill(@PathVariable Long billId, @RequestBody BillDTO dto) {
        billBizService.payBill(billId, dto);
        return Result.success(true);
    }

    @SaCheckRole("patient")
    @GetMapping("/patient/bills/{billId}/invoice")
    public Result<InvoiceVO> getInvoice(@PathVariable Long billId) {
        return Result.success(billBizService.getInvoice(billId));
    }

    // ========== Admin 接口 ==========

    @SaCheckRole("admin")
    @GetMapping("/admin/bills")
    public Result<List<BillVO>> getAllBills(@RequestParam(required = false) Integer status) {
        if (status != null) {
            return Result.success(billBizService.getAllBills().stream()
                    .filter(b -> b.getStatus().equals(status))
                    .toList());
        }
        return Result.success(billBizService.getAllBills());
    }

    @SaCheckRole("admin")
    @GetMapping("/admin/bills/{billId}")
    public Result<BillVO> adminGetBillDetail(@PathVariable Long billId) {
        return Result.success(billBizService.getBillDetail(billId));
    }

    @SaCheckRole("admin")
    @PutMapping("/admin/bills/{billId}/mark-paid")
    public Result<Boolean> markAsPaid(@PathVariable Long billId, @RequestBody BillDTO dto) {
        billBizService.markAsPaid(billId, dto.getPaymentMethod());
        return Result.success(true);
    }
}
