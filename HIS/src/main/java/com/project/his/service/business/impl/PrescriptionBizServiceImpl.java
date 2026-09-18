package com.project.his.service.business.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.project.his.domain.dto.PrescriptionDetailDTO;
import com.project.his.domain.dto.PrescriptionSubmitDTO;
import com.project.his.domain.po.*;
import com.project.his.domain.vo.PrescriptionDetailVO;
import com.project.his.domain.vo.PrescriptionVO;
import com.project.his.exception.BusinessException;
import com.project.his.service.business.IPrescriptionBizService;
import com.project.his.service.business.IBillBizService;
import com.project.his.service.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionBizServiceImpl implements IPrescriptionBizService {

    private final IPrescriptionService prescriptionService;
    private final IPrescriptionDetailService prescriptionDetailService;
    private final IDrugService drugService;
    private final IDoctorService doctorService;
    private final IPatientService patientService;
    private final IBillBizService billBizService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVO submitPrescription(Long doctorId, PrescriptionSubmitDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("处方明细不能为空");
        }
        if (dto.getPatientId() == null) {
            throw new BusinessException("患者ID不能为空");
        }

        // 创建处方主记录
        Prescription prescription = new Prescription();
        prescription.setDiagnosisId(dto.getDiagnosisId());
        prescription.setDoctorId(doctorId);
        prescription.setPatientId(dto.getPatientId());
        prescription.setStatus(1); // 直接提交
        prescriptionService.save(prescription);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PrescriptionDetailVO> detailVOs = new ArrayList<>();

        for (PrescriptionDetailDTO item : dto.getItems()) {
            Drug drug = drugService.getById(item.getDrugId());
            if (drug == null || drug.getIsActive() != 1) {
                throw new BusinessException("药品不存在或已下架: drugId=" + item.getDrugId());
            }
            if (drug.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException("药品库存不足: " + drug.getDrugName()
                        + ", 当前库存=" + drug.getStockQuantity() + ", 需要=" + item.getQuantity());
            }

            // 扣减库存
            LambdaUpdateWrapper<Drug> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Drug::getDrugId, drug.getDrugId())
                    .setSql("stock_quantity = stock_quantity - " + item.getQuantity());
            drugService.update(updateWrapper);

            // 创建明细
            PrescriptionDetail detail = new PrescriptionDetail();
            detail.setPrescriptionId(prescription.getPrescriptionId());
            detail.setDrugId(item.getDrugId());
            detail.setUnitPrice(drug.getUnitPrice());
            detail.setQuantity(item.getQuantity());
            detail.setDosage(item.getDosage());
            detail.setUsageMethod(item.getUsageMethod());
            detail.setFrequency(item.getFrequency());
            detail.setDays(item.getDays());
            detail.setRemark(item.getRemark());
            prescriptionDetailService.save(detail);

            BigDecimal subtotal = drug.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            detailVOs.add(PrescriptionDetailVO.builder()
                    .detailId(detail.getDetailId())
                    .drugId(drug.getDrugId())
                    .drugName(drug.getDrugName())
                    .specification(drug.getSpecification())
                    .unit(drug.getUnit())
                    .unitPrice(drug.getUnitPrice())
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .dosage(item.getDosage())
                    .usageMethod(item.getUsageMethod())
                    .frequency(item.getFrequency())
                    .days(item.getDays())
                    .remark(item.getRemark())
                    .build());
        }

        log.info("处方创建成功: prescriptionId={}, doctorId={}, patientId={}, items={}, total={}",
                prescription.getPrescriptionId(), doctorId, dto.getPatientId(), detailVOs.size(), totalAmount);

        // 自动生成药品费账单
        try {
            billBizService.createDrugBillItems(prescription.getPrescriptionId(), dto.getPatientId());
        } catch (Exception e) {
            log.warn("生成药品费账单失败: prescriptionId={}, error={}", prescription.getPrescriptionId(), e.getMessage());
        }

        return buildPrescriptionVO(prescription, detailVOs, totalAmount);
    }

    @Override
    public PrescriptionVO getPrescriptionDetail(Long prescriptionId) {
        Prescription prescription = prescriptionService.getById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        List<PrescriptionDetail> details = prescriptionDetailService.getByPrescriptionId(prescriptionId);
        List<PrescriptionDetailVO> detailVOs = buildDetailVOs(details);
        BigDecimal totalAmount = detailVOs.stream()
                .map(PrescriptionDetailVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return buildPrescriptionVO(prescription, detailVOs, totalAmount);
    }

    @Override
    public List<PrescriptionVO> getPatientPrescriptions(Long patientId) {
        List<Prescription> prescriptions = prescriptionService.getByPatientId(patientId);
        return prescriptions.stream().map(p -> {
            List<PrescriptionDetail> details = prescriptionDetailService.getByPrescriptionId(p.getPrescriptionId());
            List<PrescriptionDetailVO> detailVOs = buildDetailVOs(details);
            BigDecimal totalAmount = detailVOs.stream()
                    .map(PrescriptionDetailVO::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return buildPrescriptionVO(p, detailVOs, totalAmount);
        }).collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionVO> getDoctorPrescriptions(Long doctorId) {
        List<Prescription> prescriptions = prescriptionService.getByDoctorId(doctorId);
        return prescriptions.stream().map(p -> {
            List<PrescriptionDetail> details = prescriptionDetailService.getByPrescriptionId(p.getPrescriptionId());
            List<PrescriptionDetailVO> detailVOs = buildDetailVOs(details);
            BigDecimal totalAmount = detailVOs.stream()
                    .map(PrescriptionDetailVO::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return buildPrescriptionVO(p, detailVOs, totalAmount);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long prescriptionId, Integer status) {
        Prescription prescription = prescriptionService.getById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException("处方不存在");
        }
        prescription.setStatus(status);
        prescription.setUpdateTime(LocalDateTime.now());
        prescriptionService.updateById(prescription);
    }

    private PrescriptionVO buildPrescriptionVO(Prescription p, List<PrescriptionDetailVO> detailVOs, BigDecimal totalAmount) {
        String doctorName = "";
        Doctor doctor = doctorService.getById(p.getDoctorId());
        if (doctor != null) {
            doctorName = doctor.getName();
        }
        String patientName = "";
        Patient patient = patientService.getById(p.getPatientId());
        if (patient != null) {
            patientName = patient.getName();
        }
        String statusDesc;
        switch (p.getStatus()) {
            case 0: statusDesc = "草稿"; break;
            case 1: statusDesc = "已提交"; break;
            case 2: statusDesc = "已发药"; break;
            default: statusDesc = "未知";
        }
        return PrescriptionVO.builder()
                .prescriptionId(p.getPrescriptionId())
                .diagnosisId(p.getDiagnosisId())
                .doctorId(p.getDoctorId())
                .doctorName(doctorName)
                .patientId(p.getPatientId())
                .patientName(patientName)
                .status(p.getStatus())
                .statusDesc(statusDesc)
                .totalAmount(totalAmount)
                .details(detailVOs)
                .createTime(p.getCreateTime())
                .build();
    }

    private List<PrescriptionDetailVO> buildDetailVOs(List<PrescriptionDetail> details) {
        return details.stream().map(d -> {
            Drug drug = drugService.getById(d.getDrugId());
            String drugName = drug != null ? drug.getDrugName() : "";
            String specification = drug != null ? drug.getSpecification() : "";
            String unit = drug != null ? drug.getUnit() : "";
            // 优先使用处方明细中记录的开方时单价，保证历史处方金额不变
            BigDecimal unitPrice = d.getUnitPrice() != null ? d.getUnitPrice()
                    : drug != null ? drug.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unitPrice.multiply(new BigDecimal(d.getQuantity()));
            return PrescriptionDetailVO.builder()
                    .detailId(d.getDetailId())
                    .drugId(d.getDrugId())
                    .drugName(drugName)
                    .specification(specification)
                    .unit(unit)
                    .unitPrice(unitPrice)
                    .quantity(d.getQuantity())
                    .subtotal(subtotal)
                    .dosage(d.getDosage())
                    .usageMethod(d.getUsageMethod())
                    .frequency(d.getFrequency())
                    .days(d.getDays())
                    .remark(d.getRemark())
                    .build();
        }).collect(Collectors.toList());
    }
}
