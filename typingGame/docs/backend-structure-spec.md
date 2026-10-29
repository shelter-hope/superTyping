# 后端结构与开发规范（P0 基线）

> 生效日期：2026-10-30
> 配套方案：[后端结构审计与改造方案](backend-structure-audit-and-plan.md)
> 关系：本文是**唯一权威**的目录/分层/配置规范；`项目公共约束.md §2` 与本文件冲突时以本文件为准。

## 1. 适用范围

`typingGame/` 下所有 Maven 模块：1 个父 POM、2 个公共库（`libs/kernel`、`libs/security`）、8 个可启动服务（api-gateway、user-svc、game-core-svc、ai-svc、chat-svc、dict-svc、lab-svc、admin-svc）。

## 2. 目录布局（目标态）

```
typingGame/
├── pom.xml                     # 聚合父 POM（packaging=pom）
├── libs/                       # 只放库，不放可启动服务（P1 已落地）
│   ├── kernel/                 # 实体、DTO、枚举、工具、VO、ResultMessage/BusinessException、全局异常、Admin 过滤器、IM 契约
│   ├── security/               # JwtTokenProvider、JwtAuthenticationFilter、TokenRevocation
│   └── testing/                # 测试基类（可选）
├── services/                   # 8 个可启动服务（P2 迁移完成后使用；当前仍在根目录）
├── deploy/
│   ├── docker-compose.yml      # 中间件 + 服务
│   ├── docker/Dockerfile       # 通用服务镜像（ARG SERVICE）
│   └── gateway/                # 网关路由与 Nacos 覆盖说明
├── db/<svc>/                   # Flyway 迁移脚本（P4）
├── docs/
└── DB/                         # 历史手工 SQL（P4 后归档只读）
```

**过渡态说明**：P0–P1 期间服务仍直接位于 `typingGame/` 根下（与现在一致），P2 完成后再整体移动到 `services/`。任何新模块都必须先决定自己属于 `libs/` 还是 `services/`。

## 3. 服务内部分层（统一 7 包模板）

每个服务固定使用以下 7 个包，**不多不少**，包名按职责划分，不按领域切分：

```
com.typinggame.<svc>/
├── <Svc>Application.java
├── controller/     # HTTP 入口：参数校验、取登录态、调 service、返回 dto
├── service/        # 业务逻辑、事务边界、跨 repository 编排
│   └── impl/       # service 实现（有接口就必须放在这里）
├── repository/     # Spring Data JPA 接口，只做数据访问
├── entity/         # 本服务拥有的 JPA 实体，@Entity 必须在本包内
├── dto/            # 本服务的请求/响应对象
├── config/         # @Configuration、Bean、SecurityConfig、WebMvc 配置
└── support/        # 服务内部支撑：client（跨服务调用）、exception、mapper、util
```

### 3.1 各包职责与禁止事项

| 包 | 放什么 | 禁止什么 |
|---|---|---|
| `controller` | `@RestController`、`@RequestMapping`、参数校验、返回 dto | 直接依赖 `repository`；返回 `entity`；写业务分支 |
| `service` | 业务逻辑、事务、编排 | 依赖 `controller`；吞异常不记录 |
| `repository` | `JpaRepository`、`@Query` | 依赖 `service`/`controller`；写业务判断 |
| `entity` | `@Entity` 类、表映射 | 出现在 controller 签名里；当作接口返回体 |
| `dto` | request/response record 或 VO | 携带 JPA 注解；跨服务复用 |
| `config` | Bean、Security、Web、Jackson 配置 | 业务逻辑 |
| `support` | `*Client`（跨服务调用）、服务内异常映射、mapper | 领域逻辑的公共出口（公共逻辑放 `service`） |

### 3.2 依赖方向（允许）

```
controller → service → repository → entity
     ↓           ↓
    dto        support（client / exception / mapper）
config → 任意（仅声明 Bean）
entity ← 仅 repository / service / config / support
```

反方向一律禁止；由 ArchUnit 规则在 `mvn test` 中强制。

## 4. 已落地的 ArchUnit 护栏

每个服务（含 api-gateway）都有 `src/test/java/com/typinggame/archtest/ArchitectureTest.java`，扫描 `com.typinggame` 并强制以下规则（规则通过 `allowEmptyShould` 容忍「本服务没有该层」的情况）：

1. `controller` 不得依赖 `repository`；
2. `service` 不得依赖 `controller`；
3. `repository` 不得依赖 `service` / `controller`；
4. 所有 `@Entity` 类必须位于 `..entity` 包（`api-gateway` 不引入 JPA，其版本不含本条，其余 7 个服务完全相同）。

后续阶段追加（见第 9 节）：
- P2：`controller` 不得依赖 `entity`（存量违规归零后改为严格规则）；
- P2：禁止跨服务 `com.typinggame.<其它服务>` 编译期依赖（由独立 `arch-tests` 模块检查，因为单服务 classpath 看不到其它服务）；
- P3：禁止 `common` 包被新代码引用。

