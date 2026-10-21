# API 接口文档（与前端实现对齐）

本文档依据当前仓库中的 **`src/db/client.ts`**、**`src/db/api.ts`** 及各服务文件整理，描述前端实际调用的接口。业务后端为 **Java（Spring Boot）REST**，路径前缀为 **`{VITE_BACKEND_BASE_URL}/api/v1`**。

后端采用微服务架构，前端通过 **API Gateway** 统一访问各微服务。

---

## 1. 基础约定

### 1.1 Base URL

| 配置 | 说明 |
|------|------|
| `VITE_BACKEND_BASE_URL` | API Gateway 根路径，默认 `http://localhost:8080`（见 `client.ts`） |
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
- 登出目前仅清除本地 token（`AuthContext.signOut`）。

### 1.4 API 服务层文件结构

前端 API 服务已按业务域拆分为多个文件：

| 文件 | 职责 | 目标微服务 |
|------|------|-----------|
| `src/services/auth.ts` | 认证相关 | user-svc |
| `src/services/user.ts` | 用户资料、配置 | user-svc |
| `src/services/game.ts` | 游戏记录、排行榜、游戏配置 | game-core-svc / chat-svc |
| `src/services/stats.ts` | 字符统计、行为分析、趋势 | game-core-svc |
| `src/services/dictionary.ts` | 词库管理 | chat-svc |
| `src/services/social.ts` | 好友、消息 | chat-svc |
| `src/services/ai.ts` | AI 助手、训练建议 | ai-svc |
| `src/services/admin.ts` | 管理后台 | admin-svc |

`src/db/api.ts` 作为统一出口，聚合导出所有服务。

---

## 2. 认证 `/auth/*`

| 方法 | 路径 | 前端函数 | 请求体 / 备注 |
|------|------|----------|----------------|
| POST | `/auth/login` | （`AuthContext`） | `{ username, password }` → `data`: `{ accessToken, user }` |
| POST | `/auth/signup` | （`AuthContext`） | `{ username, password, nickname? }` |
| GET | `/auth/me` | `getCurrentUser` | 需 Bearer；用于会话恢复 |

封装位置：登录/注册逻辑在 `AuthContext.tsx`，`getCurrentUser` 导出在 `auth.ts`。

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

---

## 4. 用户配置与帮助

| 方法 | 路径 | 前端函数 | Body |
|------|------|----------|------|
| POST | `/user/config` | `saveUserConfig` | 任意配置对象；返回保存后的 `UserAppConfig` |
| GET | `/user/config` | `getUserConfig` | 返回当前登录用户 `UserAppConfig`，不存在时后端自动创建默认配置 |
| PATCH | `/user/sound-settings` | `updateSoundSettings` | `{ enabled: boolean }`，同步更新 `isMusic` 与 `isSoundEffect` |
| GET | `/help` | `getHelpContent` | — |

---

## 5. 游戏记录与对局 `/game/*`

| 方法 | 路径 | 前端函数 | 说明 |
|------|------|----------|------|
| POST | `/game/records` | `createGameRecord` | Body：`GameRecordUpload`；返回 `GameRecordUploadResult`（含 `recordId`、`currentDifficulty` 等） |
| GET | `/game/records` | `getUserGameRecords` / `getUserGameRecordsByDateRange` / `getAllGameRecords` | Query：`userId?`、`limit`、`startDate?`、`endDate?` |
| GET | `/game/best` | `getUserBestRecord` | Query：`userId`、`mode`（游戏模式） |

### 5.1 游戏配置与回合

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

说明：

- `/game/config/char-types` 已持久化到 `game_char_type_configs`，按当前登录用户和游戏模式读取/更新。
- `/game/config/next-round` 已按游戏模式选择词源策略：无尽/限时模式基于易错字符与字符类型生成；词库模式会按 `customDictIds`、`systemDictIds` 从 `dictionary_words`、`system_dictionary_words` 抽词。
- 自定义词库抽词会校验词库归属，只允许当前登录用户使用自己的自定义词库。
- 难度参数读取 `model_init_params`、`model_constants`、`system_monster_commo_configs`，缺失配置时使用安全兜底值。
- `/game/records` 结算保存会同步维护 `user_daily_stats`、`user_model_params`、`user_model_adapt_logs`、`string_parents`，并在词库模式下按 `customDictIds` 回写 `dictionary_words` 练习指标。

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

## 7. 字符统计 `/stats/character*`

| 方法 | 路径 | 前端函数 | 说明 |
|------|------|----------|------|
| POST | `/stats/character/batch` | `updateCharacterStat` / `updateCharacterStatsBatch` | Body：`{ updates: { charValue, isCorrect }[] }` |
| GET | `/stats/character` | `getUserCharacterStats` | Query：`userId` |
| GET | `/stats/character/weak` | `getWeakCharacters` / `getMostErrorCharacters` | Query：`userId`、`limit`（注释：后端 weak 与错误次数排序可能共用） |

### 7.1 其他统计与分析

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

## 8. 词库 `/dictionaries/*`

| 方法 | 路径 | 前端函数 | 说明 |
|------|------|----------|------|
| POST | `/dictionaries/custom` | `createCustomDictionary` | `multipart`：`name`、`isPublic`、`file`；用户身份由 Bearer token 解析，不传 `userId` |
| GET | `/dictionaries/custom` | `getUserDictionaries` | 当前登录用户词库列表；用户身份由 Bearer token 解析，不传 `userId` |
| DELETE | `/dictionaries/custom/{dictionaryId}` | `deleteDictionary` | — |
| GET | `/dictionaries/system` | `getSystemDictionaries` | 系统词库（用户侧） |

---

