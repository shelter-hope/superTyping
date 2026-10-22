import { apiClient } from '@/db/client';

export interface LabAlgorithmDto {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  sortOrder: number;
  defaultParams: Record<string, number>;
  schemaVersion: number;
}

export interface LabPresetDto {
  id: number;
  userId: number | null;
  algorithmId: string;
  name: string;
  params: Record<string, number>;
  isPublic: boolean;
  system: boolean;
}

export async function fetchLabAlgorithms(): Promise<LabAlgorithmDto[]> {
  const response = await apiClient.get('/lab/algorithms');
  return response.data.data;
}

export async function fetchLabPresets(algorithmId: string): Promise<LabPresetDto[]> {
  const response = await apiClient.get('/lab/presets', { params: { algorithmId } });
  return response.data.data;
}

export async function fetchMyLabPresets(algorithmId: string): Promise<LabPresetDto[]> {
  const response = await apiClient.get('/lab/presets/mine', { params: { algorithmId } });
  return response.data.data;
}

export async function saveLabPreset(body: {
  algorithmId: string;
  name: string;
  params: Record<string, number>;
  isPublic: boolean;
}): Promise<LabPresetDto> {
  const response = await apiClient.post('/lab/presets', body);
  return response.data.data;
}

export async function deleteLabPreset(id: number): Promise<void> {
  await apiClient.delete(`/lab/presets/${id}`);
}
