import { DoAxiosWithErro } from "..";

// 预约记录接口
export interface AppointmentVO {
  appointmentId: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  doctorTitle: string;
  clinicName: string;
  deptName: string;
  appointmentDate: string;
  timeSlot: string;
  status: number; // 0-待就诊，1-已就诊，2-已取消
  canCancel: boolean;
  createTime: string;
}

// 获取患者的预约记录
export const getPatientAppointments = async (
  patientId: string,
  status?: number
): Promise<AppointmentVO[] | undefined> => {
  try {
    const params: Record<string, unknown> = {};
    if (status !== undefined) {
      params.status = status;
    }
    const res = await DoAxiosWithErro<AppointmentVO[]>(
      `/appointment/patient/${patientId}`,
      "get",
      params,
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

// 获取数据接口
export interface getDepartment {
  deptId: string;
  deptName: string;
  isActive: number;
}

export interface getClinic {
  clinicId: string;
  clinicName: string;
  isActive: number;
}

// 传出数据接口
export interface department {
  name: string;
  state: number;
  id: string;
}

export interface clinic {
  name: string;
  state: number;
  id: string;
}

export interface doctor {
  doctorId: string;
  userId: string;
  name: string;
  title: string;

  introduction: string;

  avatar: string;

  deptName: string;
}

export interface schedule {
  scheduleId: string;
  doctorId: string;
  doctorName: string;
  doctorTitle: string;
  doctorIntroduction: string;
  doctorAvatar: string;
  scheduleDate: string;
  timeSlot: string;
  remainingQuota: number;
  canBook: boolean;
}

// 加载页面函数
export const getDepartmentRegistrations = async (): Promise<department[] | undefined> => {
  try {
    const res = await DoAxiosWithErro<getDepartment[]>(
      "/appointment/departments",
      "get",
      {},
      true,
      false
    );
    const departmentsData = res.map((department) => {
      return {
        name: department.deptName,
        state: department.isActive,
        id: department.deptId,
      };
    });
    return departmentsData;
  } catch (err) {
    console.error(err);
  }
};

export const getClinicRegistrations = async (
  departmentId: string
): Promise<clinic[] | undefined> => {
  try {
    const res = await DoAxiosWithErro<getClinic[]>(
      "/appointment/clinics",
      "get",
      { deptId: departmentId },
      true,
      false
    );
    const clinicsData = res.map((clinic) => {
      return {
        name: clinic.clinicName,
        state: clinic.isActive,
        id: clinic.clinicId,
      };
    });
    return clinicsData;
  } catch (err) {
    console.error(err);
  }
};

export const getDoctorRegistrations = async (
  departmentId: string,
  clinicId: string
): Promise<doctor[] | undefined> => {
  try {
    const res = await DoAxiosWithErro<doctor[]>(
      "/appointment/doctors",
      "get",
      { deptId: departmentId, clinicId: clinicId },
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};
export const getDoctorSchedule = async (
  doctorId: string,
  title: string,
  startDate: string,
  endDate: string
): Promise<schedule[] | undefined> => {
  try {
    const res = await DoAxiosWithErro<schedule[]>(
      "/appointment/doctor-schedules",
      "post",
      { doctorId, title, startDate, endDate },
      true,
      true
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

//挂号相关函数
export const createRegistrations = async (patientId: string, scheduleId: string) => {
  try {
    const res = await DoAxiosWithErro(
      `/appointment/create?patientId=${patientId}&scheduleId=${scheduleId}&isRevisit=0`,
      "post",
      {},
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

export const cancelRegistrations = async (appointmentId: string, patientId: string) => {
  // 后端使用 @RequestParam，需要通过 URL 查询参数传递
  const res = await DoAxiosWithErro(
    `/appointment/cancel?appointmentId=${appointmentId}&patientId=${patientId}`,
    "post",
    {},
    true,
    false
  );
  return res;
};

// 搜索相关函数
export const searchClinic = async (name: string): Promise<clinic[] | undefined> => {
  try {
    const res = await DoAxiosWithErro<getClinic[]>(
      "/appointment/clinics/search",
      "get",
      { name },
      true,
      false
    );
    const clinicsData = res.map((clinic) => {
      return {
        name: clinic.clinicName,
        state: clinic.isActive,
        id: clinic.clinicId,
      };
    });
    return clinicsData;
  } catch (err) {
    console.error(err);
  }
};

export const searchDoctor = async (name: string): Promise<doctor[] | undefined> => {
  try {
    const res = await DoAxiosWithErro<doctor[]>(
      "/appointment/doctors",
      "get",
      { name },
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};
