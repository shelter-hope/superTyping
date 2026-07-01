import axios from 'axios';

// 定义统一响应结构
export interface ResultMessage<T> {
  code: number;
  msg: string;
  data: T;
}

/** 后端服务根路径（用于 API 与拼接头像等静态资源 URL）。生产环境设置 VITE_BACKEND_BASE_URL 为实际网关地址 */
export const backendBaseUrl = import.meta.env.VITE_BACKEND_BASE_URL ?? 'http://localhost:8080/typingWord';

/** 将后端返回的相对路径（如 /uploads/xxx 或 文件名）转为可访问的完整 URL */
export function resolveBackendAssetUrl(path: string | undefined): string {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  const p = path.startsWith('/') ? path : '/uploads/' + path;
  return backendBaseUrl + p;
}

export const apiClient = axios.create({
  baseURL: backendBaseUrl + '/api/v1',
  timeout: 10000,
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

// 响应拦截器：处理统一响应结构
apiClient.interceptors.response.use((response) => {
  const res = response.data as ResultMessage<any>;
  // 如果 code 不是 200，视为错误
  if (res.code && res.code !== 200) {
    return Promise.reject(new Error(res.msg || 'Error'));
  }
  return response;
}, (error) => {
  return Promise.reject(error);
});
