import axios from "axios";
import type { AxiosRequestConfig, AxiosResponse } from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";

// 支持的请求方法类型
type Method = "get" | "post" | "put" | "delete";
type RequestPayload = Record<string, unknown> | FormData;

// 创建 Axios 实例
const API_BASE = import.meta.env.VITE_API_BASE || "/api";

const myApi = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  withCredentials: true, // === 新增：在跨域场景下携带 cookie（兼容） ===
});

// === 新增：初始化 axios 默认 header，优先使用 localStorage 中持久化的 token（容错） ===
const initialToken = localStorage.getItem("userToken") || "";
if (initialToken) {
  myApi.defaults.headers.common["sa-token-authorization"] = initialToken;
}
// === 以上为新增代码 ===

// 响应拦截器：统一处理响应和错误
myApi.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error) => {
    if (error.code === "ECONNABORTED" && error.message?.includes("timeout")) {
      error.message = "请求超时，请稍后重试";
      return Promise.reject(error);
    }

    if (!error.response) {
      error.message = "网络异常";
      return Promise.reject(error);
    }

    // 原先这里会直接在 401 时清除前端会话，导致 token 被移除，
    // 在 SSE/长连接与短暂失效场景下会误删 token，改为交由调用方处理。
    if (error.response.status === 401) {
      // 不在拦截器自动 clearSession，以免在短暂认证失效时丢失 token
      // 可在上层页面捕获 401 后统一跳转登录或清除会话
      // const userStore = useUserStore();
      // userStore?.clearSession?.();
    }

    const fallback = error.response.statusText || "系统出错，请稍后重试";
    error.message = error.response.data?.message || fallback;
    return Promise.reject(error);
  }
);

// === 新增函数：多来源获取 token（优先级：store -> axios.defaults -> localStorage -> cookie） ===
function getTokenFromSources(): string | null {
  try {
    const userStore = useUserStore();
    // 1. 优先取 store 中的 token
    if (userStore && userStore.userToken) {
      return userStore.userToken;
    }

    // 2. axios 默认 header 中的 token
    const commonHeaders = (myApi.defaults.headers?.common ?? {}) as unknown as Record<
      string,
      unknown
    >;
    const headerToken = commonHeaders["sa-token-authorization"];
    if (headerToken) {
      return String(headerToken);
    }

    // 3. localStorage 回退
    const ls = localStorage.getItem("userToken");
    if (ls) {
      return ls;
    }
  } catch (e) {
    // 忽略读取错误，继续返回 null
    console.warn("getTokenFromSources error", e);
  }
  return null;
}
// === 以上为新增代码 ===

// 核心请求函数（简洁）
export const DoAxios = async <TResponse = unknown>(
  path: string,
  method: Method,
  requestInfo: RequestPayload | null | undefined = {},
  withToken: boolean = false,
  hasBody: boolean = false,
  timeoutMs?: number
): Promise<TResponse> => {
  const requestConfig: AxiosRequestConfig = {
    url: path,
    method,
  };

  if (typeof timeoutMs === "number") {
    requestConfig.timeout = timeoutMs;
  }
  const payload: RequestPayload = (requestInfo ?? {}) as RequestPayload;

  // 如果需要 Token，就添加 token 头
  if (withToken) {
    // 使用增强的多来源 token 获取
    const token = getTokenFromSources();
    if (!token) {
      // 当确实无法找到 token，抛出明确错误（上层捕获并处理）
      throw new Error("Token 为空，请先登录");
    }
    // 设置 sa-token 的 header
    requestConfig.headers = {
      ...(requestConfig.headers || {}),
      "sa-token-authorization": token,
    };

    // 同步到 axios 默认 header
    try {
      myApi.defaults.headers.common["sa-token-authorization"] = token;
    } catch (e) {
      // ignore
    }
  }

  // 判断是 query 参数还是 body 参数
  if ((method === "get" || method === "delete") && !hasBody) {
    requestConfig.params = payload as Record<string, unknown>;
  } else {
    requestConfig.data = payload;
  }

  // 请求并返回数据
  try {
    const resp = await myApi(requestConfig);
    if (resp.data.code === 200) {
      return resp.data.data as TResponse;
    }
    throw new Error(resp.data.message || "请求失败");
  } catch (error: unknown) {
    if (axios.isAxiosError(error)) {
      const message = error.response?.data?.message || error.message || "请求失败";
      throw new Error(message);
    }
    if (error instanceof Error) {
      throw new Error(error.message);
    }
    throw new Error("未知错误");
  }
};

// 包含统一 UI 错误提示的请求函数
export const DoAxiosWithErro = async <TResponse = unknown>(
  path: string,
  method: Method,
  requestInfo: RequestPayload | null | undefined = {},
  withToken: boolean = false,
  hasBody: boolean = false,
  timeoutMs?: number
): Promise<TResponse> => {
  try {
    return await DoAxios<TResponse>(path, method, requestInfo, withToken, hasBody, timeoutMs);
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : String(e);
    ElMessage.error(message);
    throw e;
  }
};

export default myApi;
