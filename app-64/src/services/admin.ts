import { apiClient } from '@/db/client';
import type {
  Profile,
  GameRecord,
  LeaderboardEntry,
  CharacterStat,
  CustomDictionary,
  SystemDictionary,
  SystemParam,
  SystemMonsterTypeConfig,
  SystemMonsterCommoConfig,
  SystemNotification,
  UserAppConfig,
  UserGameModeConfigSummary,
  UserMonsterConfig,
  MonsterDataEntry,
  StringParentRecord,
  UserDailyStatsEntry,
  UserInfoSnapshotEntry,
  AiConversationSummary,
  AiChatMessageEntry,
  FriendRelationEntry,
  FriendChatMessageEntry,
  UserModelParamsEntry,
  UserRole,
  MonsterConfig,
} from '@/types/database';

export interface AdminPageResult<T> {
  items: T[];
  total: number;
}

export async function adminGetSystemParams(params: {
  page?: number;
  pageSize?: number;
  paramName?: string;
}): Promise<AdminPageResult<SystemParam>> {
  const response = await apiClient.get('/admin/system-params', { params });
  return response.data.data;
}

export async function adminGetSystemDictionaries(params: {
  page?: number;
  pageSize?: number;
  name?: string;
  category?: string;
  isActive?: boolean;
}): Promise<AdminPageResult<SystemDictionary>> {
  const response = await apiClient.get('/admin/system-dictionaries', { params });
  return response.data.data;
}

export async function adminGetSystemDictionaryWords(dictId: number, page = 1, pageSize = 50) {
  const response = await apiClient.get('/admin/system-dictionary-words', {
    params: { dictId, page, pageSize },
  });
  return response.data.data as AdminPageResult<unknown>;
}

export async function adminGetCustomDictionaries(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  name?: string;
  isPublic?: boolean;
}): Promise<AdminPageResult<CustomDictionary>> {
  const response = await apiClient.get('/admin/custom-dictionaries', { params });
  return response.data.data;
}

export async function adminGetCustomDictionaryWords(dictId: number, page = 1, pageSize = 50) {
  const response = await apiClient.get('/admin/custom-dictionary-words', {
    params: { dictId, page, pageSize },
  });
  return response.data.data as AdminPageResult<unknown>;
}

export async function adminGetSystemMonsterTypeConfigs(): Promise<SystemMonsterTypeConfig[]> {
  const response = await apiClient.get('/admin/system-monster-type-configs');
  return response.data.data || [];
}

export async function adminGetSystemMonsterCommonConfigs(): Promise<SystemMonsterCommoConfig[]> {
  const response = await apiClient.get('/admin/system-monster-common-configs');
  return response.data.data || [];
}

export async function adminGetLeaderboardEntries(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  gameMode?: string;
}): Promise<AdminPageResult<LeaderboardEntry>> {
  const response = await apiClient.get('/admin/leaderboard-entries', { params });
  return response.data.data;
}

export async function adminGetCharacterStats(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  charType?: string;
  charValue?: string;
}): Promise<AdminPageResult<CharacterStat>> {
  const response = await apiClient.get('/admin/character-stats', { params });
  return response.data.data;
}

export async function adminGetUserDailyStats(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
}): Promise<AdminPageResult<UserDailyStatsEntry>> {
  const response = await apiClient.get('/admin/user-daily-stats', { params });
  return response.data.data;
}

export async function adminGetUserInfoSnapshots(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
}): Promise<AdminPageResult<UserInfoSnapshotEntry>> {
  const response = await apiClient.get('/admin/user-info-snapshots', { params });
  return response.data.data;
}

export async function adminGetFriendRelations(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  friendUserId?: number;
  relationStatus?: number;
}): Promise<AdminPageResult<FriendRelationEntry>> {
  const response = await apiClient.get('/admin/friend-relations', { params });
  return response.data.data;
}

export async function adminGetUserConversations(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
}): Promise<AdminPageResult<unknown>> {
  const response = await apiClient.get('/admin/user-conversations', { params });
  return response.data.data;
}

export async function adminGetFriendChatMessages(params: {
  conversationId: number;
  page?: number;
  pageSize?: number;
}): Promise<AdminPageResult<FriendChatMessageEntry>> {
  const response = await apiClient.get('/admin/friend-chat-messages', { params });
  return response.data.data;
}

export async function adminGetSystemNotifications(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  notificationType?: number;
  isRead?: boolean;
}): Promise<AdminPageResult<SystemNotification>> {
  const response = await apiClient.get('/admin/system-notifications', { params });
  return response.data.data;
}

export async function adminGetUserAppConfigs(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
}): Promise<AdminPageResult<UserAppConfig>> {
  const response = await apiClient.get('/admin/user-app-configs', { params });
  return response.data.data;
}

export async function adminGetUserGameModeConfigs(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  modeName?: string;
}): Promise<AdminPageResult<UserGameModeConfigSummary>> {
  const response = await apiClient.get('/admin/user-game-mode-configs', { params });
  return response.data.data;
}

export async function adminGetUserMonsterConfigs(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  type?: string;
}): Promise<AdminPageResult<UserMonsterConfig>> {
  const response = await apiClient.get('/admin/user-monster-configs', { params });
  return response.data.data;
}

export async function adminGetUserModelParams(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  gameMode?: string;
}): Promise<AdminPageResult<UserModelParamsEntry>> {
  const response = await apiClient.get('/admin/user-model-params', { params });
  return response.data.data;
}

