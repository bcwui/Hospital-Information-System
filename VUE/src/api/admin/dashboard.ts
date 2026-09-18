import { DoAxiosWithErro } from "..";

export interface DashboardTodayVO {
  appointmentCount: number;
  revenue: number;
  newPatients: number;
  aiConsultCount: number;
}

export interface DashboardTrendVO {
  date: string;
  count: number;
  amount: number;
}

export interface DashboardDeptVO {
  deptName: string;
  count: number;
}

export interface DashboardDoctorWorkloadVO {
  doctorName: string;
  count: number;
}

export interface DashboardAIVO {
  totalSessions: number;
  todaySessions: number;
  avgMessagesPerSession: number;
}

export const getTodayStats = async (): Promise<DashboardTodayVO> => {
  return await DoAxiosWithErro<DashboardTodayVO>("/admin/statistics/today", "get", {}, true, false);
};

export const getAppointmentTrend = async (days = 7): Promise<DashboardTrendVO[]> => {
  return await DoAxiosWithErro<DashboardTrendVO[]>("/admin/statistics/appointment-trend", "get", { days } as Record<string, unknown>, true, false) ?? [];
};

export const getRevenueTrend = async (days = 7): Promise<DashboardTrendVO[]> => {
  return await DoAxiosWithErro<DashboardTrendVO[]>("/admin/statistics/revenue-trend", "get", { days } as Record<string, unknown>, true, false) ?? [];
};

export const getDeptDistribution = async (days = 30): Promise<DashboardDeptVO[]> => {
  return await DoAxiosWithErro<DashboardDeptVO[]>("/admin/statistics/dept-distribution", "get", { days } as Record<string, unknown>, true, false) ?? [];
};

export const getDoctorWorkload = async (days = 30, limit = 10): Promise<DashboardDoctorWorkloadVO[]> => {
  return await DoAxiosWithErro<DashboardDoctorWorkloadVO[]>("/admin/statistics/doctor-workload", "get", { days, limit } as Record<string, unknown>, true, false) ?? [];
};

export const getAIStats = async (): Promise<DashboardAIVO> => {
  return await DoAxiosWithErro<DashboardAIVO>("/admin/statistics/ai-stats", "get", {}, true, false);
};
