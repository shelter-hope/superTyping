// 数据库API封装
import { apiClient } from './client';
import type {
  Profile,
  GameRecord,
  GameRecordUpload,
  CharacterStat,
  CustomDictionary,
  LeaderboardEntry,
  GameMode,
  MonsterConfig,
  UserRole,
  FriendResponseDTO,
  FriendRequestDTO,
  MessageDTO,
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
} from '@/types/database';

// ==================== 用户相关 ====================

export async function getCurrentUser(): Promise<Profile> {
  const response = await apiClient.get('/auth/me');
  return response.data.data;
}

export async function getProfile(userId: number): Promise<Profile | null> {
  const response = await apiClient.get(`/profiles/${userId}`);
  return response.data.data;
}

export async function updateProfile(userId: number, updates: Partial<Profile>): Promise<Profile> {
  const response = await apiClient.patch(`/profiles/${userId}`, updates);
  return response.data.data;
}

/** 更新当前登录用户资料（走 PATCH /profiles/me，无需传 userId，避免路径参数问题） */
export async function updateMyProfile(updates: Partial<Profile>): Promise<Profile> {
  const response = await apiClient.patch('/profiles/me', updates);
  return response.data.data;
}

/** 上传用户头像（图片文件），返回头像 URL */
export async function uploadAvatar(userId: number, file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post(`/profiles/${userId}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data.data;
}

export async function getAllProfiles(): Promise<Profile[]> {
  const response = await apiClient.get('/profiles'); // Note: This might need admin endpoint if listing all
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

// ==================== 游戏记录相关 ====================

/** 上传游戏记录后的结算摘要（后端 GameRecordUploadResultDTO） */
export interface GameRecordUploadResult {
  recordId?: number;
  /** 本局结算后的当前难度 Dₜ */
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
  // Backend might need to implement date range filtering
  // For now, fetching all (or limit) and filtering client side, or assume backend adds this param later
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

// ==================== 排行榜相关 ====================

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

// ==================== 字符统计相关 ====================

export async function updateCharacterStat(
  userId: number,
  character: string,
  isCorrect: boolean
): Promise<void> {
  // Batch update with single item
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
  // Currently backend maps /weak to error count desc, so same endpoint
  const response = await apiClient.get('/stats/character/weak', {
    params: { userId, limit }
  });
  return response.data.data || [];
}

// ==================== 自定义词库相关 ====================

// Combined upload and create in backend
export async function createCustomDictionary(
  userId: number,
  name: string,
  words: string[], // Ignored if file is provided, or backend needs to handle text content
  file?: File,
  isPublic: boolean = false
): Promise<CustomDictionary> {
  const formData = new FormData();
  formData.append('name', name);
  formData.append('isPublic', isPublic.toString());
  if (file) {
    formData.append('file', file);
  } else {
    // If no file, maybe creating empty or from words array?
    // Backend expects file currently. If words array, generate a file?
    // For now assuming file upload is primary.
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

// ==================== 社交系统相关 ====================

export async function getFriendList(): Promise<FriendResponseDTO[]> {
  const response = await apiClient.get('/friends');
  return response.data.data || [];
}

export async function searchUsers(keyword: string): Promise<FriendResponseDTO[]> {
  const response = await apiClient.get('/friends/search', {
    params: { keyword }
  });
  return response.data.data || [];
}

export async function sendFriendRequest(receiverId: number): Promise<FriendRequestDTO> {
  const response = await apiClient.post('/friends/request', null, {
    params: { receiverId }
  });
  return response.data.data;
}

export async function handleFriendRequest(requestId: number, status: 'accepted' | 'rejected'): Promise<boolean> {
  const response = await apiClient.patch(`/friends/request/${requestId}`, null, {
    params: { status }
  });
  return response.data.data;
}

export async function getFriendRequests(): Promise<FriendRequestDTO[]> {
  const response = await apiClient.get('/friends/request');
  return response.data.data || [];
}

export async function deleteFriend(friendId: number): Promise<boolean> {
  const response = await apiClient.delete(`/friends/${friendId}`);
  return response.data.data;
}

// ==================== 管理后台相关（AdminController） ====================

export interface AdminPageResult<T> {
  items: T[];
  total: number;
}

// ---- 系统参数与配置 ----

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

// ---- 排行榜与统计 ----

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

// ---- 社交与通知 ----

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

// ---- 个性化配置与 AI ----

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

// ==================== 消息系统相关 ====================

export async function getConversations(): Promise<MessageDTO[]> { // Should define ConversationDTO
  const response = await apiClient.get('/messages/conversations');
  return response.data.data || [];
}

export async function getConversationMessages(conversationId: number, page = 0, size = 20): Promise<MessageDTO[]> {
  const response = await apiClient.get(`/messages/conversation/${conversationId}`, {
    params: { page, size }
  });
  return response.data.data || [];
}

/** 按好友拉取历史消息（不依赖会话列表，直接 GET /messages?friendId=&offset=） */
export async function getMessagesByFriend(friendId: number, offset = 0): Promise<MessageDTO[]> {
  const response = await apiClient.get('/messages', {
    params: { friendId, offset }
  });
  return response.data.data || [];
}

export async function sendMessage(receiverId: number, content: string, type = 'text'): Promise<MessageDTO> {
  const response = await apiClient.post('/messages', null, {
    params: { receiverId, content, type }
  });
  return response.data.data;
}

export async function markMessageAsRead(messageId: number): Promise<boolean> {
  const response = await apiClient.patch(`/messages/${messageId}/read`);
  return response.data.data;
}

/** 进入与某好友的聊天时调用：将该会话内当前用户收到的未读消息全部标记为已读 */
export async function markAsReadByFriend(friendId: number): Promise<boolean> {
  const response = await apiClient.post('/messages/read-by-friend', null, {
    params: { friendId },
  });
  return response.data.data;
}

export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get('/messages/unread-count');
  return response.data.data;
}

export async function getSystemMessages(): Promise<MessageDTO[]> {
  // Assuming backend has this endpoint or we filter from messages
  // For now, let's assume a new endpoint /messages/system
  // If not exists, this will fail. 
  // Alternatively, we can use getConversations and find the system conversation?
  // Or just mock it if backend isn't ready.
  // Let's try to hit /messages/system
  const response = await apiClient.get('/messages/system');
  return response.data.data || [];
}

// ==================== 游戏核心功能接口 ====================

// 游戏配置推荐接口（基于用户统计与进化建议，支持按模式）
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

// 下一轮参数与怪物列表（Dₜ、Vₜ、Nₜ、Lₜ、类型占比 + 怪物列表）
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

// ==================== 游戏准备配置：字符类型（game_char_type_configs） ====================

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

// 游戏配置保存接口
export async function saveGameConfig(config: any): Promise<boolean> {
  const response = await apiClient.post('/game/config', config);
  return response.data.data;
}

// 游戏状态管理接口
export async function updateGameState(state: 'pause' | 'resume' | 'exit'): Promise<boolean> {
  const response = await apiClient.post('/game/state', { state });
  return response.data.data;
}

// 游戏参数调整接口
export async function adjustGameParams(params: any): Promise<boolean> {
  const response = await apiClient.post('/game/params', params);
  return response.data.data;
}

// ==================== 数据记录与分析接口 ====================

// 实时轮次数据记录接口
export async function recordRoundData(roundData: any): Promise<boolean> {
  const response = await apiClient.post('/game/round-data', roundData);
  return response.data.data;
}

// 游戏结算数据导出接口
export async function exportGameData(recordId: number, format: 'excel' | 'json'): Promise<string> {
  const response = await apiClient.get(`/game/export/${recordId}`, {
    params: { format }
  });
  return response.data.data;
}

// 打字行为分析接口
export async function analyzeTypingBehavior(userId: number): Promise<any> {
  const response = await apiClient.get(`/stats/typing-behavior/${userId}`);
  return response.data.data;
}

// 进步趋势分析接口
export async function getProgressTrend(userId: number, period: 'week' | 'month' | 'year'): Promise<any> {
  const response = await apiClient.get('/stats/progress-trend', {
    params: { userId, period }
  });
  return response.data.data;
}

// 针对性训练建议接口
export async function getTrainingSuggestions(userId: number): Promise<any> {
  const response = await apiClient.get(`/stats/training-suggestions/${userId}`);
  return response.data.data;
}

// ==================== 排行榜系统接口 ====================

// 多维度排行榜接口
export async function getLeaderboardByPeriod(gameMode: GameMode, period: 'total' | 'week' | 'month', limit = 50): Promise<LeaderboardEntry[]> {
  const response = await apiClient.get('/leaderboard/period', {
    params: { mode: gameMode, period, limit }
  });
  return response.data.data || [];
}

// 排行榜搜索接口
export async function searchLeaderboard(keyword: string, gameMode: GameMode): Promise<LeaderboardEntry[]> {
  const response = await apiClient.get('/leaderboard/search', {
    params: { keyword, mode: gameMode }
  });
  return response.data.data || [];
}

// 排行榜详情接口
export async function getLeaderboardDetails(userId: number, gameMode: GameMode): Promise<any> {
  const response = await apiClient.get('/leaderboard/details', {
    params: { userId, mode: gameMode }
  });
  return response.data.data;
}

// ==================== 趋势分析接口 ====================

// 时间维度数据查询接口
export async function getTrendData(userId: number, timeRange: 'all' | 'year' | 'month' | 'week' | '3days'): Promise<any> {
  const response = await apiClient.get('/stats/trend', {
    params: { userId, timeRange }
  });
  return response.data.data;
}

// 速度趋势分析接口
export async function getSpeedTrend(userId: number, timeRange: 'all' | 'year' | 'month' | 'week' | '3days'): Promise<any> {
  const response = await apiClient.get('/stats/speed-trend', {
    params: { userId, timeRange }
  });
  return response.data.data;
}

// 准确率趋势分析接口
export async function getAccuracyTrend(userId: number, timeRange: 'all' | 'year' | 'month' | 'week' | '3days'): Promise<any> {
  const response = await apiClient.get('/stats/accuracy-trend', {
    params: { userId, timeRange }
  });
  return response.data.data;
}

// 错误次数对比接口
export async function getErrorComparison(userId: number): Promise<any> {
  const response = await apiClient.get('/stats/error-comparison', {
    params: { userId }
  });
  return response.data.data;
}

// ==================== AI 助手 ====================

/** 供 AI 提示词使用的用户训练数据摘要 */
export interface AiTrainingSummary {
  contextForPrompt: string;
  focusChars: string[];
  suggestedMode: string;
  recentAccuracy?: number;
  recentSpeed?: number;
  errorComparisonSummary?: string;
}

/** 获取当前用户训练数据摘要（调用 AI/LLM 前可注入到系统提示） */
export async function getAiTrainingSummary(): Promise<AiTrainingSummary> {
  const response = await apiClient.get('/ai/training-summary');
  return response.data.data;
}

/** 方案 A：发送消息并获取助手回复（会话存后端，自动附带训练摘要） */
export interface AiChatResponse {
  reply: string;
  conversationId: number;
  messageId?: number;
}

export async function postAiChat(message: string, conversationId?: number): Promise<AiChatResponse> {
  const response = await apiClient.post('/ai/chat', { message, conversationId });
  return response.data.data;
}

// ==================== 管理后台 ====================

// Placeholder for admin functions, needing backend implementation
export async function adminGetUsers(page = 1, pageSize = 20, searchTerm?: string): Promise<{ users: Profile[], total: number }> {
    // TODO: Implement backend endpoint for admin user list
    const response = await apiClient.get('/admin/users', { params: { page, pageSize, searchTerm } });
    return response.data.data || { users: [], total: 0 };
}

export async function adminUpdateUserRole(userId: number, role: UserRole): Promise<void> {
    // TODO: Implement backend endpoint
    await apiClient.patch(`/admin/users/${userId}/role`, { role });
}

export async function adminDeleteUserGameData(userId: number): Promise<void> {
    // TODO: Implement backend endpoint
    await apiClient.delete(`/admin/users/${userId}/game-data`);
}

export async function adminGetGameRecords(page = 1, pageSize = 20, userId?: number): Promise<{ records: GameRecord[], total: number }> {
    // TODO: Implement backend endpoint
    const response = await apiClient.get('/admin/game-records', { params: { page, pageSize, userId } });
    return response.data.data || { records: [], total: 0 };
}

export async function adminUpdateGameRecord(recordId: number, updates: Partial<GameRecord>): Promise<void> {
    // TODO: Implement backend endpoint
    await apiClient.patch(`/admin/game-records/${recordId}`, updates);
}

export async function adminDeleteGameRecord(recordId: number): Promise<void> {
    // TODO: Implement backend endpoint
    await apiClient.delete(`/admin/game-records/${recordId}`);
}

// 用户模型适配日志（Dₜ 更新记录，分页）
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

// Dₜ 趋势聚合（按日，用于管理页趋势图）
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
    // TODO: Implement backend endpoint
    const response = await apiClient.get('/admin/monster-configs');
    return response.data.data || [];
}

export async function updateMonsterConfig(type: string, updates: Partial<MonsterConfig>): Promise<void> {
    // TODO: Implement backend endpoint
    await apiClient.patch(`/admin/monster-configs/${type}`, updates);
}

// 字符生成规则配置接口
export async function getCharacterGenerationRules(): Promise<any> {
  const response = await apiClient.get('/admin/character-rules');
  return response.data.data || [];
}

export async function updateCharacterGenerationRules(rules: any): Promise<boolean> {
  const response = await apiClient.patch('/admin/character-rules', rules);
  return response.data.data;
}

// 行进规则配置接口
export async function getMovementRules(): Promise<any> {
  const response = await apiClient.get('/admin/movement-rules');
  return response.data.data || [];
}

export async function updateMovementRules(rules: any): Promise<boolean> {
  const response = await apiClient.patch('/admin/movement-rules', rules);
  return response.data.data;
}

// 难度参数调整接口
export async function getDifficultyParams(): Promise<any> {
  const response = await apiClient.get('/admin/difficulty-params');
  return response.data.data;
}

export async function updateDifficultyParams(params: any): Promise<boolean> {
  const response = await apiClient.patch('/admin/difficulty-params', params);
  return response.data.data;
}

// 配置模板管理接口
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

// ==================== 配置功能接口 ====================

// 用户配置管理接口
export async function saveUserConfig(config: any): Promise<boolean> {
  const response = await apiClient.post('/user/config', config);
  return response.data.data;
}

export async function getUserConfig(): Promise<any> {
  const response = await apiClient.get('/user/config');
  return response.data.data;
}

// 音效设置接口
export async function updateSoundSettings(settings: { enabled: boolean }): Promise<boolean> {
  const response = await apiClient.patch('/user/sound-settings', settings);
  return response.data.data;
}

// 帮助中心接口
export async function getHelpContent(): Promise<any> {
  const response = await apiClient.get('/help');
  return response.data.data;
}