## 9. 好友 `/friends/*`

| 方法 | 路径 | 前端函数 | Query / Body |
|------|------|----------|--------------|
| GET | `/friends` | `getFriendList` | — |
| GET | `/friends/search` | `searchUsers` | `keyword` |
| POST | `/friends/request` | `sendFriendRequest` | Query：`receiverId`（body 为 `null`） |
| PATCH | `/friends/request/{requestId}` | `handleFriendRequest` | Query：`status`: `accepted` \| `rejected` |
| GET | `/friends/request` | `getFriendRequests` | — |
| DELETE | `/friends/{friendId}` | `deleteFriend` | — |

---

## 10. 消息 `/messages/*`

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

## 11. AI `/ai/*`

| 方法 | 路径 | 前端函数 | Body |
|------|------|----------|------|
| GET | `/ai/training-summary` | `getAiTrainingSummary` | 返回 `AiTrainingSummary`（供提示词注入） |
| POST | `/ai/chat` | `postAiChat` | `{ message, conversationId? }` → `AiChatResponse` |

说明：路由 **`/ai-assistant`**（`AIAssistantPage`）中的 **`AIChatInterface`** 通过 **`postAiChat`** 调用后端 **`POST /ai/chat`**，与本文档一致。

---

## 12. 管理后台 `/admin/*`

以下均需 **管理员** 身份及有效 Bearer。当前后端由独立 `admin-svc` 提供聚合型 `/admin/**` 接口；`admin-svc` 不直接承载领域查询逻辑，而是按路径代理到各领域服务的 `/api/v1/internal/admin/**` 内部接口。列表类接口普遍返回 `{ items, total }`（`AdminPageResult<T>`），用户与游戏记录等兼容前端旧结构返回 `{ users, total }`、`{ records, total }`。

### 12.1 用户与游戏记录

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
| GET | `/admin/model-constants` | 管理模型常量 |
| PATCH | `/admin/model-constants/{id}` | 更新模型常量 |
| GET | `/admin/model-init-params` | 管理模型初始参数 |
| GET | `/admin/model-init-params/mode/{gameMode}` | 按模式查询模型初始参数 |
| PATCH | `/admin/model-init-params/{id}` | 更新模型初始参数 |
| GET | `/admin/game-mode-configs` | 管理游戏模式配置 |
| PATCH | `/admin/game-mode-configs/{id}` | 更新游戏模式配置 |

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

分页参数在各函数中多为 `page`、`pageSize`，部分接口另有筛选字段（与各服务文件中 `params` 类型一致）。

---

## 12.1 群体智能实验室 `/lab/*`

无需登录：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/lab/algorithms` | 已启用算法，含默认参数 |
| GET | `/lab/presets` | 系统预设和公开预设，查询参数 `algorithmId` |

需要登录：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/lab/presets/mine` | 当前用户预设 |
| POST | `/lab/presets` | `{ algorithmId, name, params, isPublic }`，服务端校验参数区间 |
| PUT | `/lab/presets/{id}` | 只能改自己的预设 |
| DELETE | `/lab/presets/{id}` | 只能删自己的预设 |

后台（管理员，经 admin-svc 代理）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/lab/algorithms` | 全部算法 |
| PUT | `/admin/lab/algorithms/{id}` | 启停、排序、默认参数 |
| GET | `/admin/lab/presets` | 系统预设 |
| POST | `/admin/lab/presets` | 新建系统预设 |
| DELETE | `/admin/lab/presets/{id}` | 删除系统预设 |

迁移脚本：`typingGame/DB/006-lab.sql`。

## 13. 微服务路由映射

前端所有请求通过 API Gateway（`http://localhost:8080`）统一入口，按路径前缀路由到对应微服务：

| 路径前缀 | 目标微服务 | 端口 |
|----------|-----------|------|
| `/api/v1/auth/*` | user-svc | 8081 |
| `/api/v1/user/*` | user-svc | 8081 |
| `/api/v1/profiles/*` | user-svc | 8081 |
| `/api/v1/friends/*` | user-svc | 8081 |
| `/api/v1/messages/*` | chat-svc | 8084 |
| `/api/v1/leaderboard/*` | chat-svc | 8084 |
| `/api/v1/game/*` | game-core-svc | 8082 |
| `/api/v1/stats/*` | game-core-svc | 8082 |
| `/api/v1/dictionary/*` | chat-svc | 8084（兼容历史路径） |
| `/api/v1/dictionaries/*` | chat-svc | 8084 |
| `/api/v1/ai/*` | ai-svc | 8083 |
| `/api/v1/admin/*` | admin-svc | 8085（代理到各服务 `/internal/admin/*`） |
| `/api/v1/lab/*` | lab-svc | 8086 |

---

## 14. 前端封装索引

所有上述 REST 调用均通过 **`apiClient`** 发起；新增接口时请同步修改对应服务文件与本文件。

服务文件入口：`src/db/api.ts`（聚合导出所有服务）

---

## 15. 修订说明

- 前端数据面已统一为 **Java `/api/v1`**，不再使用 Supabase 客户端。
- API 服务层已按业务域拆分为多个文件，替代原单一 `api.ts`。
- 后端已迁移至微服务架构，前端通过 API Gateway 统一访问各微服务。
- `/admin/**` 已独立到 `admin-svc`，并通过内部 API 调用领域服务；各领域服务内部管理接口已按 Controller → InternalAdminService 分层，内部管理 Service 已去除直接 `EntityManager` 查询，改为 Repository-backed 实现。
- 若后端路径或字段与表内不符，以运行中的 Java API 为准，并建议同时更正对应服务文件与本文件。

**文档更新**：2026-07-02