## 5. 配置规范（所有服务一致）

| 文件 | 内容 | 是否随 profile 变化 |
|---|---|---|
| `application.yml` | 端口、context-path、服务名、`spring.profiles.default: local`、`config.import`、服务自身业务配置项 | 否 |
| `application-local.yml` | 本地连接：MySQL、Redis、Nacos discovery 地址（带 `${ENV:默认值}`） | 是 |
| `application-dev.yml` | 开发/联调环境差异（**只写与 local 不同的键**） | 是 |
| `config/config.properties` | Nacos namespace/ip、JWT、Redis database、安全开关、日志级别、management | 否（可用 `${ENV:默认}`） |
| `config/jdbc.properties` | 数据源类型、Druid 参数、JPA ddl-auto、方言 | 否 |

规则：

- 不允许把连接串、密码、JWT secret 内联在 `application.yml`；
- 不允许某服务单独使用「全部内联」的配置方式（`lab-svc` 已在 P0 统一）；
- profile 文件不重复整段路由/整段 datasource，只写差异；
- 新增环境变量必须带默认值（`local` 可直接启动）；
- 安全白名单固定：`/actuator/health`、`/actuator/health/**`、`/actuator/info` 放行，其余 actuator 端点不放行；`management.endpoints.web.exposure.include` 只开 `health,info`（`config/config.properties` 已统一），不得写成 `/actuator/**` 全放行。

## 6. 数据边界

- **一张表只有一个写归属服务**（归属表见方案第 3.4 节）。其它服务只能通过 API 读，禁止 `@EntityScan` 其它服务的实体、禁止在别的服务写 `@Query` 更新该表；
- 迁移期（P0–P2）禁止新增跨服务实体引用；新增跨服务读取必须走 `support/client`；
- 跨服务调用统一用 `support/client/` 下的 `*Client`：显式超时（连接 1s / 读 3s）、最多 1 次重试、失败返回降级值并记录日志；
- `ddl-auto` 只允许 `validate`（生产）或 `none`（本地可临时 `update`，但迁移脚本必须同步进 `DB/`）；
- 表结构变更必须同时提交 SQL 脚本（P4 起改为 Flyway）。

## 7. 网关与 API 契约

- 对外统一前缀 `/api/v1/**`，前端与网关之间的路径**不随内部目录调整而变化**；
- 过渡期网关仍使用 `PrefixPath=/<svc>` 与各服务 `context-path` 配对，P2 去掉 context-path 后改为直转；
- 路由表**只维护一份**（各服务的 `application.yml`），profile 文件只放地址与 CORS 默认值；
- 新接口必须补 `springdoc` 注解（P5 统一引入后可 `/v3/api-docs` 拉取）；
- 内部管理端点固定 `/api/v1/internal/admin/**`，由 `admin-svc` 聚合。

## 8. 测试要求

- 每个服务至少：1 个 `ArchitectureTest` + controller 层测试 + 关键 service 测试；
- `ai-svc` 当前 0 测试，P2 前补齐基础用例；
- 新增/修改接口必须带对应 service 单测或 controller 测试；
- 本地验收：`mvn -q -DskipTests compile` 全绿 + 改动模块 `mvn test` 通过 + 前端 `npm run build`。

## 9. 迁移状态（P0 完成项）

已完成：

- 本文档（规范权威）；
- 父 POM 统一 ArchUnit 版本与依赖；
- 8 个服务加入相同的 `ArchitectureTest`；
- `lab-svc` 配置拆分为 `application.yml` + `application-local.yml` + `config/config.properties` + `config/jdbc.properties`；
- 修复部署入口：删除根目录失效 `Dockerfile`，新增 `deploy/docker/Dockerfile`（`ARG SERVICE`）；`docker-compose.yml` 增加 `user-svc` 服务样板；
- 网关路由合并为单份 `application.yml`，local/dev 只保留地址与 CORS；
- `项目公共约束.md §2` 改为引用本文件。

后续：

- ~~**P1**：`common` 拆为 `libs/kernel` + `libs/security`~~（**已完成**：包名保持 `com.typinggame.*` 不变，7 个服务改为同时依赖 `typing-game-kernel` 与 `typing-game-security`；删除 `common/observability` 死代码与未被使用的 `SecurityConfigBase`）；
- **P2**：实体/DTO/repository 按表归属迁回各服务；`controller` 不得依赖 `entity` 归零；跨服务读改 API；
- **P3**：按第 3 节统一包结构，移动 `chat/application`、`game/rule`、`dict/dictionary` 等领域包到 `service`/`support`；
- **P4**：分 schema + Flyway + 每服务 Dockerfile + compose 全栈；
- **P5**：springdoc + 重写 `docs/backend-architecture.md`。

