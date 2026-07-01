import { apiClient } from '@/db/client';
import type { CharacterStat } from '@/types/database';

export async function updateCharacterStat(
  userId: number,
  character: string,
  isCorrect: boolean
): Promise<void> {
  await apiClient.post('/stats/character/batch', {
    updates: [{ charValue: character, isCorrect }]
  });
}

export async function updateCharacterStatsBatch(
  userId: number,
  updates: Array<{ charValue: string; isCorrect: boolean }>
): Promise<void> {
  await apiClient.post('/stats/character/batch', { updates });
}

export async function getUserCharacterStats(userId: number): Promise<CharacterStat[]> {
  const response = await apiClient.get('/stats/character', {
    params: { userId }
  });
  return response.data.data || [];
}

export async function getWeakCharacters(userId: number, limit = 10): Promise<CharacterStat[]> {
  const response = await apiClient.get('/stats/character/weak', {
    params: { userId, limit }
  });
  return response.data.data || [];
}

export async function getMostErrorCharacters(userId: number, limit = 10): Promise<CharacterStat[]> {
  const response = await apiClient.get('/stats/character/weak', {
    params: { userId, limit }
  });
  return response.data.data || [];
}

export async function analyzeTypingBehavior(userId: number): Promise<any> {
  const response = await apiClient.get(`/stats/typing-behavior/${userId}`);
  return response.data.data;
}

export async function getProgressTrend(userId: number, period: 'week' | 'month' | 'year'): Promise<any> {
  const response = await apiClient.get('/stats/progress-trend', {
    params: { userId, period }
  });
  return response.data.data;
}

export async function getTrainingSuggestions(userId: number): Promise<any> {
  const response = await apiClient.get(`/stats/training-suggestions/${userId}`);
  return response.data.data;
}

export async function getTrendData(userId: number, timeRange: 'all' | 'year' | 'month' | 'week' | '3days'): Promise<any> {
  const response = await apiClient.get('/stats/trend', {
    params: { userId, timeRange }
  });
  return response.data.data;
}

export async function getSpeedTrend(userId: number, timeRange: 'all' | 'year' | 'month' | 'week' | '3days'): Promise<any> {
  const response = await apiClient.get('/stats/speed-trend', {
    params: { userId, timeRange }
  });
  return response.data.data;
}

export async function getAccuracyTrend(userId: number, timeRange: 'all' | 'year' | 'month' | 'week' | '3days'): Promise<any> {
  const response = await apiClient.get('/stats/accuracy-trend', {
    params: { userId, timeRange }
  });
  return response.data.data;
}

export async function getErrorComparison(userId: number): Promise<any> {
  const response = await apiClient.get('/stats/error-comparison', {
    params: { userId }
  });
  return response.data.data;
}