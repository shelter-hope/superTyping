import { apiClient } from '@/db/client';
import type { Profile } from '@/types/database';

/** 登录/注册成功后返回的会话数据；token 由调用方写入本地存储。 */
export interface AuthSession {
  accessToken: string;
  refreshToken?: string | null;
  user: Record<string, unknown>;
}

/** 账号信息有的接口返回 userId、有的返回 id，统一在这里取值。 */
export function resolveUserId(user: Record<string, unknown>): number | undefined {
  const raw = user.userId ?? user.id;
  return typeof raw === 'number' ? raw : undefined;
}

/** 读取当前登录账号（/auth/me）。 */
export async function fetchCurrentUser(): Promise<Record<string, unknown>> {
  const response = await apiClient.get('/auth/me');
  return response.data.data as Record<string, unknown>;
}

/** 账号密码登录。 */
export async function signInRequest(username: string, password: string): Promise<AuthSession> {
  const response = await apiClient.post('/auth/login', { username, password });
  return response.data.data as AuthSession;
}

/** 注册并直接拿到会话。 */
export async function signUpRequest(
  username: string,
  password: string,
  nickname?: string
): Promise<AuthSession> {
  const response = await apiClient.post('/auth/signup', {
    username,
    password,
    nickname: nickname || username,
  });
  return response.data.data as AuthSession;
}

/** 注销当前会话；失败也不影响本地清理，所以调用方放在 finally 里。 */
export async function signOutRequest(refreshToken?: string | null): Promise<void> {
  await apiClient.post('/auth/logout', null, {
    params: refreshToken ? { refreshToken } : undefined,
  });
}

/** 兼容旧调用：返回账号档案。 */
export async function getCurrentUser(): Promise<Profile> {
  const data = await fetchCurrentUser();
  return data as unknown as Profile;
}

export async function changePassword(
  oldPassword: string,
  newPassword: string,
  confirmPassword: string
): Promise<boolean> {
  const response = await apiClient.post('/auth/change-password', {
    oldPassword,
    newPassword,
    confirmPassword,
  });
  return response.data.data === true;
}
