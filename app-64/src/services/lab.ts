import { apiClient } from '@/db/client';

/** 实验台算法（玩家可选、管理端可编辑）。 */
export interface LabAlgorithmDto {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  sortOrder: number;
  defaultParams: Record<string, number>;
  schemaVersion: number;
}

/** 实验台参数预设（系统预设 userId 为空）。 */
export interface LabPresetDto {
  id: number;
  userId: number | null;
  algorithmId: string;
  name: string;
  params: Record<string, number>;
  isPublic: boolean;
  system: boolean;
}

export interface LabPresetPayload {
  algorithmId: string;
  name: string;
  params: Record<string, number>;
  isPublic: boolean;
}

export interface LabAlgorithmPayload {
  name: string;
  description: string;
  enabled: boolean;
  sortOrder: number;
  defaultParams: Record<string, number>;
  schemaVersion: number;
}

// ── 玩家侧 ──────────────────────────────────────────────

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

export async function saveLabPreset(body: LabPresetPayload): Promise<LabPresetDto> {
  const response = await apiClient.post('/lab/presets', body);
  return response.data.data;
}

export async function deleteLabPreset(id: number): Promise<void> {
  await apiClient.delete(`/lab/presets/${id}`);
}

// ── 管理端 ──────────────────────────────────────────────

export async function fetchAdminLabAlgorithms(): Promise<LabAlgorithmDto[]> {
  const response = await apiClient.get('/admin/lab/algorithms');
  return response.data.data;
}

export async function fetchAdminLabPresets(): Promise<LabPresetDto[]> {
  const response = await apiClient.get('/admin/lab/presets');
  return response.data.data;
}

export async function updateLabAlgorithm(id: string, body: LabAlgorithmPayload): Promise<void> {
  await apiClient.put(`/admin/lab/algorithms/${id}`, body);
}

export async function createSystemLabPreset(body: LabPresetPayload): Promise<LabPresetDto> {
  const response = await apiClient.post('/admin/lab/presets', body);
  return response.data.data;
}

export async function deleteSystemLabPreset(id: number): Promise<void> {
  await apiClient.delete(`/admin/lab/presets/${id}`);
}
