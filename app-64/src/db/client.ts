import axios, { type InternalAxiosRequestConfig } from 'axios';
import { createSingleFlight, shouldRefresh } from './refreshSession';

// 定义统一响应结构
export interface ResultMessage<T> {
  code: number;
  msg: string;
  data: T;
}

function trimTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '');
}

/** 后端服务根路径（API 网关地址）。生产环境设置 VITE_BACKEND_BASE_URL 为实际网关地址 */
export const backendBaseUrl = trimTrailingSlash(
  import.meta.env.VITE_BACKEND_BASE_URL || 'http://localhost:8080'
);

/** 将后端返回的相对路径（如 /uploads/xxx 或 文件名）转为可访问的完整 URL */
export function resolveBackendAssetUrl(path: string | undefined): string {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  const p = path.startsWith('/') ? path : '/uploads/' + path;
  return backendBaseUrl + p;
}

export const apiClient = axios.create({
  baseURL: backendBaseUrl + '/api/v1',
  timeout: 15000,
  paramsSerializer: {
    // Spring @RequestParam List<T> expects repeated keys: ids=1&ids=2, not ids[]=1&ids[]=2.
    indexes: null,
  },
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：自动带上 Token（登录/注册不带，避免脏 token 干扰）
apiClient.interceptors.request.use((config) => {
  const url = config.url || '';
  const isAuthPublic =
    url.includes('/auth/login') ||
    url.includes('/auth/signup') ||
    url.includes('/auth/refresh');
  if (!isAuthPublic) {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export interface ApiError extends Error {
  code?: number;
  data?: any;
}

// 响应拦截器：处理统一响应结构和错误
apiClient.interceptors.response.use((response) => {
  const res = response.data as ResultMessage<any>;
  if (typeof res?.code === 'number' && res.code !== 200) {
    const error = new Error(res.msg || '请求失败') as ApiError;
    error.code = res.code;
    error.data = res.data;
    return Promise.reject(error);
  }
  return response;
}, (error) => {
  let apiError = error as ApiError;
  
  if (error.response) {
    const status = error.response.status;
    const res = error.response.data as ResultMessage<any>;
    const reqUrl = String(error.config?.url || '');
    const isLoginAttempt =
      reqUrl.includes('/auth/login') || reqUrl.includes('/auth/signup');

    apiError.code = res?.code || status;
    apiError.message =
      res?.msg ||
      (isLoginAttempt && status === 401
        ? '用户名或密码错误'
        : getDefaultErrorMessage(status));
    apiError.data = res?.data;

    const config = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    if (config && shouldRefresh(status, reqUrl, Boolean(config._retry))) {
      config._retry = true;
      return refreshAccessToken()
        .then((token) => {
          if (!token) {
            clearSession();
            return Promise.reject(apiError);
          }
          config.headers.Authorization = `Bearer ${token}`;
          return apiClient(config);
        })
        .catch(() => {
          clearSession();
          return Promise.reject(apiError);
        });
    }
    if (status === 401 && !isLoginAttempt) {
      clearSession();
    }
  } else if (error.request) {
    // 常见于 CORS：Network 面板可能仍显示 200，但 JS 读不到响应体
    const hasCorsHint =
      typeof error.message === 'string' &&
      /cors|network error|access-control/i.test(error.message);
    apiError.message = hasCorsHint
      ? '跨域被拦截（CORS），请确认仅由网关处理跨域且响应头无重复'
      : '网络连接失败，请检查网络或后端服务是否启动';
    apiError.code = 0;
  }
  
  return Promise.reject(apiError);
});

const refreshAccessToken = createSingleFlight(async () => {
  const refreshToken = localStorage.getItem('refresh_token');
  if (!refreshToken) return null;
  const response = await axios.post(
    backendBaseUrl + '/api/v1/auth/refresh',
    null,
    { params: { refreshToken }, timeout: 15000 },
  );
  const payload = response.data as ResultMessage<{ accessToken?: string; refreshToken?: string }>;
  const accessToken = payload?.data?.accessToken;
  if (!accessToken) return null;
  localStorage.setItem('auth_token', accessToken);
  if (payload.data?.refreshToken) localStorage.setItem('refresh_token', payload.data.refreshToken);
  return accessToken;
});

function clearSession() {
  localStorage.removeItem('auth_token');
  localStorage.removeItem('refresh_token');
}

function getDefaultErrorMessage(status: number): string {
  const messages: Record<number, string> = {
    400: '请求参数错误',
    401: '未登录或登录已过期',
    403: '无权访问',
    404: '资源未找到',
    500: '服务器内部错误',
  };
  return messages[status] || '请求失败';
}
