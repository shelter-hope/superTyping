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

export async function adminGetUsers(
  page = 1,
  pageSize = 20,
  searchTerm?: string,
  filters: { role?: UserRole; status?: number } = {}
): Promise<{ users: Profile[], total: number }> {
  const response = await apiClient.get('/admin/users', { params: { page, pageSize, searchTerm, ...filters } });
  return response.data.data || { users: [], total: 0 };
}

export async function adminUpdateUserRole(userId: number, role: UserRole): Promise<void> {
  await apiClient.patch(`/admin/users/${userId}/role`, { role });
}

/** status: 1 正常 / 0 禁用；管理员账号不允许禁用 */
export async function adminUpdateUserStatus(userId: number, status: 0 | 1): Promise<void> {
  await apiClient.patch(`/admin/users/${userId}/status`, { status });
}

export async function adminUpdateUserProfile(userId: number, updates: { nickname?: string; signature?: string }): Promise<void> {
  await apiClient.patch(`/admin/users/${userId}/profile`, updates);
}

export interface AdminOverview {
  users: { userCount: number; newUsersToday: number; activeUsersToday: number; disabledUsers: number } | null;
  games: { recordCount: number; recordsToday: number; playersToday: number } | null;
  chat: {
    messageCount: number;
    messagesToday: number;
    systemDictionaryCount: number;
    customDictionaryCount: number;
    notificationCount: number;
  } | null;
}

export async function adminGetOverview(): Promise<AdminOverview> {
  const response = await apiClient.get('/admin/overview');
  return response.data.data ?? { users: null, games: null, chat: null };
}

export interface AdminOperationLogEntry {
  id: number;
  operatorId: number | null;
  method: string;
  path: string;
  queryString: string | null;
  requestBody: string | null;
  responseStatus: number | null;
  success: boolean;
  ip: string | null;
  durationMs: number | null;
  createTime: string;
}

export async function adminGetOperationLogs(params: {
  page?: number;
  pageSize?: number;
  operatorId?: number;
  method?: string;
  path?: string;
  success?: boolean;
}): Promise<AdminPageResult<AdminOperationLogEntry>> {
  const response = await apiClient.get('/admin/operation-logs', { params });
  return response.data.data;
}

/** userId 为空表示发给全部用户；返回实际写入的通知条数 */
export async function adminPublishNotification(params: {
  title: string;
  content: string;
  userId?: number;
  notificationType?: number;
}): Promise<number> {
  const response = await apiClient.post('/admin/system-notifications', params);
  return response.data.data?.count ?? 0;
}

export async function adminDeleteNotification(id: number): Promise<void> {
  await apiClient.delete(`/admin/system-notifications/${id}`);
}

export async function adminDeleteUserGameData(userId: number): Promise<void> {
  await apiClient.delete(`/admin/users/${userId}/game-data`);
}

export async function adminGetGameRecords(page = 1, pageSize = 20, userId?: number, gameMode?: string): Promise<{ records: GameRecord[], total: number }> {
  const response = await apiClient.get('/admin/game-records', { params: { page, pageSize, userId, gameMode } });
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

export async function getMonsterConfigs(): Promise<MonsterConfig[]> {
  const response = await apiClient.get('/admin/monster-configs');
  return response.data.data || [];
}

export async function updateMonsterConfig(type: string, updates: Partial<MonsterConfig>): Promise<void> {
  await apiClient.patch(`/admin/monster-configs/${type}`, updates);
}

/** 后台规则配置，保存在 system_params（JSON），未配置时后端返回默认值 */
export type AdminRuleKey = 'character-rules' | 'movement-rules' | 'difficulty-params';

export async function adminGetRules(key: AdminRuleKey): Promise<Record<string, unknown>> {
  const response = await apiClient.get(`/admin/${key}`);
  return response.data.data ?? {};
}

export async function adminSaveRules(key: AdminRuleKey, rules: Record<string, unknown>): Promise<void> {
  await apiClient.patch(`/admin/${key}`, rules);
}

export interface ConfigTemplateSummary {
  id: number;
  name: string;
  createdBy: number | null;
  createTime: string | null;
  updateTime: string | null;
}

export interface ConfigTemplateDetail extends ConfigTemplateSummary {
  config: unknown;
}

export async function adminGetConfigTemplates(): Promise<ConfigTemplateSummary[]> {
  const response = await apiClient.get('/admin/config-templates');
  return response.data.data ?? [];
}

export async function adminGetConfigTemplate(id: number): Promise<ConfigTemplateDetail> {
  const response = await apiClient.get(`/admin/config-templates/${id}`);
  return response.data.data;
}

/** 同名模板会被覆盖 */
export async function adminSaveConfigTemplate(name: string, config: unknown): Promise<void> {
  await apiClient.post('/admin/config-templates', { name, config });
}

export async function adminDeleteConfigTemplate(id: number): Promise<void> {
  await apiClient.delete(`/admin/config-templates/${id}`);
}

