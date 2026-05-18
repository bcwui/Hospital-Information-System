import { DoAxiosWithErro } from "..";
import type { getDepartment, getClinic, doctor, schedule } from "@/api/patient/registrations";
//科室相关操作
export const createDepartRegistration = async (deptName: string) => {
  try {
    const res = await DoAxiosWithErro<getDepartment>(
      "/admin/department",
      "post",
      { deptName },
      true,
      true
    );

    return res;
  } catch (err) {
    console.error(err);
  }
};

export const updeteDepartRegistration = async (deptId: string, updetedDepartName: string) => {
  try {
    const res = await DoAxiosWithErro<getDepartment>(
      "/admin/department",
      "put",
      { deptId, deptName: updetedDepartName },
      true,
      true
    );

    return res;
  } catch (err) {
    console.error(err);
  }
};

export const deleteDepartRegistration = async (deptId: string) => {
  try {
    const res = await DoAxiosWithErro<boolean>(
      `/admin/department/${deptId}`,
      "delete",
      {},
      true,
      false
    );

    return res;
  } catch (err) {
    console.error(err);
  }
};

// 门诊相关操作
export const createClinicRegistration = async (deptId: string, clinicName: string) => {
  try {
    const res = await DoAxiosWithErro<getClinic>(
      "/admin/clinic",
      "post",
      { deptId, clinicName },
      true,
      true
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

export const updeteClinicRegistration = async (
  deptId: string,
  clinicId: string,
  updetedClinicName: string
) => {
  try {
    const res = await DoAxiosWithErro<getClinic>(
      "/admin/clinic",
      "put",
      { deptId, clinicId, clinicName: updetedClinicName },
      true,
      true
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

export const deleteClinicRegistration = async (clinicId: string) => {
  try {
    const res = await DoAxiosWithErro<boolean>(
      `/admin/clinic/${clinicId}`,
      "delete",
      {},
      true,
      false
    );

    return res;
  } catch (err) {
    console.error(err);
  }
};

// 医生相关操作
export const createDoctorRegistration = async (
  username: string,
  password: string,
  email: string,
  phone: string,
  clinicId: string,
  name: string,
  title: string,
  introduction: string,
  avatarFile: File
) => {
  try {
    // 构建 doctorInfo JSON 对象
    const doctorInfo = {
      username,
      password,
      email,
      phone,
      clinicId,
      name,
      title,
      introduction
    };

    const formData = new FormData();
    formData.append("doctorInfo", new Blob([JSON.stringify(doctorInfo)], { type: "application/json" }));
    if (avatarFile) {
      formData.append("avatarFile", avatarFile);
    }

    const res = await DoAxiosWithErro<doctor | string | number>(
      "/admin/doctor",
      "post",
      formData,
      true,
      true
    );

    return res;
  } catch (err) {
    console.error(err);
  }
};

export const updateDoctorRegistration = async (
  doctorId: string,
  userId: string,
  username: string,
  password: string,
  email: string,
  phone: string,
  clinicId: string,
  name: string,
  title: string,
  introduction: string,
  avatarFile?: File | null
) => {
  try {
    // 构建 doctorInfo JSON 对象
    const doctorInfo: Record<string, string> = {
      doctorId,
      userId,
      clinicId,
      name,
      title,
      introduction
    };
    if (username) doctorInfo.username = username;
    if (password) doctorInfo.password = password;
    if (email) doctorInfo.email = email;
    if (phone) doctorInfo.phone = phone;

    const formData = new FormData();
    formData.append("doctorInfo", new Blob([JSON.stringify(doctorInfo)], { type: "application/json" }));
    if (avatarFile) {
      formData.append("avatarFile", avatarFile);
    }

    const res = await DoAxiosWithErro<doctor>(
      "/admin/doctor",
      "put",
      formData,
      true,
      true
    );

    return res;
  } catch (err) {
    console.error(err);
  }
};

export const deleteDoctorRegistration = async (doctorId: string) => {
  try {
    const res = await DoAxiosWithErro<boolean>(
      `/admin/doctor/${doctorId}`,
      "delete",
      {},
      true,
      false
    );

    return res;
  } catch (err) {
    console.error(err);
  }
};

// 排班相关操作
export const createScheduleRegistration = async (
  doctorId: string,
  clinicId: string,
  scheduleDate: string,
  timeSlot: string,
  maxPatients: number,
  currentPatients: number,
  status: number,
  doctorName: string,
  doctorTitle: string,
  doctorIntroduction: string,
  doctorAvatar: string
) => {
  try {
    const res = await DoAxiosWithErro<Schedule>(
      "/admin/schedule",
      "post",
      { doctorId, clinicId, scheduleDate, timeSlot, maxPatients, currentPatients, status },
      true,
      true
    );
    const scheduleData = res as Schedule;
    const newSchedule: schedule = {
      scheduleId: String(scheduleData.scheduleId),
      doctorId,
      doctorName,
      doctorTitle,
      doctorIntroduction,
      doctorAvatar,
      scheduleDate,
      timeSlot,
      remainingQuota: maxPatients - currentPatients,
      canBook: maxPatients - currentPatients > 0,
    };
    return newSchedule;
  } catch (err) {
    console.error(err);
  }
};

interface Schedule {
  scheduleId: number;
}

export const deleteScheduleRegistration = async (scheduleId: string) => {
  try {
    const res = await DoAxiosWithErro<boolean>(
      `/admin/schedule/${scheduleId}`,
      "delete",
      {},
      true,
      false
    );
    return res;
  } catch (err) {
    console.error(err);
  }
};

// 自动排班操作
export const autoUpdateSchedules = async (
  startDate: string,
  endDate: string,
  clinicId?: string,
  deptId?: string
) => {
  try {
    const payload: Record<string, string | undefined> = {
      startDate,
      endDate,
    };
    // 只添加非空的可选参数
    if (clinicId) {
      payload.clinicId = clinicId;
    }
    if (deptId) {
      payload.deptId = deptId;
    }
    const res = await DoAxiosWithErro("/admin/schedule/auto", "post", payload, true, true);
    return res;
  } catch (err) {
    console.error("自动更新排班失败:", err);
    throw err;
  }
};
