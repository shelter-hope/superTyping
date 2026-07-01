# API 接口文档（与前端实现对齐）

本文档依据当前仓库中的 **`src/db/client.ts`**、**`src/db/api.ts`** 及 **`src/contexts/AuthContext.tsx`** 整理，描述前端实际调用的接口。业务后端为 **Java（Spring Boot）REST**，路径前缀为 **`{VITE_BACKEND_BASE_URL}/api/v1`**。

更完整的后端契约可参考 **`docs/BACKEND_API.md`**、**`docs/迁移说明.md`**。

---

## 1. 基础约定

### 1.1 Base URL

| 配置 | 说明 |
|------|------|
| `VITE_BACKEND_BASE_URL` | 后端根路径，默认 `http://localhost:8080/typingWord`（见 `client.ts`） |
| 实际 API 前缀 | `{backendBaseUrl}/api/v1` |

### 1.2 统一响应体

Axios 响应体为 `ResultMessage<T>`（`client.ts`）：

```json
{
  "code": 200,
  "msg": "success",
  "data": { }
}
```

`code !== 200` 时，前端拦截器会 `reject`，错误信息取自 `msg`。

### 1.3 认证

- 登录/注册成功后，前端将 token 存入 `localStorage` 键 **`auth_token`**。
- 后续请求由 `apiClient` 自动附加：`Authorization: Bearer <token>`。
- 登出目前仅清除本地 token（`AuthContext.signOut`），可选调用后端 `/auth/logout` 已注释。

---

## 2. 认证 `AuthContext` + `/auth/*`

| 方法 | 路径 | 说明 | 请求体 / 备注 |
|------|------|------|----------------|
| POST | `/auth/login` | 登录 | `{ username, password }` → `data`: `{ accessToken, user }` |
| POST | `/auth/signup` | 注册 | `{ username, password, nickname? }`（用户名规则：字母数字下划线） |
| GET | `/auth/me` | 当前用户 | 需 Bearer；用于会话恢复 |

封装位置：`AuthContext.tsx`（未在 `api.ts` 中导出）。

---

## 3. 用户与资料 `/profiles/*`

| 方法 | 路径 | 前端函数 | Query / Body |
|------|------|----------|--------------|
| GET | `/profiles/{userId}` | `getProfile` | — |
| PATCH | `/profiles/{userId}` | `updateProfile` | `Partial<Profile>` |
| PATCH | `/profiles/me` | `updateMyProfile` | `Partial<Profile>` |
| POST | `/profiles/{userId}/avatar` | `uploadAvatar` | `multipart/form-data`，字段 `file` |
| GET | `/profiles` | `getAllProfiles` | 列表（注释提示可能需管理员权限） |
| GET | `/profiles/{userId}/stats` | `getUserStats` | — |
| GET | `/auth/me` | `getCurrentUser` | 同认证节（在 `api.ts` 中封装） |

---

## 4. 游戏记录与对局 `/game/*`

| 方法 | 路径 | 前端函数 | 说明 |
|------|------|----------|------|
| POST | `/game/records` | `createGameRecord` | Body：`GameRecordUpload`；返回 `GameRecordUploadResult`（含 `recordId`、`currentDifficulty` 等） |
| GET | `/game/records` | `getUserGameRecords` / `getUserGameRecordsByDateRange` / `getAllGameRecords` | Query：`userId?`、`limit`、`startDate?`、`endDate?` |
| GET | `/game/best` | `getUserBestRecord` | Query：`userId`、`mode`（游戏模式） |

### 4.1 游戏配置与回合

| 方法 | 路径 | 前端函数 | Query / Body |
|------|------|----------|--------------|
| GET | `/game/config/recommend` | `getGameRecommend` | `gameMode?` |
| GET | `/game/config/next-round` | `getNextRound` | `roundNum`、`gameMode`、`customDictIds?`、`systemDictIds?`（数组由 axios 序列化） |
| GET | `/game/config/char-types` | `getGameCharTypes` | `gameMode` |
| PUT | `/game/config/char-types` | `updateGameCharTypes` | Body：`{ gameMode, charTypes: string[] }` |
| POST | `/game/config` | `saveGameConfig` | Body：任意配置对象 |
| POST | `/game/state` | `updateGameState` | Body：`{ state: 'pause' \| 'resume' \| 'exit' }` |
| POST | `/game/params` | `adjustGameParams` | Body：任意 |
| POST | `/game/round-data` | `recordRoundData` | Body：轮次数据 |
| GET | `/game/export/{recordId}` | `exportGameData` | Query：`format`: `excel` \| `json` |

---

## 5. 字符统计 `/stats/character*`

