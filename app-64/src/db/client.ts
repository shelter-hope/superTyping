import axios from 'axios';

// 定义统一响应结构
export interface ResultMessage<T> {
  code: number;
  msg: string;
  data: T;
}

/** 后端服务根路径（API 网关地址）。生产环境设置 VITE_BACKEND_BASE_URL 为实际网关地址 */
export const backendBaseUrl = import.meta.env.VITE_BACKEND_BASE_URL ?? 'http://localhost:8080';

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
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：自动带上 Token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
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
  if (res.code && res.code !== 200) {
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
    
    apiError.code = res?.code || status;
    apiError.message = res?.msg || res?.message || getDefaultErrorMessage(status);
    apiError.data = res?.data;
    
    if (status === 401) {
      localStorage.removeItem('auth_token');
    }
  } else if (error.request) {
    apiError.message = '网络连接失败，请检查网络';
    apiError.code = 0;
  }
  
  return Promise.reject(apiError);
});

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
