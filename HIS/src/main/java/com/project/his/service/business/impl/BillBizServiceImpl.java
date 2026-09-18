package com.project.his.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.his.domain.dto.BillDTO;
import com.project.his.domain.po.*;
import com.project.his.domain.vo.BillItemVO;
import com.project.his.domain.vo.BillVO;
import com.project.his.domain.vo.InvoiceVO;
import com.project.his.exception.BusinessException;
import com.project.his.service.business.IBillBizService;
import com.project.his.service.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillBizServiceImpl implements IBillBizService {

    private final IBillService billService;
    private final IBillItemService billItemService;
    private final IPatientService patientService;
    private final IPrescriptionService prescriptionService;
    private final IPrescriptionDetailService prescriptionDetailService;
    private final IDrugService drugService;
    private final IAppointmentService appointmentService;
    private final IDoctorService doctorService;

    private static final BigDecimal REGISTRATION_FEE = new BigDecimal("20.00");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRegistrationBillItem(Long appointmentId, Long patientId) {
        Bill bill = findOrCreateBill(appointmentId, patientId);
        BillItem item = new BillItem();
        item.setBillId(bill.getBillId());
        item.setItemType("registration");
        item.setItemName("挂号费");
        item.setUnitPrice(REGISTRATION_FEE);
        item.setQuantity(1);
        item.setAmount(REGISTRATION_FEE);
        item.setRelatedId(appointmentId);
        billItemService.save(item);
        updateBillTotal(bill);
        log.info("生成挂号费账单: billId={}, appointmentId={}, amount={}", bill.getBillId(), appointmentId, REGISTRATION_FEE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDrugBillItems(Long prescriptionId, Long patientId) {
        Bill bill = findOrCreateBill(null, patientId);
        List<PrescriptionDetail> details = prescriptionDetailService.getByPrescriptionId(prescriptionId);
        for (PrescriptionDetail detail : details) {
            Drug drug = drugService.getById(detail.getDrugId());
            if (drug == null) continue;
            BigDecimal amount = drug.getUnitPrice().multiply(new BigDecimal(detail.getQuantity()));
            BillItem item = new BillItem();
            item.setBillId(bill.getBillId());
            item.setItemType("drug");
            item.setItemName(drug.getDrugName());
            item.setUnitPrice(drug.getUnitPrice());
            item.setQuantity(detail.getQuantity());
            item.setAmount(amount);
            item.setRelatedId(detail.getDetailId());
            billItemService.save(item);
        }
        updateBillTotal(bill);
        log.info("生成药品费账单: billId={}, prescriptionId={}, items={}", bill.getBillId(), prescriptionId, details.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExaminationBillItem(Long appointmentId, Long patientId, String examName, BigDecimal price) {
        Bill bill = findOrCreateBill(appointmentId, patientId);
        BillItem item = new BillItem();
        item.setBillId(bill.getBillId());
        item.setItemType("examination");
        item.setItemName(examName != null ? examName : "检查费");
        item.setUnitPrice(price);
        item.setQuantity(1);
        item.setAmount(price);
        item.setRelatedId(appointmentId);
        billItemService.save(item);
        updateBillTotal(bill);
        log.info("生成检查费账单: billId={}, examName={}, amount={}", bill.getBillId(), examName, price);
    }

    @Override
    public BillVO getBillDetail(Long billId) {
        Bill bill = billService.getById(billId);
        if (bill == null) throw new BusinessException("账单不存在");
        return buildBillVO(bill);
    }

    @Override
    public List<BillVO> getPatientBills(Long patientId) {
        List<Bill> bills = billService.getByPatientId(patientId);
        return bills.stream().map(this::buildBillVO).collect(Collectors.toList());
    }

    @Override
    public List<BillVO> getAllBills() {
        List<Bill> bills = billService.list(new LambdaQueryWrapper<Bill>().orderByDesc(Bill::getCreateTime));
        return bills.stream().map(this::buildBillVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payBill(Long billId, BillDTO dto) {
        Bill bill = billService.getById(billId);
        if (bill == null) throw new BusinessException("账单不存在");
        if (bill.getStatus() == 2) throw new BusinessException("账单已支付");
        bill.setStatus(2);
        bill.setPaidAmount(bill.getTotalAmount());
        bill.setPaymentMethod(dto.getPaymentMethod());
        bill.setPayTime(LocalDateTime.now());
        billService.updateById(bill);
        log.info("账单支付成功: billId={}, method={}, amount={}", billId, dto.getPaymentMethod(), bill.getTotalAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsPaid(Long billId, String paymentMethod) {
        Bill bill = billService.getById(billId);
        if (bill == null) throw new BusinessException("账单不存在");
        bill.setStatus(2);
        bill.setPaidAmount(bill.getTotalAmount());
        bill.setPaymentMethod(paymentMethod);
        bill.setPayTime(LocalDateTime.now());
        billService.updateById(bill);
    }

    @Override
    public InvoiceVO getInvoice(Long billId) {
        Bill bill = billService.getById(billId);
        if (bill == null) throw new BusinessException("账单不存在");
        List<BillItem> items = billItemService.getByBillId(billId);
        List<BillItemVO> itemVOs = items.stream().map(this::buildBillItemVO).collect(Collectors.toList());

        String patientName = "";
        Patient patient = patientService.getById(bill.getPatientId());
        if (patient != null) patientName = patient.getName();

        return InvoiceVO.builder()
                .billId(bill.getBillId())
                .patientName(patientName)
                .totalAmount(bill.getTotalAmount())
                .paymentMethod(bill.getPaymentMethod())
                .statusDesc(bill.getStatus() == 2 ? "已支付" : "未支付")
                .payTime(bill.getPayTime())
                .createTime(bill.getCreateTime())
                .items(itemVOs)
                .build();
    }

    private Bill findOrCreateBill(Long appointmentId, Long patientId) {
        // 有预约ID：按预约ID精确查找（一个预约一张账单）
        if (appointmentId != null) {
            LambdaQueryWrapper<Bill> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Bill::getAppointmentId, appointmentId);
            Bill bill = billService.getOne(queryWrapper);
            if (bill != null) return bill;
            bill = new Bill();
            bill.setAppointmentId(appointmentId);
            bill.setPatientId(patientId);
            bill.setTotalAmount(BigDecimal.ZERO);
            bill.setPaidAmount(BigDecimal.ZERO);
            bill.setStatus(0);
            billService.save(bill);
            return bill;
        }
        // 无预约ID：找患者最近未支付账单
        LambdaQueryWrapper<Bill> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Bill::getPatientId, patientId)
                .eq(Bill::getStatus, 0)
                .orderByDesc(Bill::getCreateTime)
                .last("LIMIT 1");
        Bill bill = billService.getOne(queryWrapper);
        if (bill == null) {
            bill = new Bill();
            bill.setPatientId(patientId);
            bill.setTotalAmount(BigDecimal.ZERO);
            bill.setPaidAmount(BigDecimal.ZERO);
            bill.setStatus(0);
            billService.save(bill);
        }
        return bill;
    }

    private void updateBillTotal(Bill bill) {
        List<BillItem> items = billItemService.getByBillId(bill.getBillId());
        BigDecimal total = items.stream()
                .map(BillItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        bill.setTotalAmount(total);
        billService.updateById(bill);
    }

    private BillVO buildBillVO(Bill bill) {
        List<BillItem> items = billItemService.getByBillId(bill.getBillId());
        List<BillItemVO> itemVOs = items.stream().map(this::buildBillItemVO).collect(Collectors.toList());
        BigDecimal unpaid = bill.getTotalAmount().subtract(bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO);

        String patientName = "";
        Patient patient = patientService.getById(bill.getPatientId());
        if (patient != null) patientName = patient.getName();

        String statusDesc;
        switch (bill.getStatus()) {
            case 0: statusDesc = "未支付"; break;
            case 1: statusDesc = "部分支付"; break;
            case 2: statusDesc = "已支付"; break;
            default: statusDesc = "未知";
        }

        return BillVO.builder()
                .billId(bill.getBillId())
                .appointmentId(bill.getAppointmentId())
                .patientId(bill.getPatientId())
                .patientName(patientName)
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .unpaidAmount(unpaid)
                .status(bill.getStatus())
                .statusDesc(statusDesc)
                .paymentMethod(bill.getPaymentMethod())
                .createTime(bill.getCreateTime())
                .payTime(bill.getPayTime())
                .items(itemVOs)
                .build();
    }

    private BillItemVO buildBillItemVO(BillItem item) {
        String typeDesc;
        switch (item.getItemType()) {
            case "registration": typeDesc = "挂号费"; break;
            case "consultation": typeDesc = "诊察费"; break;
            case "examination": typeDesc = "检查费"; break;
            case "drug": typeDesc = "药品费"; break;
            default: typeDesc = item.getItemType();
        }
        return BillItemVO.builder()
                .itemId(item.getItemId())
                .itemType(item.getItemType())
                .itemTypeDesc(typeDesc)
                .itemName(item.getItemName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .amount(item.getAmount())
                .relatedId(item.getRelatedId())
                .build();
    }
}
