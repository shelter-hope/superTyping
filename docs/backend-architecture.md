# 后端架构文档

## 一、系统概述

### 1.1 技术栈

| 类别 | 技术 | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| 语言 | Java | 22 | LTS 版本，性能稳定 |
| 框架 | Spring Boot | 3.3.x | 社区成熟，生态完善 |
| 微服务治理 | Spring Cloud | 2023.0.x | 服务注册发现、配置管理 |
| 服务注册 | Nacos | 2.3.x | 服务发现与配置中心 |
| API 网关 | Spring Cloud Gateway | - | 统一入口、路由转发 |
| 数据库 | MySQL | 8.0+ | 关系型数据库，事务支持 |
| 缓存 | Redis | 6.0+ | 缓存用户会话、排行榜数据 |
| 分布式锁 | Redisson | 3.26.x | Redis 分布式锁与缓存 |
| ORM | Spring Data JPA | - | 简化数据访问层开发 |
| 安全 | Spring Security + JWT | - | 用户认证与授权 |
| 实时通信 | Spring WebSocket | - | 实时聊天和游戏状态推送 |
| 消息队列 | RocketMQ | 2.3.x | 异步消息处理 |
| 对象映射 | MapStruct | 1.5.x | DTO/Entity 转换 |
| 链路追踪 | Zipkin | - | 分布式链路追踪 |

### 1.2 架构风格

- **架构模式**: 微服务架构 (Microservices Architecture)
- **设计原则**: 高内聚、低耦合、单一职责、依赖倒置
- **核心特性**: 动态难度调节、遗传进化算法、多游戏模式支持、服务高可用

---

## 二、微服务架构

### 2.1 服务架构图

```
┌─────────────────────────────────────────────────────────────┐
│                       API Gateway (8080)                    │
│                    context-path: /typingWord                │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
┌─────────────┐ ┌───────────────┐ ┌─────────────┐
│  User Svc   │ │ Game-Core Svc │ │    AI Svc   │
│   (8081)    │ │    (8082)     │ │    (8083)   │
│ ├─ Auth     │ │ ├─ GameCore   │ │ ├─ AIChat   │
│ ├─ User     │ │ ├─ GameRecord │ │ └─ Training │
│ └─ Friend   │ │ └─ Stats      │ │             │
└─────────────┘ └───────────────┘ └─────────────┘
        │              │              │
        ▼              ▼              ▼
┌─────────────┐ ┌───────────────┐ ┌─────────────┐
│  Chat Svc   │ │   Common      │ │             │
│   (8084)    │ │   Module      │ │             │
│ ├─ Message  │ │ ├─ Entity/DTO │ │             │
│ ├─ Leader   │ │ ├─ Security   │ │             │
│ └─ Dict     │ │ └─ Exception  │ │             │
└─────────────┘ └───────────────┘ └─────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    基础设施层                                │
│  Nacos(8848) | Redis(6379) | MySQL(3306) | RocketMQ        │
│  Zipkin(9411)                                               │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 服务列表

| 服务名称 | 端口 | 职责 | 状态 |
|----------|------|------|------|
| api-gateway | 8080 | 统一入口、路由转发 | ✅ |
| user-svc | 8081 | 用户认证、用户管理、好友管理 | ✅ |
| game-core-svc | 8082 | 游戏核心逻辑、游戏记录、自适应算法、统计分析 | ✅ |
| ai-svc | 8083 | AI 对话、训练建议、LLM 集成 | ✅ |
| chat-svc | 8084 | 消息中心、排行榜、词库管理 | ✅ |

---

## 三、项目结构

```
typingGame/
├── pom.xml                          # 聚合父项目，管理依赖版本
├── common/                          # 公共模块（共享代码）
│   ├── src/main/java/com/typinggame/
│   │   ├── common/                  # 公共组件
│   │   │   ├── config/              # 安全配置基类
│   │   │   ├── exception/           # 全局异常处理
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── security/            # 统一JWT安全组件
│   │   │       ├── JwtTokenProvider.java
│   │   │       └── JwtAuthenticationFilter.java
│   │   ├── dto/                     # 共享DTO（按功能分组）
│   │   ├── entity/                  # 共享实体（按功能分组）
│   │   ├── enums/                   # 共享枚举
│   │   ├── util/                    # 共享工具类
│   │   └── vo/                      # 共享视图对象
│   └── pom.xml
├── api-gateway/                     # API网关
│   ├── src/main/java/com/typinggame/gateway/
│   │   └── GatewayApplication.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── user-svc/                        # 用户服务
│   ├── src/main/java/com/typinggame/user/
│   │   ├── controller/              # 认证、用户、好友控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── game-core-svc/                   # 游戏核心服务
│   ├── src/main/java/com/typinggame/game/
│   │   ├── controller/              # 游戏、记录、统计控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── ai-svc/                          # AI服务
│   ├── src/main/java/com/typinggame/ai/
│   │   ├── controller/              # AI控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── chat-svc/                        # 社交聊天服务
│   ├── src/main/java/com/typinggame/chat/
│   │   ├── controller/              # 消息、排行榜、词库控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── DB/                              # 数据库脚本
├── docker-compose.yml               # 基础设施编排
└── pom.xml                          # 父项目Maven配置
```

---

## 四、API 路由

API 网关统一入口：`http://localhost:8080/typingWord`

| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| `/api/v1/auth/**` | user-svc | 注册、登录、令牌刷新 |
| `/api/v1/user/**` | user-svc | 用户资料、设置 |
| `/api/v1/friends/**` | user-svc | 好友管理 |
| `/api/v1/game/**` | game-core-svc | 游戏核心逻辑、记录 |
| `/api/v1/stats/**` | game-core-svc | 统计分析 |
| `/api/v1/ai/**` | ai-svc | AI 助手对话 |
| `/api/v1/messages/**` | chat-svc | 消息中心 |
| `/api/v1/leaderboard/**` | chat-svc | 排行榜 |
| `/api/v1/dictionaries/**` | chat-svc | 词库管理 |
| `/api/v1/dictionary/**` | chat-svc | 词库管理历史兼容路径 |

---

## 五、核心实体设计

### 5.1 用户实体 (UserInfo)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| userId | Integer | 主键，用户ID |
| username | String | 用户名，唯一 |
| password | String | 密码（加密存储） |
| nickname | String | 昵称 |
| signature | String | 个性签名 |
| avatar | String | 头像URL |
| bestScore | Integer | 最佳分数 |
| bestRound | Integer | 最佳回合数 |
| bestSpeed | Double | 最佳速度 (cpm) |
| bestAccuracy | Double | 最佳准确率 (%) |
| avgSpeed | Double | 平均速度 |
| avgAccuracy | Double | 平均准确率 |
| peakAccuracy | Double | 峰值准确率 |
| gameNum | Integer | 游戏次数 |
| role | String | 角色 (user/admin) |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### 5.2 游戏记录实体 (GameRecord)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| recordId | Integer | 主键，记录ID |
| userId | Integer | 用户ID，外键 |
| gameMode | String | 游戏模式 (endless/dictionary/limit) |
| selectedChars | String | 选择的字符类型 |
| roundNum | Integer | 回合数 |
| totalScore | Integer | 总分 |
| totalChars | Integer | 总字符数 |
| correctChars | Integer | 正确字符数 |
| avgSpeed | Double | 平均速度 |
| peakSpeed | Double | 峰值速度 |
| avgAccuracy | Double | 平均准确率 |
| peakAccuracy | Double | 峰值准确率 |
| gameDuration | Integer | 游戏时长 (秒) |
| createTime | LocalDateTime | 创建时间 |

### 5.3 怪物记录实体 (MonsterRecord)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| monsterId | Integer | 主键，怪物ID |
| recordId | Integer | 游戏记录ID，外键 |
| monsterType | String | 怪物类型 (NORMAL/ATTACKER/EVASIVE/SMART) |
| targetStr | String | 目标字符串 |
| speed | Double | 移动速度 |
| survivalTime | Double | 存活时间 |
| destroyCostTime | Double | 消灭耗时 |
| totalCount | Integer | 总输入次数 |
| inputErrorCount | Integer | 错误次数 |
| isTrueMean | Boolean | 是否被正确消灭 |
| score | Integer | 得分 |
| spawnWave | Integer | 生成波次 |
| isParent | Boolean | 是否为父本 |
| difficultyLevel | Integer | 难度等级 |

