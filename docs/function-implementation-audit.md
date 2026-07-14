# 功能与数据库落实审计

更新时间：2026-07-14

本文按 `typingGame/DB/typing_word_game.sql`、后端微服务代码与 `app-64` 前端页面进行对齐，标记当前功能落实状态。

## 状态说明

| 状态 | 含义 |
|------|------|
| 已闭环 | 有表结构、后端接口/业务逻辑、前端调用或页面入口，主流程可运行 |
| 部分落实 | 有表和部分接口，但只读、管理后台代理、未接入核心流程或缺少前端入口 |
| 未落实 | 表或前端封装存在，但后端业务接口缺失或当前实现明确不可用 |

## 当前已具备的功能

| 功能域 | 主要表 | 前端入口 | 后端核心代码 | 状态 |
|--------|--------|----------|--------------|------|
| 登录、注册、会话恢复 | `user_info` | `LoginPage.tsx`、`RegisterPage.tsx`、`AuthContext.tsx` | `AuthController`、`AuthServiceImpl` | 已闭环 |
| 用户资料查看与编辑 | `user_info` | `ProfilePage.tsx`、`EditProfileDialog.tsx` | `UserController`、`UserServiceImpl.updateUser` | 已闭环 |
| 头像上传 | `user_info.avatar` | `uploadAvatar`、`UserAvatar` | `UserController.uploadAvatar`、`UserServiceImpl.uploadAvatar`、`WebConfig.addResourceHandlers` | 已闭环，本次补齐 |
| 用户应用配置 | `user_app_configs` | `saveUserConfig`、`getUserConfig`、`updateSoundSettings` | `UserConfigController`、`UserConfigServiceImpl` | 已闭环，本次补齐后端接口 |
| 好友申请与好友列表 | `friend_relation` | `FriendsPage.tsx` | `FriendController`、`FriendServiceImpl` | 已闭环 |
| 好友消息与会话 | `user_conversation`、`friend_chat_message` | `MessageCenterPage.tsx`、`ChatPage.tsx` | `MessageController`、`MessageServiceImpl` | 已闭环，当前以轮询/REST 为主 |
| 自定义词库上传与删除 | `custom_dictionaries`、`dictionary_words` | `DictionaryPage.tsx`、`PreparePage.tsx` | `DictionaryController`、`DictionaryServiceImpl` | 已闭环 |
| 系统词库列表与抽词 | `system_dictionaries`、`system_dictionary_words` | `PreparePage.tsx` | `DictionaryController.getSystemDictionaries`、`DictionaryWordSourceStrategy` | 已闭环，下一轮生成支持按所选系统词库抽词 |
| 游戏记录保存 | `game_record`、`monster_data` | `GamePage.tsx`、`ResultPage.tsx` | `GameRecordController`、`GameRecordServiceImpl.saveRecord` | 已闭环 |
| 排行榜 | `leaderboard_entry` | `LeaderboardPage.tsx` | `LeaderboardController`、`LeaderboardServiceImpl`、`GameRecordServiceImpl.syncLeaderboardAndUserSummary` | 已闭环 |
| 字符统计 | `character_stats` | 游戏结束/统计相关调用 | `CharacterStatsController`、`CharacterStatsServiceImpl` | 已闭环，但最近准确率仍用全量准确率近似 |
| 趋势分析与训练建议 | `game_record`、`character_stats` | `TrendsPage.tsx`、`AIAssistantPage.tsx` | `StatsController`、`StatsServiceImpl` | 已闭环，使用游戏记录聚合计算 |
| AI 对话 | `ai_conversation`、`ai_chat_message` | `AIAssistantPage.tsx` | `AiController`、`AiChatServiceImpl` | 已闭环，依赖当前 AI 服务实现质量 |
| 管理后台聚合 | 多数业务表 | `AdminPage.tsx` | `AdminProxyController`、各服务 `InternalAdminController` | 部分落实，主要是列表/部分更新，不是完整 CRUD |

## 表级落实情况

