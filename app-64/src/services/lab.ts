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

export async function fetchAdminLabRuns(params?: {
  userId?: number;
  algorithmId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<LabRunPage> {
  const response = await apiClient.get('/admin/lab/runs', { params });
  return response.data.data;
}

// ── 实验快照 runs ──────────────────────────────────────

export interface LabRunDto {
  id: number;
  userId: number;
  algorithmId: string;
  pluginId: string;
  schemaVersion: number;
  schemaStale: boolean;
  engineKind: string;
  title: string | null;
  notes: string | null;
  seed: number;
  params: Record<string, number>;
  presetId: number | null;
  status: string;
  startedAt: string | null;
  endedAt: string | null;
  durationMs: number | null;
  summary: Record<string, number> | null;
  metrics: Record<string, unknown> | null;
  clientMeta: Record<string, unknown> | null;
  createdAt: string | null;
}

export interface LabRunPayload {
  algorithmId: string;
  pluginId?: string;
  schemaVersion?: number;
  title?: string;
  notes?: string;
  seed: number;
  params: Record<string, number>;
  presetId?: number | null;
  status?: string;
  startedAt?: string;
  endedAt?: string;
  durationMs?: number;
  summary?: Record<string, number>;
  metrics?: Record<string, unknown>;
  clientMeta?: Record<string, unknown>;
}

export interface LabRunPage {
  content: LabRunDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export async function createLabRun(body: LabRunPayload): Promise<LabRunDto> {
  const response = await apiClient.post('/lab/runs', body);
  return response.data.data;
}

export async function fetchLabRuns(params?: {
  algorithmId?: string;
  page?: number;
  size?: number;
}): Promise<LabRunPage> {
  const response = await apiClient.get('/lab/runs', { params });
  return response.data.data;
}

export async function fetchLabRun(id: number): Promise<LabRunDto> {
  const response = await apiClient.get(`/lab/runs/${id}`);
  return response.data.data;
}

export async function deleteLabRun(id: number): Promise<void> {
  await apiClient.delete(`/lab/runs/${id}`);
}

export async function forkLabRunPreset(id: number, name?: string): Promise<LabPresetDto> {
  const response = await apiClient.post(`/lab/runs/${id}/fork-preset`, name ? { name } : {});
  return response.data.data;
}