### 5.4 字符统计实体 (CharacterStats)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| statId | Integer | 主键 |
| userId | Integer | 用户ID，外键 |
| charValue | String | 字符 |
| totalInput | Integer | 总输入次数 |
| errorCount | Integer | 错误次数 |
| correctCount | Integer | 正确次数 |
| accuracy | Double | 准确率 |
| lastPracticeTime | LocalDateTime | 上次练习时间 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### 5.5 词库实体 (Dictionary)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| dictId | Integer | 主键 |
| dictName | String | 词库名称 |
| description | String | 描述 |
| wordCount | Integer | 单词数量 |
| dictType | String | 类型 (system/custom) |
| userId | Integer | 用户ID（自定义词库） |
| isPublic | Boolean | 是否公开 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

---

## 六、服务层设计

### 6.1 认证服务 (AuthService) - user-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| register | 用户注册 | UserRegisterDTO | UserResponseDTO |
| login | 用户登录 | UserLoginDTO | TokenResponseDTO |
| refreshToken | 刷新令牌 | String refreshToken | TokenResponseDTO |
| changePassword | 修改密码 | ChangePasswordDTO | void |

### 6.2 用户服务 (UserService) - user-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getUserById | 根据ID获取用户 | Integer userId | UserResponseDTO |
| getUserByUsername | 根据用户名获取用户 | String username | UserResponseDTO |
| updateUser | 更新用户信息 | Integer userId, UserUpdateDTO | UserResponseDTO |
| updateAvatar | 更新头像 | Integer userId, String avatarUrl | UserResponseDTO |
| getUserStats | 获取用户统计 | Integer userId | UserStatsDTO |

### 6.3 好友服务 (FriendService) - user-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| sendFriendRequest | 发送好友请求 | Integer userId, Integer targetUserId | void |
| acceptFriendRequest | 接受好友请求 | Integer userId, Integer requestUserId | void |
| rejectFriendRequest | 拒绝好友请求 | Integer userId, Integer requestUserId | void |
| getFriends | 获取好友列表 | Integer userId | List<FriendDTO> |
| getFriendRequests | 获取好友请求 | Integer userId | List<FriendRequestDTO> |
| deleteFriend | 删除好友 | Integer userId, Integer friendId | void |

### 6.4 游戏记录服务 (GameRecordService) - game-core-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| createGameRecord | 创建游戏记录 | GameRecordRequestDTO | GameRecordResponseDTO |
| getGameRecordById | 根据ID获取记录 | Integer recordId | GameRecordResponseDTO |
| getGameRecordsByUser | 获取用户游戏记录 | Integer userId, Pageable | Page<GameRecordResponseDTO> |

### 6.5 统计服务 (StatsService) - game-core-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| updateStats | 更新字符统计 | Integer userId, List<CharacterInputDTO> | void |
| getCharacterStats | 获取字符统计 | Integer userId | List<CharacterStatsDTO> |
| getErrorCharacters | 获取易错字符 | Integer userId | List<CharacterStatsDTO> |

### 6.6 游戏核心服务 (GameCoreService) - game-core-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getNextRoundConfig | 获取下一轮配置 | Integer userId, Integer roundNum, String gameMode | NextRoundResponseDTO |
| getCharTypes | 获取字符类型列表 | - | List<String> |

### 6.7 遗传进化服务 (EvolutionService) - game-core-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| selectParents | 选择父本怪物 | Integer recordId | List<MonsterRecord> |
| recombineAndMutate | 基因重组与变异 | List<MonsterRecord> parents, AdjustParam param | List<String> |
| generateNextRoundMonsters | 生成下一轮怪物 | Integer userId, AdjustParam param, String gameMode | List<MonsterDTO> |

### 6.8 AI 聊天服务 (AiChatService) - ai-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| chat | AI 对话 | Integer userId, String message | AiChatResponseDTO |
| getConversation | 获取对话记录 | Integer userId, Integer conversationId | List<AiChatMessageDTO> |

### 6.9 消息服务 (MessageService) - chat-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| sendMessage | 发送消息 | Integer userId, Integer conversationId, String content | MessageDTO |
| getConversationMessages | 获取会话消息 | Integer userId, Integer conversationId, Pageable | Page<MessageDTO> |
| createConversation | 创建会话 | Integer userId, Integer targetUserId | ConversationDTO |

### 6.10 排行榜服务 (LeaderboardService) - chat-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getLeaderboard | 获取排行榜 | String gameMode, Integer limit | List<LeaderboardDTO> |