| 表名 | 设计目标 | 当前落实情况 | 主要代码 | 缺口 |
|------|----------|--------------|----------|------|
| `user_info` | 用户账号、资料、战绩摘要 | 已闭环 | `AuthServiceImpl`、`UserServiceImpl`、`GameRecordServiceImpl.syncLeaderboardAndUserSummary` | 平均值当前用最近一局覆盖，不是历史加权平均 |
| `user_daily_stats` | 玩家每日汇总 | 已闭环 | `GameRecordPostProcessServiceImpl.upsertDailyStats`、`UserServiceImpl.getDailyStats` | 游戏结算后按用户+日期累计汇总；当前唯一键仍按用户+日期，不区分模式 |
| `user_info_snapshot` | 用户历史快照 | 部分落实 | 管理后台只读 | 未有定时快照或游戏结算快照写入逻辑 |
| `character_stats` | 单字符正确率与错误次数 | 已闭环 | `CharacterStatsServiceImpl.batchUpdate` | `recent_accuracy` 尚未维护最近 10 次滑动窗口 |
| `character_type_configs` | 字符类型与正则 | 部分落实 | SQL 初始化；`EndlessAdaptiveWordSourceStrategy` 使用固定类型池 | 正则配置尚未作为运行时字符分类规则加载 |
| `game_record` | 对局结果 | 已闭环 | `GameRecordServiceImpl.saveRecord` | 缺少导出接口 `/game/export/{recordId}` 的后端实现 |
| `game_mode_configs` | 系统游戏模式参数 | 部分落实 | 注册初始化、管理后台列表/更新、`GameConfigServiceImpl.resolveCharTypeConfig` | 已用于定位用户模式字符配置，怪物数量等模式参数仍未完全表驱动 |
| `game_char_type_configs` | 用户按模式选择字符类型 | 已闭环 | `GameConfigServiceImpl.getCharTypes`、`GameConfigServiceImpl.updateCharTypes` | 已支持用户侧查询和持久化，后续可补管理后台编辑 |
| `monster_data` | 每局怪物数据 | 已闭环 | `GameRecordServiceImpl.saveRecord` | 下一轮父本选择未从历史 `monster_data` 查询实现 |
| `system_monster_configs` | 系统怪物基础配置 | 部分落实 | 管理后台列表 | 游戏生成逻辑未按该表动态读取分值、速度、描述 |
| `system_monster_type_configs` | 怪物类型开关与基础值 | 部分落实 | `GameConfigServiceImpl.buildMonsters` | 已接入怪物分值和启用配置，类型占比仍由模型输出 |
| `system_monster_commo_configs` | 怪物速度/长度边界 | 已闭环 | `DifficultyModelServiceImpl.calculate` | 用于限制速度和字符串长度边界 |
| `user_monster_configs` | 用户个性化怪物配置 | 部分落实 | 注册初始化、管理后台列表 | 用户侧无配置页面，生成逻辑未读取 |
| `user_game_mode_configs` | 用户个性化模式配置 | 部分落实 | 注册初始化、管理后台列表 | 游戏生成逻辑未读取 |
| `custom_dictionaries` | 用户自定义词库 | 已闭环 | `DictionaryServiceImpl` | 文件只按 UTF-8 文本解析，doc/docx 仍未实现 |
| `dictionary_words` | 自定义词库词条与熟练度 | 已闭环 | `DictionaryServiceImpl`、`DictionaryWordSourceStrategy`、`GameRecordPostProcessServiceImpl.updateDictionaryWordStats` | 自定义词库已按本局同词出现次数聚合回写练习次数、错误率、连续正确、熟练度；系统词库词条暂不回写用户个人熟练度 |
| `system_dictionaries` | 系统词库元数据 | 部分落实 | `DictionaryServiceImpl.getSystemDictionaries` | 管理后台只读，用户侧未按词库抽词 |
| `system_dictionary_words` | 系统词库词条 | 已闭环 | `DictionaryWordSourceStrategy` | 支持按所选系统词库抽词 |
| `leaderboard_entry` | 排行榜 | 已闭环 | `GameRecordServiceImpl.syncLeaderboardAndUserSummary`、`LeaderboardServiceImpl` | 排名字段 `ranking` 未持久化重算，当前可通过查询顺序展示 |
| `model_init_params` | 模型初始权重 | 已闭环 | `DifficultyModelServiceImpl.calculate` | 下一轮难度计算已读取权重，用户个性化权重尚未覆盖系统权重 |
| `user_model_params` | 用户个性化模型参数 | 已闭环 | `DifficultyModelServiceImpl.calculate`、`GameRecordPostProcessServiceImpl.updateUserModelAndLog` | 生成下一轮优先使用用户参数，结算后更新最近难度和轮次 |
| `user_model_adapt_logs` | 模型调参日志 | 已闭环 | `GameRecordPostProcessServiceImpl.writeAdaptLog`、管理后台趋势读取 | 每局结算写入调参前后难度、触发规则和关键参数 |
| `model_constants` | 数学模型常量 | 已闭环 | `DifficultyModelServiceImpl.calculate` | 已用于最大参考速度、参考峰值速度兜底 |
| `system_params` | 系统参数 | 部分落实 | 管理后台列表 | 未建立统一参数读取服务或缓存 |
| `user_app_configs` | 用户 UI/应用配置 | 已闭环 | `UserConfigController`、`UserConfigServiceImpl` | 前端设置页目前只使用本地 `GameConfigContext`，未主动加载远端配置 |
| `string_parents` | 下一轮父本记录 | 已闭环 | `GameRecordPostProcessServiceImpl.updateStringParents`、`EndlessAdaptiveWordSourceStrategy.loadParentStrings` | 结算后沉淀本局高价值怪物作为父本，下一轮生成时优先加入父本字符串 |
| `friend_relation` | 好友关系 | 已闭环 | `FriendServiceImpl` | 黑名单状态未开放用户侧入口 |
| `user_conversation` | 好友会话 | 已闭环 | `MessageServiceImpl` | 置顶/免打扰字段未开放用户侧操作 |
| `friend_chat_message` | 好友消息 | 已闭环 | `MessageServiceImpl` | 撤回、表情、游戏邀请、战绩分享未完整实现 |
| `system_notification` | 系统通知 | 部分落实 | `MessageServiceImpl.getSystemMessages`、管理后台列表 | 缺少统一通知创建服务和已读/过期清理策略 |
| `ai_conversation` | AI 会话 | 已闭环 | `AiChatServiceImpl` | 会话置顶/免打扰未开放 |
| `ai_chat_message` | AI 消息 | 已闭环 | `AiChatServiceImpl` | 指令型消息执行状态仅基础保存 |

