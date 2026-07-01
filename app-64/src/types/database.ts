/**
 * 前端与后端 DTO 对齐的类型定义（与 Java 后端 REST 返回结构一致，camelCase）
 */

// ==================== 用户与认证 ====================

/** 当前用户 /auth/me 返回，对应后端 UserDTO */
export interface Profile {
  userId: number;
  username: string;
  nickname: string;
  avatar: string;
  role: UserRole;
}

export type UserRole = 'user' | 'admin';

// ==================== 游戏记录 ====================

/** 游戏记录，对应后端 GameRecord 实体 */
export interface GameRecord {
  recordId: number;
  userId: number;
  gameMode: string;
  selectedChars?: string;
  roundNum: number;
  totalScore: number;
  totalChars: number;
  correctChars: number;
  avgSpeed: number;
  peakSpeed: number;
  avgAccuracy: number;
  peakAccuracy: number;
  gameDuration: number;
  createTime?: string;
}

/** 单条字符统计更新，对应后端 CharacterStatsUpdateDTO；用于本局结算时上传 */
export interface CharacterStatsUpdateDTO {
  charValue: string;
  isCorrect: boolean;
  charType?: string;
}

/** 上传游戏记录时的请求体，对应后端 GameRecordUploadDTO；可含怪物列表与字符统计 */
export type GameRecordUpload = Omit<GameRecord, 'recordId' | 'createTime' | 'userId'> & {
  monsters?: unknown[];
  /** 本局每个字符的输入结果，用于更新 character_stats，个人页「字符统计（错误最多）」展示 */
  characterStatsUpdates?: CharacterStatsUpdateDTO[];
};

// ==================== 字符统计 ====================

/** 字符统计，对应后端 CharacterStats 实体 */
export interface CharacterStat {
  statId: number;
  userId: number;
  charValue: string;
  charType: string;
  totalAttempts: number;
  correctAttempts: number;
  errorCount: number;
  averageAccuracy: number | null;
  recentAccuracy: number | null;
  updateTime?: string;
}

// ==================== 词库 ====================

/** 自定义词库，对应后端 CustomDictionary 实体；words 为前端扩展（推荐词库或从文件解析） */
export interface CustomDictionary {
  dictId: number;
  userId: number;
  name: string;
  description?: string;
  wordCount: number;
  isPublic?: boolean;
  fileUrl?: string;
  fileName?: string;
  createTime?: string;
  updateTime?: string;
  /** 词条列表（后端列表接口不返回，由推荐接口或前端解析文件得到） */
  words?: string[];
}

/** 系统词库，对应后端 SystemDictionary 实体 */
export interface SystemDictionary {
  dictId: number;
  name: string;
  description?: string;
  wordCount: number;
  category?: string;
  isActive?: boolean;
  fileUrl?: string;
  fileName?: string;
  createTime?: string;
  updateTime?: string;
}

// ==================== 排行榜 ====================

/** 排行榜条目，对应后端 LeaderboardDTO */
export interface LeaderboardEntry {
  entryId?: number;
  recordId?: number;
  userId: number;
  username: string;
  nickname: string;
  avatar: string;
  gameMode: string;
  totalScore: number;
  roundNum: number;
  avgSpeed: number;
  peakSpeed: number;
  avgAccuracy: number;
  peakAccuracy: number;
  gameDuration: number;
  createTime?: string;
  rank: number;
}

// ==================== 游戏配置 ====================

export type GameMode = 'endless' | 'dictionary' | 'limit';

/** 怪物类型配置，对应后端 SystemMonsterConfig */
export interface MonsterConfig {
  id?: number;
  type: string;
  initSpeed?: number;
  initStrLength?: number;
  score: number;
  description?: string;
  updatedAt?: string;
}

// ==================== 好友与消息 ====================

/** 好友信息，对应后端 FriendResponseDTO */
export interface FriendResponseDTO {
  id: number;
  userId: number;
  username: string;
  avatar?: string;
  bio?: string;
  isOnline?: boolean;
  lastActiveTime?: string;
}

/** 好友请求，对应后端 FriendRequestDTO */
export interface FriendRequestDTO {
  id: number;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName: string;
  sendTime: string;
  status: string;
}

/** 消息，对应后端 MessageDTO */
export interface MessageDTO {
  id: number;
  senderId: number;
  senderName: string;
  senderAvatar?: string;
  receiverId: number;
  receiverName?: string;
  content: string;
  sendTime: string;
  isRead: boolean;
  type: string;
  conversationId?: number;
  unreadCount?: number;
}

// ==================== 后台管理 / 系统配置 ====================

/** 系统参数，对应后端 SystemParam 实体 */
export interface SystemParam {
  paramId: number;
  paramName: string;
  paramValue: string;
  paramType?: string;
  description?: string;
  isSystem?: boolean;
  isActive?: boolean;
  createTime?: string;
  updateTime?: string;
}

/** 系统怪物类型基础配置，对应 SystemMonsterTypeConfig */
export interface SystemMonsterTypeConfig {
  id: number;
  initSpeed: number;
  initStrLength: number;
  type: string;
  score: number;
  description?: string;
  isActive?: boolean;
  updatedAt?: string;
}