### 9.1 P2 进度（按域分刀，每刀一次提交）

已完成（实体归属 + 跨服务读写改走内部 api + 本模块编译/测试全绿）：

| 切片 | 迁到 | 服务侧配套改动 |
| --- | --- | --- |
| leaderboard | `com.typinggame.game.entity.LeaderboardEntry` | game-core 的 `@EntityScan` 增加自有实体包 |
| AI 消息 | `com.typinggame.ai.entity.AiChatMessage/AiConversation` | ai-svc 同上；其余服务已无旧 FQN 引用 |
| 聊天消息 | `com.typinggame.chat.entity.FriendChatMessage/UserConversation/ChatAttachment/SystemNotification` | chat-svc 同上；`FriendRelation` 因 user-svc 共用暂留 kernel |
| 用户配置/快照 | `com.typinggame.user.entity.UserAppConfig/UserInfoSnapshot` | user-svc 同上 |
| 词库 | `com.typinggame.dict.entity.CustomDictionary/SystemDictionary/DictionaryWord/SystemDictionaryWord` | dict-svc 新增内部接口 `POST /api/v1/internal/dictionaries/{ids,words,practice}`；game-core 删除 4 个 `Game*Dictionary*Repository` 与 `JpaWordSource`，改用 `HttpWordSource`（读）与 `DictionaryPracticeClient`（结算写回），`WordSource` 端口语义不变 |
| 好友关系 | `com.typinggame.user.entity.FriendRelation` | user-svc 新增 `GET /api/v1/internal/friends/ids`；chat-svc 删除 `FriendRelationRepository` 与 `JpaCanMessageAdapter`，改由 `UserSvcCanMessageAdapter`（鉴权）与 `UserSvcFriendsClient`（好友 ID 列表）远程获取，`chat.friends.auth-mode` 固定为 `user-svc` |
| 对局/配置/模型（仅 game-core 使用） | `com.typinggame.game.entity.` 下的 `GameRound`、`GameSession`、`MonsterAttempt`、`MonsterData`、`MonsterParentRelation`、`StringParent`、`ConfigTemplate`、`GameRuleConfig`、`SystemCharacterTypeConfig`、`SystemMonsterCommoConfig`、`SystemMonsterTypeConfig`、`SystemParam`、`ModelConstant`、`UserModelAdaptLog` | 纯机械迁移（仅 game-core 引用），`@EntityScan` 复用已加的 `com.typinggame.game.entity` |
| 对局记录 / 字符统计（ai-svc 只读） | `com.typinggame.game.entity.GameRecord`、`com.typinggame.game.entity.CharacterStats` | game-core 新增内部接口 `GET /api/v1/internal/player-data/{game-records,weak-chars}`（返回 kernel 的 `GameRecordSummaryDTO` / `WeakCharDTO`）；ai-svc 删除 `GameRecordRepository` 与 `CharacterStatsRepository`，改由 `GameCoreClient`（RestTemplate，2s/3s 超时，失败空结果 + WARN）读取 |

跨服务读改的统一约定（本轮固化）：

- 内部接口路径固定 `/api/v1/internal/**`，由目标服务 `SecurityConfig` 显式放行，网关侧对 `/internal/` 路径一律 404（`GatewayGuardFilter`），只允许服务间直连；
- game-core 的直连地址取 `@Value("${game.dict.base-url:http://dict-svc/dict-svc/api/v1/internal/dictionaries}")`，生产可通过环境变量覆盖；
- 读取类调用失败按「空结果 + WARN」降级，结算类写入失败只记录告警，不允许拖垮主流程；
- 每个切片都在本模块 `mvn -o test` 全绿后再进入下一刀；父 POM 全量 `mvn -o test` 为合并门禁。

剩余（按依赖顺序）：`SystemMonsterConfig`/`UserMonsterConfig`/`UserGameModeConfig`/`GameCharTypeConfig`/`SystemGameModeConfig`/`ModelInitParams`/`UserModelParams`（user-svc 的后台分页管理仍在直连这些表，目标是把管理端路由改到 game-core 的内部管理接口，并删除 user-svc 里的重复仓库）；之后是 `user_info` 及其 `stats` 子表（先拆 `UserModelAdaptLog.user`、`UserModelParams.user` 等 `@ManyToOne` 外键桩，再改内部 API）。


## 10. 提交与评审检查清单

- [ ] 新类放在 7 个包的正确位置，没有新增第 8 个包；
- [ ] controller 不返回 entity、不注入 repository；
- [ ] 没有新增跨服务实体引用；
- [ ] 配置只改对应文件，不内联连接串；
- [ ] 新增表/字段同时提交 SQL；
- [ ] `mvn -q -DskipTests compile` 通过，改动模块 `mvn test` 通过；
- [ ] `ArchitectureTest` 未被跳过或删除。
