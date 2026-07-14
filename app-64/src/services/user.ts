import { apiClient } from '@/db/client';
import type { Profile } from '@/types/database';

export async function getProfile(userId: number): Promise<Profile | null> {
  const response = await apiClient.get(`/profiles/${userId}`);
  return response.data.data;
}

export async function updateProfile(userId: number, updates: Partial<Profile>): Promise<Profile> {
  const response = await apiClient.patch(`/profiles/${userId}`, updates);
  return response.data.data;
}

export async function updateMyProfile(updates: Partial<Profile>): Promise<Profile> {
  const response = await apiClient.patch('/profiles/me', updates);
  return response.data.data;
}

export async function uploadAvatar(userId: number, file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post(`/profiles/${userId}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data.data;
}

export async function getAllProfiles(): Promise<Profile[]> {
  const response = await apiClient.get('/profiles');
  return response.data.data || [];
}

export interface UserStats {
  userId: number;
  username: string;
  totalGames: number;
  averageAccuracy: number;
  totalWordsTyped: number;
  totalCharactersTyped: number;
  bestScore: number;
  bestGameMode: string;
  ranking: number;
}

export async function getUserStats(userId: number): Promise<UserStats> {
  const response = await apiClient.get(`/profiles/${userId}/stats`);
  return response.data.data;
}

export async function saveUserConfig(config: any): Promise<any> {
  const response = await apiClient.post('/user/config', config);
  return response.data.data;
}

export async function getUserConfig(): Promise<any> {
  const response = await apiClient.get('/user/config');
  return response.data.data;
}

export async function updateSoundSettings(settings: { enabled: boolean }): Promise<boolean> {
  const response = await apiClient.patch('/user/sound-settings', settings);
  return response.data.data;
}
