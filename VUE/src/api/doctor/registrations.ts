import { DoAxiosWithErro } from "..";
export interface Registration {
  doctorId: string;
  appointmentId: string;
  scheduleId: string;
  patientName: string;
  patientId: string;
  isRevisit: number;
  status: number;
  description: string;
  appointmentDate: string;
  deptName: string;
  clinicName: string;
  doctorName: string;
  statusDesc: string;
}

export interface detailRegistration extends Registration {
  aiConsultSessionId: string;
}

// 获取挂号信息
export const doctorGetRegistrations = async (doctorId: string) => {
  try {
    const res = await DoAxiosWithErro<Registration[]>(
      `/appointment/doctor/${doctorId}`,
      "get",
      {},
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

export const doctorGetDetailRegistration = async (doctorId: string, appointmentId: string) => {
  try {
    const res = await DoAxiosWithErro<detailRegistration>(
      `/appointment/doctor/${doctorId}/appointment/${appointmentId}`,
      "get",
      {},
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

// 操作挂号
export const creatRegistrationDiagnoses = async (
  appointmentId: string,
  doctorId: string,
  patientId: string,
  diagnosisResult: string,
  examination: string,
  prescription: string,
  advice: string
) => {
  try {
    const res = await DoAxiosWithErro(
      "/medical/diagnoses",
      "post",
      {
        appointmentId,
        doctorId,
        patientId,
        diagnosisResult,
        examination,
        prescription,
        advice,
      },
      true,
      true
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};
