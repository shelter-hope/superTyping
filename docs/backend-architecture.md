# 后端架构文档

## 一、系统概述

### 1.1 技术栈

| 类别 | 技术 | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| 语言 | Java | 21 | LTS 版本，性能稳定 |
| 框架 | Spring Boot | 3.x | 社区成熟，生态完善 |
| 数据库 | MySQL | 8.0+ | 关系型数据库，事务支持 |
| 缓存 | Redis | 7.0+ | 缓存用户会话、排行榜数据 |
| ORM | Spring Data JPA | 3.x | 简化数据访问层开发 |
| 安全 | Spring Security + JWT | - | 用户认证与授权 |
| 实时通信 | Spring WebSocket | - | 实时聊天和游戏状态推送 |
| API文档 | SpringDoc OpenAPI | - | 自动生成API文档 |

### 1.2 架构风格

- **架构模式**: 集成式单体应用 (Integrated Monolith)，采用分层架构设计
- **设计原则**: 高内聚、低耦合、单一职责、依赖倒置
- **核心特性**: 动态难度调节、遗传进化算法、多游戏模式支持

---

## 二、项目结构

```
typingGame/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/typinggame/
│       │       ├── controller/          # REST API控制层
│       │       │   ├── AuthController.java
│       │       │   ├── UserController.java
│       │       │   ├── GameRecordController.java
│       │       │   ├── CharacterStatsController.java
│       │       │   ├── DictionaryController.java
│       │       │   ├── GameConfigController.java
│       │       │   ├── LeaderboardController.java
│       │       │   ├── FriendController.java
│       │       │   ├── MessageController.java
│       │       │   └── AiChatController.java
│       │       ├── service/             # 业务逻辑层
│       │       │   ├── AuthService.java
│       │       │   ├── UserService.java
│       │       │   ├── GameRecordService.java
│       │       │   ├── CharacterStatsService.java
│       │       │   ├── DictionaryService.java
│       │       │   ├── GameConfigService.java
│       │       │   ├── EvolutionService.java
│       │       │   ├── LeaderboardService.java
│       │       │   ├── FriendService.java
│       │       │   ├── MessageService.java
│       │       │   └── AiChatService.java
│       │       ├── repository/          # 数据访问层
│       │       │   ├── UserRepository.java
│       │       │   ├── GameRecordRepository.java
│       │       │   ├── MonsterRecordRepository.java
│       │       │   ├── CharacterStatsRepository.java
│       │       │   ├── DictionaryRepository.java
│       │       │   ├── AdaptiveLogRepository.java
│       │       │   ├── FriendRepository.java
│       │       │   ├── MessageRepository.java
│       │       │   ├── ConversationRepository.java
│       │       │   └── AiChatConversationRepository.java
│       │       ├── entity/              # 数据库实体
│       │       │   ├── User.java
│       │       │   ├── GameRecord.java
│       │       │   ├── MonsterRecord.java
│       │       │   ├── CharacterStats.java
│       │       │   ├── Dictionary.java
│       │       │   ├── AdaptiveLog.java
│       │       │   ├── Friend.java
│       │       │   ├── Message.java
│       │       │   ├── Conversation.java
│       │       │   └── AiChatConversation.java
│       │       ├── dto/                 # 数据传输对象
│       │       │   ├── request/         # 请求DTO
│       │       │   └── response/        # 响应DTO
│       │       ├── config/              # 配置类
│       │       │   ├── SecurityConfig.java
│       │       │   ├── WebSocketConfig.java
│       │       │   ├── RedisConfig.java
│       │       │   └── OpenApiConfig.java
│       │       ├── security/            # 安全相关
│       │       │   ├── JwtTokenProvider.java
│       │       │   ├── JwtAuthenticationFilter.java
│       │       │   └── UserDetailsServiceImpl.java
│       │       ├── util/                # 工具类
│       │       │   ├── DifficultyCalculationUtil.java
│       │       │   ├── GameCoreUtil.java
│       │       │   ├── PasswordUtil.java
│       │       │   └── JwtUtil.java
│       │       ├── exception/           # 异常处理
│       │       │   ├── GlobalExceptionHandler.java
│       │       │   └── BusinessException.java
│       │       ├── websocket/           # WebSocket相关
│       │       │   ├── ChatWebSocketHandler.java
│       │       │   └── GameStatusWebSocketHandler.java
│       │       ├── enums/               # 枚举类
│       │       │   ├── GameMode.java
│       │       │   ├── MonsterType.java
│       │       │   ├── DifficultyLevel.java
│       │       │   └── FriendStatus.java
│       │       └── TypingGameApplication.java  # 启动类
│       └── resources/
│           ├── application.yml          # 应用配置
│           └── schema.sql               # 数据库初始化脚本
└── pom.xml                              # Maven配置
```