### 6.11 词库服务 (DictionaryService) - chat-svc

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getSystemDictionaries | 获取系统词库 | - | List<DictionaryDTO> |
| getCustomDictionaries | 获取用户自定义词库 | Integer userId | List<DictionaryDTO> |
| createCustomDictionary | 创建自定义词库 | Integer userId, DictionaryCreateDTO | DictionaryDTO |
| updateDictionary | 更新词库 | Integer dictId, DictionaryUpdateDTO | DictionaryDTO |
| deleteDictionary | 删除词库 | Integer dictId | void |

---

## 七、控制层设计

### 7.1 认证控制器 (AuthController) - user-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/v1/auth/login | 用户登录 | 否 |
| POST | /api/v1/auth/signup | 用户注册 | 否 |
| POST | /api/v1/auth/refresh | 刷新令牌 | 否 |

### 7.2 用户控制器 (UserController) - user-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/v1/user/profile | 获取当前用户信息 | 是 |
| PUT | /api/v1/user/profile | 更新用户信息 | 是 |
| PUT | /api/v1/user/avatar | 更新头像 | 是 |

### 7.3 好友控制器 (FriendController) - user-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/v1/friends/request | 发送好友请求 | 是 |
| PUT | /api/v1/friends/request/{requestId}/accept | 接受好友请求 | 是 |
| PUT | /api/v1/friends/request/{requestId}/reject | 拒绝好友请求 | 是 |
| GET | /api/v1/friends | 获取好友列表 | 是 |
| GET | /api/v1/friends/requests | 获取好友请求 | 是 |
| DELETE | /api/v1/friends/{friendId} | 删除好友 | 是 |

### 7.4 游戏记录控制器 (GameRecordController) - game-core-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/v1/game/records | 创建游戏记录 | 是 |
| GET | /api/v1/game/records/{recordId} | 获取游戏记录 | 是 |
| GET | /api/v1/game/records/user/{userId} | 获取用户游戏记录列表 | 是 |

### 7.5 统计控制器 (StatsController) - game-core-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/v1/stats/character/user/{userId} | 获取用户字符统计 | 是 |
| POST | /api/v1/stats/character | 更新字符统计 | 是 |

### 7.6 游戏配置控制器 (GameConfigController) - game-core-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/v1/game/config/next-round | 获取下一轮配置 | 是 |
| GET | /api/v1/game/config/char-types | 获取字符类型 | 否 |

### 7.7 AI聊天控制器 (AiController) - ai-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/v1/ai/chat | AI对话 | 是 |
| POST | /api/v1/ai/training-summary | 训练摘要 | 是 |

### 7.8 消息控制器 (MessageController) - chat-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/v1/messages/conversations | 获取会话列表 | 是 |
| POST | /api/v1/messages/conversations | 创建会话 | 是 |
| GET | /api/v1/messages/conversations/{conversationId} | 获取会话消息 | 是 |
| POST | /api/v1/messages/send | 发送消息 | 是 |

### 7.9 排行榜控制器 (LeaderboardController) - chat-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/v1/leaderboard/{gameMode} | 获取排行榜 | 否 |

### 7.10 词库控制器 (DictionaryController) - chat-svc

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/v1/dictionaries/system | 获取系统词库 | 否 |
| GET | /api/v1/dictionaries/custom | 获取当前登录用户自定义词库 | 是 |
| POST | /api/v1/dictionaries/custom | 上传文件并创建自定义词库 | 是 |
| DELETE | /api/v1/dictionaries/custom/{id} | 删除当前登录用户自定义词库 | 是 |

---

## 八、安全设计

### 8.1 公共安全组件 (common 模块)

| 组件 | 文件 | 职责 |
| :--- | :--- | :--- |
| JwtTokenProvider | common/security/JwtTokenProvider.java | 令牌生成、验证、解析 |
| JwtAuthenticationFilter | common/security/JwtAuthenticationFilter.java | 请求拦截与认证 |
| GlobalExceptionHandler | common/exception/GlobalExceptionHandler.java | 全局异常处理 |
| BusinessException | common/exception/BusinessException.java | 业务异常封装 |

### 8.2 JWT认证流程

