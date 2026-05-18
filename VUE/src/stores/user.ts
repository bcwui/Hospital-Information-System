import { defineStore } from "pinia";
import { DoAxiosWithErro, DoAxios } from "@/api";
import myApi from "@/api"; // 新增：引入默认 Axios 实例，以便同步 header

// 定义用户信息类型（根据实际接口返回结构调整）
export interface UserInfo {
  userId: string;
  patientId: string;
  doctorId?: string;
  username: string;
  email: string;
  phone: string;
  avatar: string; // 返回的数据中avatar字段有问题，请检查链接是否合法或稍后重试查看正确内容
  name: string;
  gender: number; // 0 or 1
  age: number;
  idCard: string;
  region: string;
  address: string;
  role: number; // 0,1,2,3,4,5
  createTime: string; // ISO 8601 格式的日期时间字符串
  updateTime: string; // ISO 8601 格式的日期时间字符串
  token: string; // 可选的token字段
}

// Store 定义
export const useUserStore = defineStore("user", {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem("userInfo") || "null") as UserInfo | null,
    userToken: localStorage.getItem("userToken") || "",
    isLoggedIn: !!localStorage.getItem("userToken"),
  }),

  getters: {
    isAuthenticated: (state): boolean => !!state.userToken,
    userName: (state): string => state.userInfo?.username || "Guest",
  },

  actions: {
    async login(userData: { account: string; password: string }) {
      const info = await DoAxiosWithErro<UserInfo>("/auth/login", "post", userData);
      // 登录成功，保存用户信息
      this.userToken = info.token;
      this.userInfo = info;
      this.isLoggedIn = true;

      // 持久化
      localStorage.setItem("userToken", this.userToken);
      localStorage.setItem("userInfo", JSON.stringify(info));

      // === 新增：将 token 同步到 Axios 实例默认 header，确保后续所有请求（包括未显式设置 header 的场景）都会携带 token ===
      if (this.userToken) {
        myApi.defaults.headers.common["sa-token-authorization"] = this.userToken;
      }
      // === 以上为新增代码 ===
    },

    async logout() {
      try {
        await DoAxios("/auth/logout", "post", {}, true);
      } catch {
        // 可忽略登出接口错误（如 token 失效）
      }
      this.clearSession();
    },

    clearSession() {
      this.userInfo = null;
      this.userToken = "";
      this.isLoggedIn = false;
      localStorage.removeItem("userToken");
      localStorage.removeItem("userInfo");

      // === 新增：清除 Axios 默认 header，避免残留失效 token 导致异步请求行为异常 ===
      try {
        delete myApi.defaults.headers.common["sa-token-authorization"];
      } catch (e) {
        // ignore
      }
      // === 以上为新增代码 ===
    },
  },
});
