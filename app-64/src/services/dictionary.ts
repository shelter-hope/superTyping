import { apiClient } from '@/db/client';
import type { CustomDictionary, SystemDictionary } from '@/types/database';

export async function createCustomDictionary(
  name: string,
  file: File,
  isPublic: boolean = false
): Promise<CustomDictionary> {
  const formData = new FormData();
  formData.append('name', name);
  formData.append('isPublic', isPublic.toString());
  formData.append('file', file);

  const response = await apiClient.post('/dictionaries/custom', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data.data;
}

export async function getUserDictionaries(): Promise<CustomDictionary[]> {
  const response = await apiClient.get('/dictionaries/custom');
  return response.data.data || [];
}

export async function deleteDictionary(dictionaryId: number): Promise<void> {
  await apiClient.delete(`/dictionaries/custom/${dictionaryId}`);
}

export async function getSystemDictionaries(): Promise<SystemDictionary[]> {
  const response = await apiClient.get('/dictionaries/system');
  return response.data.data || [];
}