| 方法 | 路径 | 前端函数 | 说明 |
|------|------|----------|------|
| POST | `/stats/character/batch` | `updateCharacterStat` / `updateCharacterStatsBatch` | Body：`{ updates: { charValue, isCorrect }[] }` |
| GET | `/stats/character` | `getUserCharacterStats` | Query：`userId` |
| GET | `/stats/character/weak` | `getWeakCharacters` / `getMostErrorCharacters` | Query：`userId`、`limit`（注释：后端 weak 与错误次数排序可能共用） |

### 5.1 其他统计与分析

| 方法 | 路径 | 前端函数 | Query / 路径参数 |
|------|------|----------|------------------|
| GET | `/stats/typing-behavior/{userId}` | `analyzeTypingBehavior` | — |
| GET | `/stats/progress-trend` | `getProgressTrend` | `userId`、`period`: `week` \| `month` \| `year` |
| GET | `/stats/training-suggestions/{userId}` | `getTrainingSuggestions` | — |
| GET | `/stats/trend` | `getTrendData` | `userId`、`timeRange`: `all` \| `year` \| `month` \| `week` \| `3days`（趋势页将 `three-days` 映射为 `3days`） |
| GET | `/stats/speed-trend` | `getSpeedTrend` | 同上 |
| GET | `/stats/accuracy-trend` | `getAccuracyTrend` | 同上 |
| GET | `/stats/error-comparison` | `getErrorComparison` | `userId` |

---

## 6. 排行榜 `/leaderboard/*`

| 方法 | 路径 | 前端函数 | Query |
|------|------|----------|--------|
| GET | `/leaderboard` | `getLeaderboard` | `mode`（游戏模式）、`limit` |
| GET | `/leaderboard/my-rank` | `getUserRank` | `mode` |
| GET | `/leaderboard/period` | `getLeaderboardByPeriod` | `mode`、`period`: `total` \| `week` \| `month`、`limit` |
| GET | `/leaderboard/search` | `searchLeaderboard` | `keyword`、`mode` |
| GET | `/leaderboard/details` | `getLeaderboardDetails` | `userId`、`mode` |

游戏模式与后端约定一致，常见值含：`endless`、`dictionary`、`limit`（见准备页与类型定义）。

---

## 7. 词库 `/dictionaries/*`

| 方法 | 路径 | 前端函数 | 说明 |
|------|------|----------|------|
| POST | `/dictionaries/custom` | `createCustomDictionary` | `multipart`：`name`、`isPublic`、`file`（当前封装在无文件时会抛错） |
| GET | `/dictionaries/custom` | `getUserDictionaries` | 当前用户词库列表 |
| DELETE | `/dictionaries/custom/{dictionaryId}` | `deleteDictionary` | — |
| GET | `/dictionaries/system` | `getSystemDictionaries` | 系统词库（用户侧） |

---

## 8. 好友 `/friends/*`

| 方法 | 路径 | 前端函数 | Query / Body |
|------|------|----------|--------------|
| GET | `/friends` | `getFriendList` | — |
| GET | `/friends/search` | `searchUsers` | `keyword` |
| POST | `/friends/request` | `sendFriendRequest` | Query：`receiverId`（body 为 `null`） |
| PATCH | `/friends/request/{requestId}` | `handleFriendRequest` | Query：`status`: `accepted` \| `rejected` |
| GET | `/friends/request` | `getFriendRequests` | — |
| DELETE | `/friends/{friendId}` | `deleteFriend` | — |

---

## 9. 消息 `/messages/*`

| 方法 | 路径 | 前端函数 | Query / Body |
|------|------|----------|--------------|
| GET | `/messages/conversations` | `getConversations` | — |
| GET | `/messages/conversation/{conversationId}` | `getConversationMessages` | `page`、`size` |
| GET | `/messages` | `getMessagesByFriend` | `friendId`、`offset` |
| POST | `/messages` | `sendMessage` | Query：`receiverId`、`content`、`type`（默认 `text`） |
| PATCH | `/messages/{messageId}/read` | `markMessageAsRead` | — |
| POST | `/messages/read-by-friend` | `markAsReadByFriend` | Query：`friendId` |
| GET | `/messages/unread-count` | `getUnreadCount` | — |
| GET | `/messages/system` | `getSystemMessages` | — |

---

## 10. AI `/ai/*`

| 方法 | 路径 | 前端函数 | Body |
|------|------|----------|------|
| GET | `/ai/training-summary` | `getAiTrainingSummary` | 返回 `AiTrainingSummary`（供提示词注入） |
| POST | `/ai/chat` | `postAiChat` | `{ message, conversationId? }` → `AiChatResponse` |

说明：路由 **`/ai-assistant`**（`AIAssistantPage`）中的 **`AIChatInterface`** 通过 **`postAiChat`** 调用后端 **`POST /ai/chat`**，与本文档一致。

---

## 11. 用户配置与帮助

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| POST | `/user/config` | `saveUserConfig` |
| GET | `/user/config` | `getUserConfig` |
| PATCH | `/user/sound-settings` | `updateSoundSettings` |
| GET | `/help` | `getHelpContent` |

