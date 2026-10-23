import { apiClient } from '@/db/client';
import type { Profile } from '@/types/database';

export async function getCurrentUser(): Promise<Profile> {
  const response = await apiClient.get('/auth/me');
  return response.data.data;
}

export async function changePassword(oldPassword: string, newPassword: string, confirmPassword: string): Promise<boolean> {
  const response = await apiClient.post('/auth/change-password', { oldPassword, newPassword, confirmPassword });
  return response.data.data === true;
}