---

## 三、核心实体设计

### 3.1 用户实体 (User)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| userId | Long | 主键，用户ID |
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

### 3.2 游戏记录实体 (GameRecord)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| recordId | Long | 主键，记录ID |
| userId | Long | 用户ID，外键 |
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

### 3.3 怪物记录实体 (MonsterRecord)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| monsterId | Long | 主键，怪物ID |
| recordId | Long | 游戏记录ID，外键 |
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

### 3.4 字符统计实体 (CharacterStats)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| statId | Long | 主键 |
| userId | Long | 用户ID，外键 |
| charValue | String | 字符 |
| totalInput | Integer | 总输入次数 |
| errorCount | Integer | 错误次数 |
| correctCount | Integer | 正确次数 |
| accuracy | Double | 准确率 |
| lastPracticeTime | LocalDateTime | 上次练习时间 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### 3.5 词库实体 (Dictionary)

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| dictId | Long | 主键 |
| dictName | String | 词库名称 |
| description | String | 描述 |
| wordCount | Integer | 单词数量 |
| dictType | String | 类型 (system/custom) |
| userId | Long | 用户ID（自定义词库） |
| isPublic | Boolean | 是否公开 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

---

## 四、服务层设计

### 4.1 认证服务 (AuthService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| register | 用户注册 | UserRegisterDTO | UserResponseDTO |
| login | 用户登录 | UserLoginDTO | TokenResponseDTO |
| refreshToken | 刷新令牌 | String refreshToken | TokenResponseDTO |
| changePassword | 修改密码 | ChangePasswordDTO | void |

### 4.2 用户服务 (UserService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getUserById | 根据ID获取用户 | Long userId | UserResponseDTO |
| getUserByUsername | 根据用户名获取用户 | String username | UserResponseDTO |
| updateUser | 更新用户信息 | Long userId, UserUpdateDTO | UserResponseDTO |
| updateAvatar | 更新头像 | Long userId, String avatarUrl | UserResponseDTO |
| getUserStats | 获取用户统计 | Long userId | UserStatsDTO |
| deleteUser | 删除用户 | Long userId | void |

### 4.3 游戏记录服务 (GameRecordService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| createGameRecord | 创建游戏记录 | GameRecordRequestDTO | GameRecordResponseDTO |
| getGameRecordById | 根据ID获取记录 | Long recordId | GameRecordResponseDTO |
| getGameRecordsByUser | 获取用户游戏记录 | Long userId, Pageable | Page<GameRecordResponseDTO> |
| deleteGameRecord | 删除游戏记录 | Long recordId | void |

### 4.4 字符统计服务 (CharacterStatsService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| updateStats | 更新字符统计 | Long userId, List<CharacterInputDTO> | void |
| getCharacterStats | 获取字符统计 | Long userId | List<CharacterStatsDTO> |
| getErrorCharacters | 获取易错字符 | Long userId | List<CharacterStatsDTO> |

### 4.5 词库服务 (DictionaryService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getSystemDictionaries | 获取系统词库 | - | List<DictionaryDTO> |
| getCustomDictionaries | 获取用户自定义词库 | Long userId | List<DictionaryDTO> |
| createCustomDictionary | 创建自定义词库 | Long userId, DictionaryCreateDTO | DictionaryDTO |
| updateDictionary | 更新词库 | Long dictId, DictionaryUpdateDTO | DictionaryDTO |
| deleteDictionary | 删除词库 | Long dictId | void |

### 4.6 游戏配置服务 (GameConfigService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getNextRoundConfig | 获取下一轮配置 | Long userId, Integer roundNum, String gameMode | NextRoundResponseDTO |
| getCharTypes | 获取字符类型列表 | - | List<String> |

### 4.7 遗传进化服务 (EvolutionService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| selectParents | 选择父本怪物 | Long recordId | List<MonsterRecord> |
| recombineAndMutate | 基因重组与变异 | List<MonsterRecord> parents, AdjustParam param | List<String> |
| generateNextRoundMonsters | 生成下一轮怪物 | Long userId, AdjustParam param, String gameMode | List<MonsterDTO> |

### 4.8 排行榜服务 (LeaderboardService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| getLeaderboard | 获取排行榜 | String gameMode, Integer limit | List<LeaderboardDTO> |

