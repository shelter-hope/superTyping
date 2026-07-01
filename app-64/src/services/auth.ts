import { apiClient } from '@/db/client';
import type { Profile } from '@/types/database';

export async function getCurrentUser(): Promise<Profile> {
  const response = await apiClient.get('/auth/me');
  return response.data.data;
}