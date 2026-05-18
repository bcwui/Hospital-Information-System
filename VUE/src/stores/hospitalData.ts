import { reactive } from "vue";
import { defineStore, acceptHMRUpdate } from "pinia";
import type {
  getDepartment,
  getClinic,
  department,
  clinic,
  doctor,
  schedule,
} from "@/api/patient/registrations";
import {
  getDepartmentRegistrations,
  getClinicRegistrations,
  getDoctorRegistrations,
  getDoctorSchedule,
  searchClinic,
  searchDoctor,
} from "@/api/patient/registrations";
import {
  createDepartRegistration,
  updeteDepartRegistration,
  deleteDepartRegistration,
  createClinicRegistration,
  deleteClinicRegistration,
  updeteClinicRegistration,
  createDoctorRegistration,
  updateDoctorRegistration,
  deleteDoctorRegistration,
  createScheduleRegistration,
  deleteScheduleRegistration,
} from "@/api/admin/registrations";

function getCurrentDate(): {
  currentDate: string;
} {
  const currentDate = new Date();

  function formatDate(date: Date): string {
    const year = date.getFullYear();
    // 月份从 0 开始，需 +1 并补零（如 1 月 → "01"）
    const month = String(date.getMonth() + 1).padStart(2, "0");
    // 日期补零（如 5 号 → "05"）
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  return {
    currentDate: formatDate(currentDate),
  };
}

function getRoundedUpCurrentHour(): number {
  const now = new Date();
  const currentHour = now.getHours(); // 当前小时（0-23）
  const currentMinute = now.getMinutes(); // 当前分钟（0-59）

  // 分钟 > 0 时小时加 1，否则保持当前小时
  const roundedHour = currentMinute > 0 ? currentHour + 1 : currentHour;

  return roundedHour;
}

export const useHospitalStore = defineStore("hospital", () => {
  const departs: department[] = reactive([]);
  const clinics: clinic[] = reactive([]);
  const doctors = reactive<doctor[]>([]);
  const schedules: schedule[] = reactive([]);
  type ScheduleRefreshOptions = {
    title: string;
    startDate: string;
    endDate: string;
  };

  // 获取列表
  const getDepartments = async () => {
    departs.splice(0, departs.length);
    const newDepartments = await getDepartmentRegistrations();
    if (!newDepartments) {
      console.error("获取科室列表失败");
      return;
    }
    departs.splice(0, departs.length, ...newDepartments);
  };

  const getClinics = async (departmentId: string) => {
    clinics.splice(0, clinics.length);
    const newClinics = await getClinicRegistrations(departmentId);
    if (!newClinics) {
      console.error("获取门诊列表失败");
      return;
    }
    clinics.splice(0, clinics.length, ...newClinics);
  };

  const getDoctors = async (departmentId: string, clinicId: string) => {
    doctors.splice(0, doctors.length);
    const newDoctors = await getDoctorRegistrations(departmentId, clinicId);
    if (!newDoctors) {
      console.error("获取医生列表失败");
      return;
    }
    doctors.splice(0, doctors.length, ...newDoctors);
  };

  const getSchedules = async (
    doctorId: string,
    title: string,
    startDate: string,
    endDate: string
  ): Promise<boolean> => {
    schedules.splice(0, schedules.length);
    const scheduleList = await getDoctorSchedule(doctorId, title, startDate, endDate);
    if (!scheduleList) {
      console.error("获取医生排班失败");
      return false;
    }
    const toDay = getCurrentDate().currentDate;
    const nowHour = getRoundedUpCurrentHour();

    const newSchedules = scheduleList.filter((schedule: schedule) => {
      return (
        schedule.scheduleDate !== toDay ||
        (schedule.scheduleDate === toDay && Number(schedule.timeSlot.split("-")[1]) <= nowHour)
      );
    });
    schedules.splice(0, schedules.length, ...newSchedules);
    return true;
  };

  //搜索操作
  const searchForClinic = async (clinicName: string) => {
    const newClinics = await searchClinic(clinicName);
    if (!newClinics) {
      console.error("搜索门诊失败");
      return;
    }
    clinics.splice(0, clinics.length, ...newClinics);
  };

  const searchForDoctor = async (doctorName: string) => {
    const newDoctors = await searchDoctor(doctorName);
    if (!newDoctors) {
      console.error("搜索医生失败");
      return;
    }
    doctors.splice(0, doctors.length, ...newDoctors);
  };

  // 科室相关操作
  const createDepart = async (newName: string) => {
    const getDepart = await createDepartRegistration(newName);
    if (!getDepart) {
      console.error("创建科室失败");
      return;
    }
    await getDepartments();
    return getDepart;
  };

  const updateDepart = async (deptId: string, updetedDepartName: string) => {
    const getDepart = await updeteDepartRegistration(deptId, updetedDepartName);
    if (!getDepart) {
      console.error("更新科室失败");
      return;
    }
    await getDepartments();
    return getDepart;
  };

  const deleteDepart = async (deptId: string) => {
    const res = await deleteDepartRegistration(deptId);
    if (!res) {
      console.error("删除科室失败");
      return;
    }
    await getDepartments();
    return true;
  };
  // 门诊相关操作
  const createClinic = async (deptId: string, newName: string) => {
    const getClinic = await createClinicRegistration(deptId, newName);
    if (!getClinic) {
      console.error("创建门诊失败");
      return;
    }
    await getClinics(deptId);
    return getClinic;
  };

  const updateClinic = async (clinicId: string, updetedClinicName: string, deptId: string) => {
    const getClinic = await updeteClinicRegistration(deptId, clinicId, updetedClinicName);
    if (!getClinic) {
      console.error("更新门诊失败");
      return;
    }
    await getClinics(deptId);
    return getClinic;
  };

  const deleteClinic = async (clinicId: string, deptId: string) => {
    const res = await deleteClinicRegistration(clinicId);
    if (!res) {
      console.error("删除门诊失败");
      return;
    }
    await getClinics(deptId);
    return true;
  };
  // 医生相关操作
  const createDoctor = async (
    username: string,
    password: string,
    email: string,
    phone: string,
    deptId: string,
    clinicId: string,
    name: string,
    title: string,
    introduction: string,
    avatar: File,
    avatarUrl: string
  ) => {
    try {
      const res = await createDoctorRegistration(
        username,
        password,
        email,
        phone,
        clinicId,
        name,
        title,
        introduction,
        avatar
      );
      if (!res) {
        console.error("创建医生失败");
        return false;
      }

      // 不依赖创建接口返回体（可能只返回 doctorId），统一刷新列表。
      await getDoctors(deptId, clinicId);
      return true;
    } catch (e) {
      console.error("创建医生异常", e);
      return false;
    }
  };

  const updateDoctor = async (
    doctorId: string,
    userId: string,
    username: string,
    password: string,
    email: string,
    phone: string,
    deptId: string,
    clinicId: string,
    name: string,
    title: string,
    introduction: string,
    avatar?: File | null,
    avatarUrl?: string
  ) => {
    const msg = await updateDoctorRegistration(
      doctorId,
      userId,
      username,
      password,
      email,
      phone,
      clinicId,
      name,
      title,
      introduction,
      avatar
    );
    if (!msg) {
      console.error("更新医生失败");
      return;
    }
    await getDoctors(deptId, clinicId);
    return msg;
  };

  const deleteDoctor = async (doctorId: string, deptId: string, clinicId: string) => {
    const msg = await deleteDoctorRegistration(doctorId);
    if (!msg) {
      console.error("删除医生失败");
      return;
    }
    await getDoctors(deptId, clinicId);
    return msg;
  };

  // 排班相关操作
  const createSchedule = async (
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
    doctorAvatar: string,
    refresh: ScheduleRefreshOptions
  ) => {
    const getSchedule = await createScheduleRegistration(
      doctorId,
      clinicId,
      scheduleDate,
      timeSlot,
      maxPatients,
      currentPatients,
      status,
      doctorName,
      doctorTitle,
      doctorIntroduction,
      doctorAvatar
    );
    if (!getSchedule) {
      console.error("创建排班失败");
      return;
    }
    await getSchedules(doctorId, refresh.title, refresh.startDate, refresh.endDate);
    return true;
  };

  const deleteSchedule = async (
    scheduleId: string,
    doctorId: string,
    refresh: ScheduleRefreshOptions
  ) => {
    const res = await deleteScheduleRegistration(scheduleId);
    if (!res) {
      console.error("删除排班失败");
      return;
    }
    await getSchedules(doctorId, refresh.title, refresh.startDate, refresh.endDate);
    return true;
  };

  return {
    departs,
    clinics,
    doctors,
    schedules,
    getDepartments,
    getClinics,
    getDoctors,
    getSchedules,
    searchForClinic,
    searchForDoctor,
    createDepart,
    updateDepart,
    deleteDepart,
    createClinic,
    updateClinic,
    deleteClinic,
    createDoctor,
    updateDoctor,
    deleteDoctor,
    createSchedule,
    deleteSchedule,
  };
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useHospitalStore, import.meta.hot));
}
