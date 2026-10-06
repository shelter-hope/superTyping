import { apiClient } from '@/db/client';
import type { GameRecord, GameRecordUpload, GameMode, LeaderboardEntry } from '@/types/database';

export interface GameRecordUploadResult {
  recordId?: number;
  currentDifficulty?: number;
}

export async function createGameRecord(record: GameRecordUpload): Promise<GameRecordUploadResult | null> {
  const response = await apiClient.post('/game/records', record);
  return response.data.data ?? null;
}

export async function getUserGameRecords(userId: number, limit = 50): Promise<GameRecord[]> {
  const response = await apiClient.get('/game/records', {
    params: { userId, limit }
  });
  return response.data.data || [];
}

export async function getUserGameRecordsByDateRange(
  userId: number,
  startDate: string,
  endDate?: string
): Promise<GameRecord[]> {
  const response = await apiClient.get('/game/records', {
    params: { userId, startDate, endDate, limit: 100 } 
  });
  return response.data.data || [];
}

export async function getUserBestRecord(userId: number, gameMode: GameMode): Promise<GameRecord | null> {
  const response = await apiClient.get('/game/best', {
    params: { userId, mode: gameMode }
  });
  return response.data.data;
}

export async function getAllGameRecords(limit = 100): Promise<GameRecord[]> {
  const response = await apiClient.get('/game/records', {
    params: { limit }
  });
  return response.data.data || [];
}

export interface GameRecommendResponse {
  focusChars: string[];
  suggestedBaseSpeed?: number;
  suggestedMode?: string;
  previewWords?: string[];
  summary?: string;
  suggestedMonsterCount?: number;
  suggestedAvgStringLength?: number;
  suggestedNormalRatio?: number;
  suggestedAttackRatio?: number;
  suggestedEvasiveRatio?: number;
  suggestedSmartRatio?: number;
  currentDifficulty?: number;
}

export async function getGameRecommend(gameMode?: string): Promise<GameRecommendResponse> {
  const params = gameMode ? { gameMode } : {};
  const response = await apiClient.get('/game/config/recommend', { params });
  return response.data.data ?? {};
}

export interface GameAdjustParam {
  currentDifficulty?: number;
  monsterSpeed?: number;
  monsterCount?: number;
  avgStringLength?: number;
  normalRatio?: number;
  attackRatio?: number;
  evasiveRatio?: number;
  smartRatio?: number;
}

export interface MonsterDataDTO {
  monsterType?: string;
  targetStr?: string;
  speed?: number;
  survivalTime?: number;
  destroyCostTime?: number;
  totalCount?: number;
  inputErrorCount?: number;
  isTrueMean?: boolean;
  score?: number;
  difficultyLevel?: number;
  spawnWave?: number;
  isParent?: boolean;
}

export interface NextRoundResponse {
  adjustParam: GameAdjustParam;
  monsters: MonsterDataDTO[];
  planVersion?: number;
  targetAccuracy?: number;
}

export async function getNextRound(
  roundNum: number,
  gameMode: string,
  customDictIds?: number[],
  systemDictIds?: number[]
): Promise<NextRoundResponse> {
  const params: Record<string, unknown> = { roundNum, gameMode };
  if (customDictIds?.length) params.customDictIds = customDictIds;
  if (systemDictIds?.length) params.systemDictIds = systemDictIds;
  const response = await apiClient.get('/game/config/next-round', { params });
  return response.data.data ?? { adjustParam: {}, monsters: [] };
}

export interface GameCharTypeConfigDTO {
  gameMode: string;
  charType?: string;
  charTypes?: string[];
}

export async function getGameCharTypes(gameMode: string): Promise<GameCharTypeConfigDTO> {
  const response = await apiClient.get('/game/config/char-types', {
    params: { gameMode },
  });
  return response.data.data ?? { gameMode };
}

export async function updateGameCharTypes(gameMode: string, charTypes: string[]): Promise<boolean> {
  const response = await apiClient.put('/game/config/char-types', {
    gameMode,
    charTypes,
  });
  return response.data.data === true;
}

export async function saveGameConfig(config: any): Promise<boolean> {
  const response = await apiClient.post('/game/config', config);
  return response.data.data;
}

export async function updateGameState(state: 'pause' | 'resume' | 'exit'): Promise<boolean> {
  const response = await apiClient.post('/game/state', { state });
  return response.data.data;
}

export async function adjustGameParams(params: any): Promise<boolean> {
  const response = await apiClient.post('/game/params', params);
  return response.data.data;
}

export async function recordRoundData(roundData: any): Promise<boolean> {
  const response = await apiClient.post('/game/round-data', roundData);
  return response.data.data;
}

export async function exportGameData(recordId: number, format: 'excel' | 'json'): Promise<string> {
  const response = await apiClient.get(`/game/export/${recordId}`, {
    params: { format }
  });
  return response.data.data;
}

export async function getLeaderboard(gameMode: GameMode, limit = 50): Promise<LeaderboardEntry[]> {
  const response = await apiClient.get('/leaderboard', {
    params: { mode: gameMode, limit }
  });
  return response.data.data || [];
}

export async function getUserRank(userId: number, gameMode: GameMode): Promise<number | null> {
  const response = await apiClient.get('/leaderboard/my-rank', {
    params: { mode: gameMode }
  });
  return response.data.data?.rank ?? null;
}

export async function getLeaderboardByPeriod(gameMode: GameMode, period: 'total' | 'week' | 'month', limit = 50): Promise<LeaderboardEntry[]> {
  const response = await apiClient.get('/leaderboard/period', {
    params: { mode: gameMode, period, limit }
  });
  return response.data.data || [];
}

export async function searchLeaderboard(keyword: string, gameMode: GameMode): Promise<LeaderboardEntry[]> {
  const response = await apiClient.get('/leaderboard/search', {
    params: { keyword, mode: gameMode }
  });
  return response.data.data || [];
}

export async function getLeaderboardDetails(userId: number, gameMode: GameMode): Promise<any> {
  const response = await apiClient.get('/leaderboard/details', {
    params: { userId, mode: gameMode }
  });
  return response.data.data;
}
