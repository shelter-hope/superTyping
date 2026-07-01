import { apiClient } from '@/db/client';
import type { CustomDictionary, SystemDictionary } from '@/types/database';

export async function createCustomDictionary(
  userId: number,
  name: string,
  words: string[],
  file?: File,
  isPublic: boolean = false
): Promise<CustomDictionary> {
  const formData = new FormData();
  formData.append('name', name);
  formData.append('isPublic', isPublic.toString());
  if (file) {
    formData.append('file', file);
  } else {
    throw new Error("File upload required for now");
  }

  const response = await apiClient.post('/dictionaries/custom', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data.data;
}

export async function getUserDictionaries(userId: number): Promise<CustomDictionary[]> {
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