import { DoAxiosWithErro } from "..";

export interface PrescriptionDetailDTO {
  drugId: number;
  quantity: number;
  dosage: string;
  usageMethod: string;
  frequency: string;
  days: number;
  remark?: string;
}

export interface PrescriptionSubmitDTO {
  diagnosisId?: number;
  patientId: number;
  items: PrescriptionDetailDTO[];
}

export interface PrescriptionDetailVO {
  detailId: number;
  drugId: number;
  drugName: string;
  specification: string;
  unit: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  dosage: string;
  usageMethod: string;
  frequency: string;
  days: number;
  remark: string;
}

export interface PrescriptionVO {
  prescriptionId: number;
  diagnosisId: number;
  doctorId: number;
  doctorName: string;
  patientId: number;
  patientName: string;
  status: number;
  statusDesc: string;
  totalAmount: number;
  details: PrescriptionDetailVO[];
  createTime: string;
}

export const searchDrugs = async (name?: string): Promise<DrugVO[]> => {
  try {
    const params: Record<string, unknown> = {};
    if (name) params.name = name;
    return await DoAxiosWithErro<DrugVO[]>("/doctor/drugs/search", "get", params, true, false) ?? [];
  } catch (err) {
    console.error("搜索药品失败:", err);
    throw err;
  }
};

export const submitPrescription = async (dto: PrescriptionSubmitDTO): Promise<PrescriptionVO> => {
  try {
    return await DoAxiosWithErro<PrescriptionVO>("/doctor/prescriptions", "post", dto as unknown as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("提交处方失败:", err);
    throw err;
  }
};

export const getPatientPrescriptions = async (patientId: number): Promise<PrescriptionVO[]> => {
  try {
    return await DoAxiosWithErro<PrescriptionVO[]>(`/doctor/prescriptions/patient/${patientId}`, "get", {}, true, false) ?? [];
  } catch (err) {
    console.error("获取患者处方失败:", err);
    throw err;
  }
};

export const getPrescriptionDetail = async (prescriptionId: number): Promise<PrescriptionVO> => {
  try {
    return await DoAxiosWithErro<PrescriptionVO>(`/doctor/prescriptions/${prescriptionId}`, "get", {}, true, false);
  } catch (err) {
    console.error("获取处方详情失败:", err);
    throw err;
  }
};

export const updatePrescriptionStatus = async (prescriptionId: number, status: number): Promise<boolean> => {
  try {
    return await DoAxiosWithErro<boolean>(`/doctor/prescriptions/${prescriptionId}/status`, "put", { status } as Record<string, unknown>, true, true);
  } catch (err) {
    console.error("更新处方状态失败:", err);
    throw err;
  }
};

export const getMyPrescriptions = async (): Promise<PrescriptionVO[]> => {
  try {
    return await DoAxiosWithErro<PrescriptionVO[]>("/patient/prescriptions", "get", {}, true, false) ?? [];
  } catch (err) {
    console.error("获取我的处方失败:", err);
    throw err;
  }
};

export const getMyPrescriptionDetail = async (prescriptionId: number): Promise<PrescriptionVO> => {
  try {
    return await DoAxiosWithErro<PrescriptionVO>(`/patient/prescriptions/${prescriptionId}`, "get", {}, true, false);
  } catch (err) {
    console.error("获取处方详情失败:", err);
    throw err;
  }
};

export interface PatientVO {
  patientId: number;
  userId: number;
  name: string;
  idCard: string;
  gender: number;
  age: number;
  region: string;
  address: string;
}

export const searchPatients = async (keyword: string): Promise<PatientVO[]> => {
  try {
    return await DoAxiosWithErro<PatientVO[]>("/doctor/patients/search", "get", { keyword } as Record<string, unknown>, true, false) ?? [];
  } catch (err) {
    console.error("搜索患者失败:", err);
    throw err;
  }
};

// Reuse DrugVO from admin/drug
export interface DrugVO {
  drugId: number;
  drugName: string;
  specification: string;
  manufacturer: string;
  unit: string;
  unitPrice: number;
  stockQuantity: number;
  description: string;
  stockStatus: string;
}