/** 系统怪物公共配置，对应 SystemMonsterCommoConfig */
export interface SystemMonsterCommoConfig {
  id: number;
  minSpeed: number;
  maxSpeed: number;
  minStrLength: number;
  maxStrLength: number;
  updatedAt?: string;
}

/** 系统通知，对应 SystemNotification */
export interface SystemNotification {
  id: number;
  userId: number;
  notificationType: number;
  notificationTitle: string;
  notificationContent: string;
  businessId?: number;
  isRead: boolean;
  isPersistent: boolean;
  expireTime?: string;
  sendTime: string;
  readTime?: string;
  createTime?: string;
}

/** 用户应用配置，对应 UserAppConfig */
export interface UserAppConfig {
  configId: number;
  userId: number;
  isMusic?: boolean;
  isSoundEffect?: boolean;
  showGameRecords?: boolean;
  showRankings?: boolean;
  isSearchable?: boolean;
  darkMode?: boolean;
  notificationsEnabled?: boolean;
  autoSave?: boolean;
  language?: string;
  fontSize?: number;
  theme?: string;
  createTime?: string;
  updateTime?: string;
}

/** 用户游戏模式配置概要，对应 UserGameModeConfig */
export interface UserGameModeConfigSummary {
  id: number;
  userId: number;
  modeName: string;
  monsterInitNum: number;
  monsterTypePro: string;
  createTime?: string;
  updateTime?: string;
}

/** 用户怪物配置，对应 UserMonsterConfig */
export interface UserMonsterConfig {
  id: number;
  userId: number;
  initSpeed?: number;
  initStrLength: number;
  type?: string;
  updatedAt?: string;
}

/** 怪物明细，对应 MonsterData */
export interface MonsterDataEntry {
  monsterId: number;
  recordId: number;
  monsterType: string;
  targetStr: string;
  speed?: number;
  survivalTime?: number;
  destroyCostTime?: number;
  totalCount?: number;
  inputErrorCount?: number;
  isTrueMean?: number;
  score?: number;
  spawnWave?: number;
  isParent?: boolean;
  difficultyLevel?: number;
}

/** 字符串父本记录，对应 StringParent */
export interface StringParentRecord {
  id: number;
  userId: number;
  strParents: string;
  createTime?: string;
  updateTime?: string;
}

/** 用户每日统计，对应 UserDailyStats */
export interface UserDailyStatsEntry {
  id: number;
  userId: number;
  statDate: string;
  gameCount: number;
  totalScore: number;
  avgSpeed?: number;
  avgAccuracy?: number;
  peakSpeed?: number;
  peakAccuracy?: number;
  totalChars: number;
  correctChars: number;
  gameMode?: string;
  createTime?: string;
  updateTime?: string;
}

/** 用户历史快照，对应 UserInfoSnapshot */
export interface UserInfoSnapshotEntry {
  id: number;
  userId: number;
  snapshotTime: string;
  bestScore: number;
  bestRound: number;
  bestSpeed?: number;
  bestAccuracy?: number;
  avgSpeed?: number;
  avgAccuracy?: number;
  gameNum: number;
}

/** AI 会话概要，对应 AiConversation */
export interface AiConversationSummary {
  id: number;
  userId: number;
  aiRobotId: number;
  conversationTitle?: string;
  lastMessageId?: number;
  lastMessageTime?: string;
  isTop?: boolean;
  isMute?: boolean;
  createTime?: string;
  updateTime?: string;
}

/** AI 聊天消息，对应 AiChatMessage */
export interface AiChatMessageEntry {
  id: number;
  conversationId: number;
  userId: number;
  aiRobotId: number;
  messageRole: number;
  messageType: number;
  messageContent: string;
  sendTime: string;
  isSuccess?: boolean;
  errorMsg?: string;
  createTime?: string;
}

/** 好友关系概要，对应 FriendRelation */
export interface FriendRelationEntry {
  id: number;
  userId: number;
  friendUserId: number;
  relationStatus: number;
  applyTime: string;
  agreeTime?: string;
  createTime?: string;
  updateTime?: string;
}

/** 好友聊天消息概要，对应 FriendChatMessage */
export interface FriendChatMessageEntry {
  id: number;
  conversationId: number;
  senderId: number;
  receiverId: number;
  messageType: number;
  messageContent: string;
  sendTime: string;
  readStatus: number;
  readTime?: string;
  isRecall: boolean;
  recallTime?: string;
  createTime?: string;
}

/** 用户个性化模型参数概要，对应 UserModelParams */
export interface UserModelParamsEntry {
  paramId: number;
  userId: number;
  gameMode: string;
  latestRound: number;
  lastDifficulty?: number;
  weightRealAccuracy?: number;
  weightRealSpeed?: number;
  weightCharErrorRate?: number;
  weightHistoryAccuracy?: number;
  weightHistoryPeakSpeed?: number;
  weightErrorCharRatio?: number;
  weightCurrentRound?: number;
  fatigueBufferFlag?: boolean;
  progressSpeedFlag?: boolean;
  rollbackFlag?: boolean;
  maxAdjustRatio?: number;
  smoothAdjustCoeff?: number;
  crossoverRate?: number;
  mutationRate?: number;
  speedAdjustCoeff?: number;
  lengthAdjustCoeff?: number;
  consecutiveMismatchRound?: number;
  lastAdjustTime?: string;
  createTime?: string;
  updateTime?: string;
}