### 4.9 好友服务 (FriendService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| sendFriendRequest | 发送好友请求 | Long userId, Long targetUserId | void |
| acceptFriendRequest | 接受好友请求 | Long userId, Long requestUserId | void |
| rejectFriendRequest | 拒绝好友请求 | Long userId, Long requestUserId | void |
| getFriends | 获取好友列表 | Long userId | List<FriendDTO> |
| getFriendRequests | 获取好友请求 | Long userId | List<FriendRequestDTO> |
| deleteFriend | 删除好友 | Long userId, Long friendId | void |

### 4.10 消息服务 (MessageService)

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| sendMessage | 发送消息 | Long userId, Long conversationId, String content | MessageDTO |
| getConversationMessages | 获取会话消息 | Long userId, Long conversationId, Pageable | Page<MessageDTO> |
| createConversation | 创建会话 | Long userId, Long targetUserId | ConversationDTO |

---

## 五、控制层设计

### 5.1 认证控制器 (AuthController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录 | 否 |
| POST | /api/auth/refresh | 刷新令牌 | 否 |
| POST | /api/auth/change-password | 修改密码 | 是 |

### 5.2 用户控制器 (UserController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/users/{userId} | 获取用户信息 | 是 |
| PUT | /api/users/{userId} | 更新用户信息 | 是 |
| PUT | /api/users/{userId}/avatar | 更新头像 | 是 |
| GET | /api/users/{userId}/stats | 获取用户统计 | 是 |
| DELETE | /api/users/{userId} | 删除用户 | 管理员 |

### 5.3 游戏记录控制器 (GameRecordController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/game/records | 创建游戏记录 | 是 |
| GET | /api/game/records/{recordId} | 获取游戏记录 | 是 |
| GET | /api/game/records/user/{userId} | 获取用户游戏记录列表 | 是 |
| DELETE | /api/game/records/{recordId} | 删除游戏记录 | 是 |

### 5.4 字符统计控制器 (CharacterStatsController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/stats/character/user/{userId} | 获取用户字符统计 | 是 |
| POST | /api/stats/character | 更新字符统计 | 是 |

### 5.5 词库控制器 (DictionaryController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/dictionary/system | 获取系统词库 | 否 |
| GET | /api/dictionary/custom/user/{userId} | 获取用户自定义词库 | 是 |
| POST | /api/dictionary/custom | 创建自定义词库 | 是 |
| PUT | /api/dictionary/{dictId} | 更新词库 | 是 |
| DELETE | /api/dictionary/{dictId} | 删除词库 | 是 |

### 5.6 游戏配置控制器 (GameConfigController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/game/config/next-round | 获取下一轮配置 | 是 |
| GET | /api/game/config/char-types | 获取字符类型 | 否 |

### 5.7 排行榜控制器 (LeaderboardController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/leaderboard/{gameMode} | 获取排行榜 | 否 |

### 5.8 好友控制器 (FriendController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| POST | /api/friends/request | 发送好友请求 | 是 |
| PUT | /api/friends/request/{requestId}/accept | 接受好友请求 | 是 |
| PUT | /api/friends/request/{requestId}/reject | 拒绝好友请求 | 是 |
| GET | /api/friends | 获取好友列表 | 是 |
| GET | /api/friends/requests | 获取好友请求 | 是 |
| DELETE | /api/friends/{friendId} | 删除好友 | 是 |

### 5.9 消息控制器 (MessageController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/messages/conversations | 获取会话列表 | 是 |
| POST | /api/messages/conversations | 创建会话 | 是 |
| GET | /api/messages/conversations/{conversationId} | 获取会话消息 | 是 |
| POST | /api/messages/send | 发送消息 | 是 |

### 5.10 AI对话控制器 (AiChatController)

| HTTP方法 | 路径 | 功能 | 需要认证 |
| :--- | :--- | :--- | :--- |
| GET | /api/ai-chat/conversations | 获取对话列表 | 是 |
| POST | /api/ai-chat/conversations | 创建对话 | 是 |
| POST | /api/ai-chat/conversation/{conversationId}/send | 发送消息 | 是 |

---

## 六、DTO定义

### 6.1 请求DTO

#### 6.1.1 认证请求

```java
// UserRegisterDTO.java
public class UserRegisterDTO {
    private String username;
    private String password;
    private String nickname;
}

// UserLoginDTO.java
public class UserLoginDTO {
    private String username;
    private String password;
}

// ChangePasswordDTO.java
public class ChangePasswordDTO {
    private String oldPassword;
    private String newPassword;
}
```

#### 6.1.2 游戏请求