export async function adminGetMonsterData(recordId: number, page = 1, pageSize = 100): Promise<AdminPageResult<MonsterDataEntry>> {
  const response = await apiClient.get('/admin/monster-data', {
    params: { recordId, page, pageSize },
  });
  return response.data.data;
}

export async function adminGetStringParents(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
}): Promise<AdminPageResult<StringParentRecord>> {
  const response = await apiClient.get('/admin/string-parents', { params });
  return response.data.data;
}

export async function adminGetAiConversations(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  aiRobotId?: number;
}): Promise<AdminPageResult<AiConversationSummary>> {
  const response = await apiClient.get('/admin/ai-conversations', { params });
  return response.data.data;
}

export async function adminGetAiChatMessages(params: {
  conversationId: number;
  page?: number;
  pageSize?: number;
}): Promise<AdminPageResult<AiChatMessageEntry>> {
  const response = await apiClient.get('/admin/ai-chat-messages', { params });
  return response.data.data;
}

export async function adminGetUsers(page = 1, pageSize = 20, searchTerm?: string): Promise<{ users: Profile[], total: number }> {
  const response = await apiClient.get('/admin/users', { params: { page, pageSize, searchTerm } });
  return response.data.data || { users: [], total: 0 };
}

export async function adminUpdateUserRole(userId: number, role: UserRole): Promise<void> {
  await apiClient.patch(`/admin/users/${userId}/role`, { role });
}

export async function adminDeleteUserGameData(userId: number): Promise<void> {
  await apiClient.delete(`/admin/users/${userId}/game-data`);
}

export async function adminGetGameRecords(page = 1, pageSize = 20, userId?: number): Promise<{ records: GameRecord[], total: number }> {
  const response = await apiClient.get('/admin/game-records', { params: { page, pageSize, userId } });
  return response.data.data || { records: [], total: 0 };
}

export async function adminUpdateGameRecord(recordId: number, updates: Partial<GameRecord>): Promise<void> {
  await apiClient.patch(`/admin/game-records/${recordId}`, updates);
}

export async function adminDeleteGameRecord(recordId: number): Promise<void> {
  await apiClient.delete(`/admin/game-records/${recordId}`);
}

export interface UserModelAdaptLogDTO {
  logId: number;
  userId: number;
  gameRecordId: number;
  gameMode: string;
  roundNum: number;
  beforeDifficulty: number | null;
  afterDifficulty: number | null;
  adjustParams: string | null;
  triggerRule: string | null;
  logTime: string;
}

export async function adminGetUserModelAdaptLogs(params: {
  page?: number;
  pageSize?: number;
  userId?: number;
  gameMode?: string;
  startDate?: string;
  endDate?: string;
}): Promise<{ logs: UserModelAdaptLogDTO[]; total: number }> {
  const response = await apiClient.get('/admin/user-model-adapt-logs', { params });
  const data = response.data.data ?? {};
  return { logs: data.logs ?? [], total: data.total ?? 0 };
}

export interface AdaptLogTrendPoint {
  date: string;
  avgAfterDifficulty: number | null;
  count: number;
  maxRound: number;
}

export async function adminGetAdaptLogsTrend(params: {
  userId: number;
  gameMode?: string;
  startDate: string;
  endDate: string;
}): Promise<AdaptLogTrendPoint[]> {
  const response = await apiClient.get('/admin/user-model-adapt-logs/trend', { params });
  return response.data.data ?? [];
}

export async function getHelpContent(): Promise<any> {
  const response = await apiClient.get('/help');
  return response.data.data;
}

export async function getMonsterConfigs(): Promise<MonsterConfig[]> {
  const response = await apiClient.get('/admin/monster-configs');
  return response.data.data || [];
}

export async function updateMonsterConfig(type: string, updates: Partial<MonsterConfig>): Promise<void> {
  await apiClient.patch(`/admin/monster-configs/${type}`, updates);
}

export async function getCharacterGenerationRules(): Promise<any> {
  const response = await apiClient.get('/admin/character-rules');
  return response.data.data || [];
}

export async function updateCharacterGenerationRules(rules: any): Promise<boolean> {
  const response = await apiClient.patch('/admin/character-rules', rules);
  return response.data.data;
}

export async function getMovementRules(): Promise<any> {
  const response = await apiClient.get('/admin/movement-rules');
  return response.data.data || [];
}

export async function updateMovementRules(rules: any): Promise<boolean> {
  const response = await apiClient.patch('/admin/movement-rules', rules);
  return response.data.data;
}

export async function getDifficultyParams(): Promise<any> {
  const response = await apiClient.get('/admin/difficulty-params');
  return response.data.data;
}

export async function updateDifficultyParams(params: any): Promise<boolean> {
  const response = await apiClient.patch('/admin/difficulty-params', params);
  return response.data.data;
}

export async function saveConfigTemplate(name: string, config: any): Promise<boolean> {
  const response = await apiClient.post('/admin/config-templates', { name, config });
  return response.data.data;
}

export async function getConfigTemplates(): Promise<any[]> {
  const response = await apiClient.get('/admin/config-templates');
  return response.data.data || [];
}

export async function loadConfigTemplate(templateId: number): Promise<any> {
  const response = await apiClient.get(`/admin/config-templates/${templateId}`);
  return response.data.data;
}