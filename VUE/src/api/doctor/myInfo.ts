import myApi from "..";

export interface DoctorProfile {
  doctorId: number;
  userId: number;
  name: string;
  clinicId: number;
  deptName: string;
  title: string;
  introduction: string;
  avatar: string;
}

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
  timestamp: number;
};

function buildAuthHeaders(): Record<string, string> {
  const tokenValue = localStorage.getItem("userToken") || "";
  if (!tokenValue) return {};
  return {
    satoken: tokenValue,
    Authorization: `Bearer ${tokenValue}`,
  };
}

export const getCurrentDoctorInfo = async (): Promise<DoctorProfile | undefined> => {
  try {
    const resp = await myApi.get<ApiResponse<DoctorProfile>>("/medical/doctor/profile", {
      headers: buildAuthHeaders(),
    });

    // 后端可能返回 code=0（成功）或 code=200（成功）
    if (resp.data && (resp.data.code === 0 || resp.data.code === 200)) {
      return resp.data.data;
    }

    throw new Error(resp.data?.message || "获取医生信息失败");
  } catch (err) {
    console.error(err);
    return undefined;
  }
};