```java
// GameRecordRequestDTO.java
public class GameRecordRequestDTO {
    private String gameMode;
    private String selectedChars;
    private Integer roundNum;
    private Integer totalScore;
    private Integer totalChars;
    private Integer correctChars;
    private Double avgSpeed;
    private Double peakSpeed;
    private Double avgAccuracy;
    private Double peakAccuracy;
    private Integer gameDuration;
    private List<MonsterRecordDTO> monsters;
}

// NextRoundRequestDTO.java
public class NextRoundRequestDTO {
    private Integer roundNum;
    private String gameMode;
    private List<Long> customDictIds;
    private List<Long> systemDictIds;
    private Double currentDifficulty;
}
```

### 6.2 响应DTO

#### 6.2.1 认证响应

```java
// TokenResponseDTO.java
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserResponseDTO user;
}
```

#### 6.2.2 用户响应

```java
// UserResponseDTO.java
public class UserResponseDTO {
    private Long userId;
    private String username;
    private String nickname;
    private String signature;
    private String avatar;
    private Integer bestScore;
    private Integer bestRound;
    private Double bestSpeed;
    private Double bestAccuracy;
    private Double avgSpeed;
    private Double avgAccuracy;
    private Double peakAccuracy;
    private Integer gameNum;
    private String role;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### 6.2.3 游戏响应

```java
// NextRoundResponseDTO.java
public class NextRoundResponseDTO {
    private AdjustParam adjustParam;
    private List<MonsterDTO> monsters;
}

// AdjustParam.java
public class AdjustParam {
    private Double currentDifficulty;
    private Double monsterSpeed;
    private Integer monsterCount;
    private Double avgStringLength;
    private Double normalRatio;
    private Double attackRatio;
    private Double evasiveRatio;
    private Double smartRatio;
}
```

---

## 七、安全设计

### 7.1 JWT认证流程

```
1. 用户登录请求 → POST /api/auth/login
2. 后端验证用户名密码 → 生成accessToken和refreshToken
3. 前端存储token到localStorage
4. 后续请求在Header中携带Authorization: Bearer {token}
5. 后端过滤器验证token有效性
6. token过期前45分钟自动刷新
7. 验证失败返回401，前端跳转登录页
```

### 7.2 权限控制

| 角色 | 权限范围 |
| :--- | :--- |
| user | 访问自己的数据，游戏功能，好友系统 |
| admin | 访问所有数据，管理用户，管理词库 |

### 7.3 安全配置要点

- 密码使用BCrypt加密存储
- JWT令牌设置合理过期时间
- 刷新令牌存储在数据库中
- 配置CORS跨域策略
- 防止SQL注入（JPA参数化查询）
- 防止XSS攻击（前端过滤+后端验证）

---

## 八、WebSocket设计

### 8.1 实时消息

- **端点**: `/ws/messages`
- **订阅主题**: `/topic/messages/{conversationId}`
- **功能**: 实时接收好友消息

### 8.2 游戏状态推送

- **端点**: `/ws/game-status`
- **订阅主题**: `/topic/game/{gameId}`
- **功能**: 推送游戏实时状态

---

## 九、缓存策略

### 9.1 Redis缓存应用

| 缓存内容 | Key格式 | 过期时间 | 说明 |
| :--- | :--- | :--- | :--- |
| 用户会话 | `session:{userId}` | 2小时 | 存储用户登录状态 |
| 排行榜 | `leaderboard:{gameMode}` | 10分钟 | 缓存排行榜数据 |
| 用户统计 | `stats:{userId}` | 5分钟 | 缓存用户游戏统计 |
| 词库列表 | `dictionary:system` | 30分钟 | 缓存系统词库 |

---

## 十、事务管理

### 10.1 事务策略

| 场景 | 事务隔离级别 | 说明 |
| :--- | :--- | :--- |
| 用户注册 | DEFAULT | 创建用户时保证原子性 |
| 游戏结算 | DEFAULT | 保存记录和更新统计需同时完成 |
| 好友操作 | DEFAULT | 好友关系变更需双方同步 |
| 消息发送 | DEFAULT | 创建消息和更新会话需同时完成 |

### 10.2 事务边界

- 服务层方法使用`@Transactional`注解
- 跨服务调用使用分布式事务（如需）
- 事务回滚点设置

---

## 十一、部署配置

### 11.1 application.yml关键配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/typing_game?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: admin
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
  
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 10000ms

jwt:
  secret: typing-game-jwt-secret-key-2024
  access-token-expire-minutes: 60
  refresh-token-expire-days: 7
```

---

## 十二、扩展规划

| 扩展方向 | 说明 |
| :--- | :--- |
| 微服务拆分 | 将认证、游戏、统计等模块拆分为独立服务 |
| 分布式缓存 | 使用Redis Cluster提升缓存性能 |
| 消息队列 | 使用RabbitMQ异步处理游戏结算 |
| 多数据库 | 读写分离，主从复制 |
| 容器化部署 | 使用Docker和Kubernetes |