package com.project.his.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionSubmitDTO {

    private Long diagnosisId;

    private Long patientId;

    private List<PrescriptionDetailDTO> items;

}