## 当前最主要缺口

1. 游戏自适应算法已经从 Controller 下沉到 `DifficultyModelServiceImpl`，并接入 `model_init_params`、`model_constants`、`system_monster_commo_configs`、`user_model_params`；结算后会更新用户模型并写入 `user_model_adapt_logs`。
2. 词库模式已按 `customDictIds`、`systemDictIds` 抽词，并校验自定义词库归属；自定义词库词条会在结算时回写练习次数、错误率、熟练度。
3. `user_daily_stats` 已在游戏结算后同步维护；`user_info_snapshot` 仍缺少定时或关键里程碑快照写入。
4. 用户个性化配置后端接口已补齐，但 `SettingsPage` 当前仍只操作本地上下文，尚未主动加载远端配置。
5. 管理后台多数资源是列表型管理：部分表支持更新，如模型参数；但怪物配置、系统词库、系统通知等仍缺新增/编辑/删除主流程。
6. 实时能力未完全落地：文档提到 WebSocket，当前聊天和消息主链路以 REST 为主。

## 本轮已补齐的核心实现

### 统一异常处理

核心代码：

- `common/common/exception/GlobalExceptionHandler`：统一处理业务异常、参数校验、权限异常、上传异常、未预期异常。
- `common/common/exception/BusinessException`：统一业务异常类型。
- `UserServiceApplication`、`GameCoreServiceApplication`、`ChatServiceApplication`、`AiServiceApplication`、`AdminServiceApplication`：显式扫描本服务包和 `com.typinggame.common`，JPA 服务显式声明本服务 Repository 与公共 Entity 扫描范围。
- 已删除 `user-svc` 内重复的局部 `GlobalExceptionHandler`，避免错误响应格式漂移。

### 头像上传

核心代码：

- `UserController.uploadAvatar`：校验当前登录用户只能上传自己的头像。
- `UserServiceImpl.uploadAvatar`：校验文件非空、大小不超过 2MB、类型为 jpg/png/webp/gif，保存到 `uploads/avatars`，更新 `user_info.avatar`。
- `WebConfig.addResourceHandlers`：将 `/uploads/**` 映射到本地上传目录。
- `api-gateway/application.yml`：将 `/uploads/**` 路由到 `user-svc`。
- `SecurityConfig`：放开 `/uploads/**` 资源读取，保证头像 `<img>` 能正常加载。

### 用户应用配置

核心代码：

- `UserConfigController`：提供 `GET /api/v1/user/config`、`POST /api/v1/user/config`、`PATCH /api/v1/user/sound-settings`。
- `UserConfigServiceImpl`：基于 `user_app_configs` 自动创建默认配置，支持白名单字段更新，限制字体大小、字符串长度、布尔字段类型。

### 游戏配置生产化重构

核心代码：

- `GameConfigController`：薄控制层，只负责 HTTP 参数与统一返回。
- `GameConfigServiceImpl`：统一编排字符配置、推荐配置、下一轮生成、怪物构造。
- `DifficultyModelServiceImpl`：读取 `model_init_params`、`model_constants`、`system_monster_commo_configs` 计算难度参数。
- `WordSourceStrategy`：词源策略接口，按游戏模式选择实现。
- `EndlessAdaptiveWordSourceStrategy`：无尽/限时模式按用户字符类型和易错字符生成训练字符串。
- `DictionaryWordSourceStrategy`：词库模式按 `dictionary_words`、`system_dictionary_words` 抽词，并校验自定义词库归属。

### 游戏结算后处理闭环

核心代码：

- `GameRecordPostProcessService`：游戏记录保存后的领域事件式后处理入口。
- `GameRecordPostProcessServiceImpl.upsertDailyStats`：写入 `user_daily_stats`。
- `GameRecordPostProcessServiceImpl.updateUserModelAndLog`：更新 `user_model_params` 并写入 `user_model_adapt_logs`。
- `GameRecordPostProcessServiceImpl.updateStringParents`：将高价值怪物 ID 写入 `string_parents`，并标记 `monster_data.is_parent`。
- `GameRecordPostProcessServiceImpl.updateDictionaryWordStats`：按同词出现次数聚合回写 `dictionary_words` 的练习次数、错误率、连续正确与熟练度。
- `GameRecordServiceImpl.saveRecord`：保存主记录和怪物后统一调用后处理服务，并把结算后难度返回给前端。