```
1. 用户登录请求 → POST /api/v1/auth/login
2. user-svc 验证用户名密码 → 生成accessToken和refreshToken
3. 前端存储token到localStorage
4. 后续请求在Header中携带Authorization: Bearer {token}
5. API网关转发请求到对应服务
6. 各服务JwtAuthenticationFilter验证token有效性
7. token过期返回401，前端自动清理token并跳转登录页
```

### 8.3 权限控制

| 角色 | 权限范围 |
| :--- | :--- |
| user | 访问自己的数据，游戏功能，好友系统 |
| admin | 访问所有数据，管理用户，管理词库 |

### 8.4 安全配置要点

- 密码使用BCrypt加密存储
- JWT令牌设置合理过期时间
- 刷新令牌存储在数据库中
- 配置CORS跨域策略
- 防止SQL注入（JPA参数化查询）
- 防止XSS攻击（前端过滤+后端验证）

---

## 九、WebSocket设计

### 9.1 实时消息

- **端点**: `/typingWord/ws`
- **订阅主题**: `/topic/messages/{conversationId}`
- **功能**: 实时接收好友消息

### 9.2 游戏状态推送

- **端点**: `/typingWord/ws`
- **订阅主题**: `/topic/game/{gameId}`
- **功能**: 推送游戏实时状态

---

## 十、缓存策略

### 10.1 Redis缓存应用

| 缓存内容 | Key格式 | 过期时间 | 说明 |
| :--- | :--- | :--- | :--- |
| 用户会话 | `session:{userId}` | 2小时 | 存储用户登录状态 |
| 排行榜 | `leaderboard:{gameMode}` | 10分钟 | 缓存排行榜数据 |
| 用户统计 | `stats:{userId}` | 5分钟 | 缓存用户游戏统计 |
| 词库列表 | `dictionary:system` | 30分钟 | 缓存系统词库 |

---

## 十一、事务管理

### 11.1 事务策略

| 场景 | 事务隔离级别 | 说明 |
| :--- | :--- | :--- |
| 用户注册 | DEFAULT | 创建用户时保证原子性 |
| 游戏结算 | DEFAULT | 保存记录和更新统计需同时完成 |
| 好友操作 | DEFAULT | 好友关系变更需双方同步 |
| 消息发送 | DEFAULT | 创建消息和更新会话需同时完成 |

### 11.2 事务边界

- 服务层方法使用`@Transactional`注解
- 跨服务调用使用分布式事务（如需）
- 事务回滚点设置

---

## 十二、部署配置

### 12.1 各服务 application.yml 关键配置

#### API Gateway

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
      routes:
        - id: user-svc
          uri: lb://user-svc
          predicates:
            - Path=/api/v1/auth/**,/api/v1/user/**,/api/v1/friends/**
        - id: game-core-svc
          uri: lb://game-core-svc
          predicates:
            - Path=/api/v1/game/**,/api/v1/stats/**
        - id: ai-svc
          uri: lb://ai-svc
          predicates:
            - Path=/api/v1/ai/**
        - id: chat-svc
          uri: lb://chat-svc
          predicates:
            - Path=/api/v1/messages/**,/api/v1/leaderboard/**,/api/v1/dictionary/**,/api/v1/dictionaries/**
```

#### 用户服务 (user-svc)

```yaml
server:
  port: 8081

spring:
  application:
    name: user-svc
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
  datasource:
    url: jdbc:mysql://localhost:3306/typing_game?useSSL=false&serverTimezone=Asia/Shanghai
    username: admin
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

jwt:
  secret: H5FQLg1tDHnqxGXoNhAKDvSwQfkQLDa4W+I72dPruXE=
```

#### 游戏核心服务 (game-core-svc)

```yaml
server:
  port: 8082

spring:
  application:
    name: game-core-svc
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

#### AI服务 (ai-svc)

```yaml
server:
  port: 8083

spring:
  application:
    name: ai-svc
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

#### 聊天服务 (chat-svc)

```yaml
server:
  port: 8084

spring:
  application:
    name: chat-svc
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

---

## 十三、扩展规划

| 扩展方向 | 说明 |
| :--- | :--- |
| 服务监控 | 集成 Prometheus + Grafana |
| 分布式追踪 | 完善 Zipkin 链路追踪 |
| 服务熔断 | 集成 Hystrix 或 Resilience4j |
| 容器化部署 | 使用 Docker 和 Kubernetes |
| 灰度发布 | 实现服务灰度发布机制 |
