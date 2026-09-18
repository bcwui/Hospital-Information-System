package com.project.his.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.project.his.common.Result;
import com.project.his.domain.dto.DrugDTO;
import com.project.his.domain.dto.DrugStockDTO;
import com.project.his.domain.dto.PrescriptionSubmitDTO;
import com.project.his.domain.po.Doctor;
import com.project.his.domain.po.Drug;
import com.project.his.domain.po.Patient;
import com.project.his.domain.vo.DrugVO;
import com.project.his.domain.vo.PrescriptionVO;
import com.project.his.exception.BusinessException;
import com.project.his.service.business.IPrescriptionBizService;
import com.project.his.service.entity.IDoctorService;
import com.project.his.service.entity.IDrugService;
import com.project.his.service.entity.IPatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final IDrugService drugService;
    private final IPrescriptionBizService prescriptionBizService;
    private final IDoctorService doctorService;
    private final IPatientService patientService;

    // ========== Admin 药品管理 ==========

    @SaCheckRole("admin")
    @PostMapping("/admin/drugs")
    public Result<Drug> createDrug(@RequestBody DrugDTO dto) {
        Drug drug = new Drug();
        drug.setDrugName(dto.getDrugName());
        drug.setSpecification(dto.getSpecification());
        drug.setManufacturer(dto.getManufacturer());
        drug.setUnit(dto.getUnit());
        drug.setUnitPrice(dto.getUnitPrice());
        drug.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        drug.setDescription(dto.getDescription());
        drug.setIsActive(1);
        drugService.save(drug);
        return Result.success(drug);
    }

    @SaCheckRole("admin")
    @PutMapping("/admin/drugs")
    public Result<Drug> updateDrug(@RequestBody DrugDTO dto) {
        if (dto.getDrugId() == null) {
            return Result.error("药品ID不能为空");
        }
        Drug drug = drugService.getById(dto.getDrugId());
        if (drug == null) {
            return Result.error("药品不存在");
        }
        drug.setDrugName(dto.getDrugName());
        drug.setSpecification(dto.getSpecification());
        drug.setManufacturer(dto.getManufacturer());
        drug.setUnit(dto.getUnit());
        drug.setUnitPrice(dto.getUnitPrice());
        drug.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) {
            drug.setIsActive(dto.getIsActive());
        }
        drugService.updateById(drug);
        return Result.success(drug);
    }

    @SaCheckRole("admin")
    @DeleteMapping("/admin/drugs/{drugId}")
    public Result<Boolean> deleteDrug(@PathVariable Long drugId) {
        Drug drug = drugService.getById(drugId);
        if (drug == null) {
            return Result.error("药品不存在");
        }
        drug.setIsActive(0);
        drugService.updateById(drug);
        return Result.success(true);
    }

    @SaCheckRole("admin")
    @GetMapping("/admin/drugs")
    public Result<List<DrugVO>> listDrugs(@RequestParam(required = false) String name,
                                           @RequestParam(required = false) Integer isActive) {
        List<Drug> drugs = drugService.list();
        if (name != null && !name.trim().isEmpty()) {
            drugs = drugs.stream()
                    .filter(d -> d.getDrugName().toLowerCase().contains(name.trim().toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (isActive != null) {
            drugs = drugs.stream()
                    .filter(d -> d.getIsActive().equals(isActive))
                    .collect(Collectors.toList());
        }
        List<DrugVO> vos = drugs.stream().map(this::toDrugVO).collect(Collectors.toList());
        return Result.success(vos);
    }

    @SaCheckRole("admin")
    @GetMapping("/admin/drugs/{drugId}")
    public Result<DrugVO> getDrug(@PathVariable Long drugId) {
        Drug drug = drugService.getById(drugId);
        if (drug == null) {
            return Result.error("药品不存在");
        }
        return Result.success(toDrugVO(drug));
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/drugs/{drugId}/stock-in")
    public Result<DrugVO> stockIn(@PathVariable Long drugId, @RequestBody DrugStockDTO dto) {
        Drug drug = drugService.getById(drugId);
        if (drug == null) {
            return Result.error("药品不存在");
        }
        drug.setStockQuantity(drug.getStockQuantity() + dto.getQuantity());
        drugService.updateById(drug);
        log.info("药品入库: drugId={}, quantity={}, remark={}", drugId, dto.getQuantity(), dto.getRemark());
        return Result.success(toDrugVO(drug));
    }

    @SaCheckRole("admin")
    @PostMapping("/admin/drugs/{drugId}/stock-out")
    public Result<DrugVO> stockOut(@PathVariable Long drugId, @RequestBody DrugStockDTO dto) {
        Drug drug = drugService.getById(drugId);
        if (drug == null) {
            return Result.error("药品不存在");
        }
        if (drug.getStockQuantity() < dto.getQuantity()) {
            return Result.error("库存不足，当前库存: " + drug.getStockQuantity());
        }
        drug.setStockQuantity(drug.getStockQuantity() - dto.getQuantity());
        drugService.updateById(drug);
        log.info("药品出库: drugId={}, quantity={}, remark={}", drugId, dto.getQuantity(), dto.getRemark());
        return Result.success(toDrugVO(drug));
    }

    // ========== Doctor 处方管理 ==========

    @SaCheckRole("doctor")
    @GetMapping("/doctor/drugs/search")
    public Result<List<DrugVO>> searchDrugs(@RequestParam(required = false) String name) {
        List<Drug> drugs;
        if (name != null && !name.trim().isEmpty()) {
            drugs = drugService.searchByName(name);
        } else {
            drugs = drugService.getActiveDrugs();
        }
        return Result.success(drugs.stream().map(this::toDrugVO).collect(Collectors.toList()));
    }

    @SaCheckRole("doctor")
    @PostMapping("/doctor/prescriptions")
    public Result<PrescriptionVO> createPrescription(@RequestBody PrescriptionSubmitDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Doctor doctor = doctorService.getDoctorByUserId(userId);
        if (doctor == null) {
            throw new BusinessException("医生信息不存在");
        }
        return Result.success(prescriptionBizService.submitPrescription(doctor.getDoctorId(), dto));
    }

    @SaCheckRole("doctor")
    @GetMapping("/doctor/prescriptions/patient/{patientId}")
    public Result<List<PrescriptionVO>> getPatientPrescriptions(@PathVariable Long patientId) {
        return Result.success(prescriptionBizService.getPatientPrescriptions(patientId));
    }

    @SaCheckRole("doctor")
    @GetMapping("/doctor/prescriptions/{prescriptionId}")
    public Result<PrescriptionVO> getPrescriptionDetail(@PathVariable Long prescriptionId) {
        return Result.success(prescriptionBizService.getPrescriptionDetail(prescriptionId));
    }

    @SaCheckRole("doctor")
    @PutMapping("/doctor/prescriptions/{prescriptionId}/status")
    public Result<Boolean> updatePrescriptionStatus(@PathVariable Long prescriptionId, @RequestParam Integer status) {
        prescriptionBizService.updateStatus(prescriptionId, status);
        return Result.success(true);
    }

    // ========== Patient 查看处方 ==========

    @SaCheckRole("patient")
    @GetMapping("/patient/prescriptions")
    public Result<List<PrescriptionVO>> getMyPrescriptions() {
        Long userId = StpUtil.getLoginIdAsLong();
        Patient patient = patientService.getByUserId(userId);
        if (patient == null) {
            throw new BusinessException("患者信息不存在");
        }
        return Result.success(prescriptionBizService.getPatientPrescriptions(patient.getPatientId()));
    }

    @SaCheckRole("patient")
    @GetMapping("/patient/prescriptions/{prescriptionId}")
    public Result<PrescriptionVO> getMyPrescriptionDetail(@PathVariable Long prescriptionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Patient patient = patientService.getByUserId(userId);
        if (patient == null) {
            throw new BusinessException("患者信息不存在");
        }
        PrescriptionVO vo = prescriptionBizService.getPrescriptionDetail(prescriptionId);
        if (vo == null || !vo.getPatientId().equals(patient.getPatientId())) {
            throw new BusinessException("无权查看该处方");
        }
        return Result.success(vo);
    }

    // ========== Doctor 患者查询 ==========

    @SaCheckRole("doctor")
    @GetMapping("/doctor/patients/search")
    public Result<List<Patient>> searchPatients(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(List.of());
        }
        List<Patient> patients = patientService.list();
        String kw = keyword.trim().toLowerCase();
        return Result.success(patients.stream()
                .filter(p -> p.getName().toLowerCase().contains(kw)
                        || (p.getIdCard() != null && p.getIdCard().contains(kw)))
                .limit(20)
                .collect(Collectors.toList()));
    }

    private DrugVO toDrugVO(Drug d) {
        String stockStatus;
        if (d.getStockQuantity() == null || d.getStockQuantity() == 0) {
            stockStatus = "缺货";
        } else if (d.getStockQuantity() < 10) {
            stockStatus = "库存紧张";
        } else {
            stockStatus = "库存充足";
        }
        return DrugVO.builder()
                .drugId(d.getDrugId())
                .drugName(d.getDrugName())
                .specification(d.getSpecification())
                .manufacturer(d.getManufacturer())
                .unit(d.getUnit())
                .unitPrice(d.getUnitPrice())
                .stockQuantity(d.getStockQuantity())
                .description(d.getDescription())
                .isActive(d.getIsActive())
                .stockStatus(stockStatus)
                .createTime(d.getCreateTime())
                .build();
    }
}