---

## 12. 管理后台 `/admin/*`

以下均需 **管理员** 身份及有效 Bearer。列表类接口普遍返回 `{ items, total }`（`AdminPageResult<T>`），具体字段以后端为准。

### 12.1 用户与游戏记录（`api.ts` 中带 TODO 注释，仍被管理页调用）

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| GET | `/admin/users` | `adminGetUsers` |
| PATCH | `/admin/users/{userId}/role` | `adminUpdateUserRole` |
| DELETE | `/admin/users/{userId}/game-data` | `adminDeleteUserGameData` |
| GET | `/admin/game-records` | `adminGetGameRecords` |
| PATCH | `/admin/game-records/{recordId}` | `adminUpdateGameRecord` |
| DELETE | `/admin/game-records/{recordId}` | `adminDeleteGameRecord` |

### 12.2 系统数据与配置

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| GET | `/admin/system-params` | `adminGetSystemParams` |
| GET | `/admin/system-dictionaries` | `adminGetSystemDictionaries` |
| GET | `/admin/system-dictionary-words` | `adminGetSystemDictionaryWords` |
| GET | `/admin/custom-dictionaries` | `adminGetCustomDictionaries` |
| GET | `/admin/custom-dictionary-words` | `adminGetCustomDictionaryWords` |
| GET | `/admin/system-monster-type-configs` | `adminGetSystemMonsterTypeConfigs` |
| GET | `/admin/system-monster-common-configs` | `adminGetSystemMonsterCommonConfigs` |
| GET | `/admin/leaderboard-entries` | `adminGetLeaderboardEntries` |
| GET | `/admin/character-stats` | `adminGetCharacterStats` |
| GET | `/admin/user-daily-stats` | `adminGetUserDailyStats` |
| GET | `/admin/user-info-snapshots` | `adminGetUserInfoSnapshots` |

### 12.3 社交与通知（只读列表）

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| GET | `/admin/friend-relations` | `adminGetFriendRelations` |
| GET | `/admin/user-conversations` | `adminGetUserConversations` |
| GET | `/admin/friend-chat-messages` | `adminGetFriendChatMessages` |
| GET | `/admin/system-notifications` | `adminGetSystemNotifications` |

### 12.4 用户个性化与怪物数据

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| GET | `/admin/user-app-configs` | `adminGetUserAppConfigs` |
| GET | `/admin/user-game-mode-configs` | `adminGetUserGameModeConfigs` |
| GET | `/admin/user-monster-configs` | `adminGetUserMonsterConfigs` |
| GET | `/admin/user-model-params` | `adminGetUserModelParams` |
| GET | `/admin/monster-data` | `adminGetMonsterData` |
| GET | `/admin/string-parents` | `adminGetStringParents` |

### 12.5 AI 与会话审计

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| GET | `/admin/ai-conversations` | `adminGetAiConversations` |
| GET | `/admin/ai-chat-messages` | `adminGetAiChatMessages` |

### 12.6 自适应难度（Dₜ）日志与趋势

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| GET | `/admin/user-model-adapt-logs` | `adminGetUserModelAdaptLogs` |
| GET | `/admin/user-model-adapt-logs/trend` | `adminGetAdaptLogsTrend` |

### 12.7 怪物与规则配置（管理页）

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| GET | `/admin/monster-configs` | `getMonsterConfigs` |
| PATCH | `/admin/monster-configs/{type}` | `updateMonsterConfig` |
| GET | `/admin/character-rules` | `getCharacterGenerationRules` |
| PATCH | `/admin/character-rules` | `updateCharacterGenerationRules` |
| GET | `/admin/movement-rules` | `getMovementRules` |
| PATCH | `/admin/movement-rules` | `updateMovementRules` |
| GET | `/admin/difficulty-params` | `getDifficultyParams` |
| PATCH | `/admin/difficulty-params` | `updateDifficultyParams` |

### 12.8 配置模板

| 方法 | 路径 | 前端函数 |
|------|------|----------|
| POST | `/admin/config-templates` | `saveConfigTemplate` |
| GET | `/admin/config-templates` | `getConfigTemplates` |
| GET | `/admin/config-templates/{templateId}` | `loadConfigTemplate` |

分页参数在各函数中多为 `page`、`pageSize`，部分接口另有筛选字段（与 `api.ts` 中 `params` 类型一致）。

---

## 13. 前端封装索引（`src/db/api.ts`）

所有上述 REST 调用均通过 **`apiClient`** 发起；新增接口时请同步修改 **`api.ts`** 与本文件。

---

## 14. 修订说明

- 前端数据面已统一为 **Java `/api/v1`**，不再使用 Supabase 客户端。
- 若后端路径或字段与表内不符，以运行中的 Java API 及 **`docs/BACKEND_API.md`** 为准，并建议同时更正 `api.ts` 与本文件。

**文档更新**：2026-03-24
