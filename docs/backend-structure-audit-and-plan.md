# 后端结构审计与微服务规范化改造方案

> 审计对象：`typingGame/`（Spring Boot 3.3.13 / Java 22 / Nacos）
> 审计时间：2026-10-30
> 结论先行：**后端不是“有的不是微服务”，而是“8 个服务共用 1 个库、1 套全量实体、1 个上帝公共库”；部署上是 8 个进程，数据与代码边界上接近单体。**

> **执行状态（2026-10-30 更新）**
> - ✅ **P0 已完成**（提交 `8d5347e`）：规范文件、8 个服务的 ArchUnit 护栏、lab-svc 配置拆分、网关单份路由、`deploy/` 与 Dockerfile、compose 样板。
> - ✅ **P1 已完成**：`common` 拆为 `libs/kernel` + `libs/security`（包名不变），7 个服务改为双库依赖，删除死代码 `TraceIdFilter` 与未使用的 `SecurityConfigBase`。
> - ✅ **P2 已完成**（2026-10-30）：十一刀全部落地，实体已全部归位到各自服务，`libs/kernel` 只剩跨服务共享的 DTO / 枚举 / 工具；跨服务读取统一走 `/api/v1/internal/**`，`admin-svc` 对应路由已改指 game-core。逐刀明细见 `typingGame/docs/backend-structure-spec.md §9.1`。
> - ✅ **P3 已完成**（2026-10-30）：8 个模块全部收敛到 7 包模板（`controller / service(+impl) / repository / entity / dto / config / support`），实现类与测试同步归位，明细见 `typingGame/docs/backend-structure-spec.md §9.2`。
> - ⏭ 下一步 **P4**：分 schema + Flyway + 每服务 Dockerfile + compose 全栈。
> - 表结构基线已合并为单文件 `DB/typing_word_game.sql`（44 张表 / 42 外键 / 64 行种子），旧 001–014 增量脚本已删除。
> - 下文“一、现状盘点”记录的是 **P0 前的原始状态**，用于说明改造理由，不再代表当前代码。

---

## 一、现状盘点

### 1.1 模块清单（9 个 Maven 模块）

| 模块 | 类型 | 端口 | context-path | Java 文件 | 主代码行数 | 控制器 | 端点 | 测试类 |
|---|---|---|---|---|---|---|---|---|
| `common` | **库（jar，非服务）** | - | - | 107 | 4245 | 0 | 0 | 0 |
| `api-gateway` | 服务（网关） | 8080 | 无 | 3 | 112 | 0 | 0 | 1 |
| `user-svc` | 服务 | 8081 | `/user-svc` | 38 | 2026 | 6 | 37 | 5 |
| `game-core-svc` | 服务 | 8082 | `/game-core-svc` | 84 | 6222 | 10 | 69 | 10 |
| `ai-svc` | 服务 | 8083 | `/ai-svc` | 15 | 626 | 2 | 4 | 0 |
| `chat-svc` | 服务 | 8084 | `/chat-svc` | 42 | 2442 | 2 | 21 | 8 |
| `admin-svc` | 服务（BFF 聚合） | 8085 | `/admin-svc` | 9 | 454 | 2 | 2 | 2 |
| `lab-svc` | 服务 | 8086 | `/lab-svc` | 23 | 1265 | 2 | 18 | 3 |
| `dict-svc` | 服务 | 8087 | `/dict-svc` | 17 | 900 | 3 | 20 | 2 |
| 合计 | — | — | — | **338** | **约 18,300 行** | 27 | 171 | 31 |

另外仓库根下还有 **非 Maven 目录**：`DB/`（14 个手工 SQL + 1 个全量建库脚本）、`docs/`、`logs/`、`uploads/`、`target/`。

**“有的是微服务、有的是模块”的观感来源**：
1. `common` 是纯 jar 库，确实不是服务；
2. `DB/docs/logs/uploads/target` 是普通目录，不是 Maven 模块；
3. `common` 里混装了 6 个域的实体和 8 个域的 DTO，看起来像“模块堆”而不是“公共库”；
4. `game-core-svc` 84 个文件、69 个端点，体量是 ai-svc 的 10 倍，形态上更像单体模块而非微服务。

### 1.2 依赖关系

```
api-gateway ──(Nacos 路由)──▶ 8 个服务
user-svc   ┐
game-core  ├── 编译期唯一依赖：common
ai-svc     │
chat-svc   │
admin-svc  │   （运行期 HTTP 聚合：admin-svc ▶ 各服务 /internal/admin/**）
lab-svc    │   （运行期 HTTP：chat-svc ▶ user-svc；user-svc ▶ game-core-svc；ai-svc ▶ 部分服务）
dict-svc   ┘
```

- 编译期依赖**没有循环**，这是好的一面；
- 但**运行期通过共享 MySQL 表和共享实体强耦合**（见 1.4），比编译期依赖更难拆。

### 1.3 `common` 的内容构成（上帝库）

| 包 | 文件数 | 内容 | 问题 |
|---|---|---|---|
| `entity/game`、`entity/user`、`entity/message`、`entity/model`、`entity/config`、`entity/dictionaries`、`entity/leaderboard` | 40 个实体 / 40 张表 | 全部 JPA 实体 | 6 个域的实体放在一个库里，所有服务 `@EntityScan("com.typinggame.entity")` 全量加载 |
| `dto/**` | 50 个 DTO | game 16、stats 8、auth 4、ai 3、user 3、friend 2、message 2 等 | DTO 是“接口契约”，却按服务拆不开 |
| `common/security` | 3 | JwtAuthenticationFilter / JwtTokenProvider / TokenRevocationService | 合理，但应独立成安全库 |
| `common/exception`、`common/admin`、`common/im`、`common/config`、`common/observability` | 8 | 全局异常、管理端过滤、IM 契约、安全基类、TraceId 过滤器 | `TraceIdFilter` **0 处引用**（死代码）；`common/config` 仅 1 个基类 |
| `enums`、`util`、`vo` | 15 | 枚举、工具、统一返回 `ResultMessage` | 可下沉到 kernel 库 |

`common/pom.xml` 携带：`spring-boot-starter-web + data-jpa + security + data-redis + validation + jjwt + mysql-connector + druid + prometheus + logstash`。
→ **任何服务只要依赖 common，就注定是“Web + JPA + Security + Redis”全家桶**，无法按需裁剪。

### 1.4 数据边界现状（最关键的问题）

**所有 8 个服务连接同一个库** `typing_word_game`，且每个服务都配置了：

```yaml
spring.jpa.hibernate.ddl-auto=update   # 7 个服务和 lab-svc 内联配置里都有
```

**实体跨域使用矩阵**（import 次数）：

| 服务 | 使用的实体域 | 说明 |
|---|---|---|
| `game-core-svc` | game(37) config(26) model(22) user(17) dictionaries(7) leaderboard(4) | **直接读写 6 个域的 40 张表** |
| `user-svc` | user(16) config(8) game(5) message(4) model(3) | 读游戏/消息/模型表 |
| `chat-svc` | message(24) user(2) | 读用户表 |
| `ai-svc` | message(6) user(4) game(2) | 读消息/用户/游戏表 |
| `dict-svc` | dictionaries(22) | 唯一自洽（但仍与 game-core 共表） |
| `lab-svc` | （自有 3 实体） | 唯一“实体归一化”正确的服务 |
| `admin-svc` | （自有 1 实体 `admin_operation_log`） | BFF，符合定位 |

表总数 **44 张**：game 10、config 9、message 7、user 5、dictionaries 4、model 4、lab 3、leaderboard 1、admin 1。

**后果**：
- 改一张表/一个实体，要同步 7 个服务的代码与构建；
- 7 个服务同时 `ddl-auto=update` 抢改同一 schema，无迁移历史，出了问题无法定位是谁改的；
- 权限边界无法审计（任何服务代码都能写任何表）；
- 无法独立扩容/独立部署（表锁、连接池、事务互相影响）。

---

## 二、结构问题清单（按严重度）

### 2.1 内部目录结构：7 种写法

| 服务 | 包结构 | 差异点 |
|---|---|---|
| `user-svc` | `controller / service / service.impl / repository / config / security / im` | 有 interface+impl；无 dto/entity（用 common） |
| `game-core-svc` | `controller / service / service.impl / repository / rule / admin / word / config` | 算法包 `rule`、管理聚合包 `admin`、端口包 `word` 与分层包混排 |
| `ai-svc` | `controller / service / service.impl / repository / config` | 无 dto/entity |
| `chat-svc` | `controller / service / service.impl / repository / **application** / **friends** / **im** / **storage** / config` | 4 个领域包与分层包并存 |
| `admin-svc` | `controller / config / repository / service / entity` | **无 dto、无 impl**，service 为具体类 |
| `lab-svc` | `controller / service / repository / entity / dto / config / boids` | **唯一自带 entity+dto**，无 impl |
| `dict-svc` | `controller / service / service.impl / repository / **dictionary** / config` | 有 `dictionary` 领域包 |

**结论**：同一个项目里同时存在
- “接口 + Impl” 与 “只有具体 Service” 两种写法；
- “DTO/实体放 common” 与 “DTO/实体放本服务” 两种写法；
- “纯分层” 与 “分层 + 领域包混排” 两种写法；
- 测试目录不统一（`ai-svc` 0 测试，其余服务测试类分布在不同层）。

### 2.2 配置：4 套写法并存，且不可互换

| 服务 | application.yml | application-local/dev.yml | config/config.properties | config/jdbc.properties | 问题 |
|---|---|---|---|---|---|
| user / game-core / ai / chat / admin / dict | 有（端口、context-path、name、config.import） | 有（DB URL、CORS 等） | 有（nacos 地址、jwt、redis、日志级别） | 有（Druid、ddl-auto） | 4 处分散 |
| `lab-svc` | **全部内联**（DB/Redis/JWT/Nacos/日志） | **无** | **无** | **无** | 与其它服务完全不兼容 |
| `api-gateway` | 有 | local + dev 两份路由表**重复** | 有 | 无 | 路由表两份易漂移 |

其它配置问题：
- `spring.profiles.default: local` 在所有服务里写死；
- **没有启用 Nacos 配置中心**（只有 discovery），所以“改配置要重新打包”；
- 配置项命名不统一：`spring.datasource.url` 在 local yml，`spring.datasource.druid.*` 在 jdbc.properties。

### 2.3 网关与契约

- 网关路由写在 `application-local.yml` / `application-dev.yml` 两份文件里，靠 `PrefixPath=/user-svc` 与各服务 `server.servlet.context-path` 手工配对；**服务 context-path 一改，网关就 404**；
- 网关**没有** OpenAPI/契约，服务端也没有 `springdoc`（全仓库 0 依赖），接口文档靠手写 md；
- 文档已过时：`docs/backend-architecture.md` 仍画着 6 个服务、网关 context-path `/typingWord`，`docs/code-refactoring-plan.md §1.3` 还在描述单体目录（`src/main/java/com/typinggame/controller` 17 个控制器）；
- 内部管理端点 `/api/v1/internal/admin/**` 在 6 个服务里同名重复（`dict-svc` 里 `DictionaryAdminController` 与 `InternalAdminController` 还共用同一 base path）。

### 2.4 部署与数据库

- 根目录 `Dockerfile`：`COPY target/typingGame-0.0.1-SNAPSHOT.jar app.jar` —— **父 POM 是 pom packaging，这个 jar 不存在**，镜像构建必然失败；
- `docker-compose.yml` 只有中间件（Nacos/Zipkin/RocketMQ/Redis/MySQL），**没有任何服务容器**；
- 每个服务没有自己的 Dockerfile / healthcheck / 启动脚本；
- 没有 Flyway/Liquibase，迁移靠 `DB/` 下 14 个手工 SQL（`ddl-auto=update` 会在启动时偷偷改表）；
- 无 CI 约束（根目录无 `.github/workflows`、无 Jenkinsfile）。

### 2.5 规范与文档

`项目公共约束.md` 当前规定：
- `typingGame/common/` = “公共实体、DTO、工具与跨服务共享代码”（**这正是 1.3 上帝库的制度来源**）；
- 后端分层 = `controller / service / repository / entity / dto / vo / config`（**与实际 7 种写法不一致**）；
- 4.2 规定“新功能必须放对应领域服务，不允许堆到 game-core-svc”——但 `game-core-svc` 已经堆了 6 个域的表访问。

→ 改造必须**同时修改规范文档**，否则新代码会继续往 `common` 里加。

---

## 三、目标结构

### 3.1 物理布局

```
typingGame/
├── pom.xml                        # 聚合父 POM（pom packaging）
├── libs/                          # 只放库，不放可启动服务
│   ├── kernel/                    # ResultMessage、ErrorCode、BusinessException、GlobalExceptionHandler、TraceId、时钟/JSON 工具
│   ├── security/                  # JwtTokenProvider、JwtAuthenticationFilter、SecurityConfigBase、TokenRevocationService
│   └── testing/                   # 测试基类、Testcontainers 封装（可选）
├── services/
│   ├── api-gateway/
│   ├── user-svc/
│   ├── game-core-svc/
│   ├── ai-svc/
│   ├── chat-svc/
│   ├── dict-svc/
│   ├── lab-svc/
│   └── admin-svc/
├── deploy/
│   ├── docker-compose.yml         # 中间件 + 8 个服务
│   ├── docker/                    # 每服务 Dockerfile 或统一 Jib 配置
│   └── gateway/                   # 路由表（单份）+ Nacos 覆盖说明
├── db/
│   ├── user-svc/                  # Flyway: V1__baseline.sql ...
│   ├── game-core-svc/
│   ├── chat-svc/
│   ├── dict-svc/
│   ├── ai-svc/
│   ├── lab-svc/
│   └── admin-svc/
└── docs/
```

### 3.2 统一包模板（推荐：分层式，贴合现有规范，改动最小）

每个服务固定 7 个包，**不多不少**：

```
com.typinggame.<svc>/
├── <Svc>Application.java
├── controller/        # 只做 HTTP：入参校验、鉴权上下文、调用 service、返回 dto
├── service/           # 业务逻辑 + 事务边界
│   └── impl/          # 实现类（有接口就必须放这里；纯函数服务可省略接口但类仍放 service）
├── repository/        # 只做数据访问（Spring Data JPA）
├── entity/            # 本服务拥有的 JPA 实体（禁止 import 其它服务的 entity）
├── dto/               # 本服务的请求/响应对象（禁止放 libs）
├── config/            # @Configuration、Bean、SecurityConfig
└── support/           # 本服务内部支撑：client（跨服务调用）、exception（服务内异常映射）、mapper
```

规则（用 ArchUnit 固化）：
1. `controller` 只能依赖 `service`、`dto`、`support`；
2. `repository` 只能依赖 `entity`；
3. `service` 可以依赖 `repository`、`entity`、`dto`、`support`；
4. 任何类不得 import `com.typinggame.<其它服务>.*`；
5. `entity` 不得出现在 `controller` 的签名里；
6. 全仓库只允许 `libs.kernel` / `libs.security` 被跨服务共享。

> 备选：领域驱动模板 `api / application / domain / infrastructure`。更纯粹，但与现有 338 个文件、`项目公共约束.md`、团队习惯冲突更大，建议作为第二阶段之后的可选项，而不是第一步。

### 3.3 边界规则

| 规则 | 说明 |
|---|---|
| **表归属** | 每张表只允许一个服务写；其它服务只能通过 API 读，或通过只读视图（短期过渡） |
| **跨服务调用** | 走 `support/client` 的 RestClient + 服务名发现；必须设置超时、重试上限、降级返回值 |
| **admin-svc** | 保持 BFF：只做 `/api/v1/admin/**` 聚合与代理，不建领域表（`admin_operation_log` 除外） |
| **网关** | 路由表进仓库单份维护；服务去掉 `server.servlet.context-path`，由网关按 `/api/v1/**` 前缀直接转发 |
| **契约** | 每服务引入 `springdoc-openapi`，`/v3/api-docs` 可拉取；变更接口必须同步文档 |

### 3.4 表归属建议（44 张）

| 归属服务 | 表 |
|---|---|
| `user-svc` | `user_info`、`user_info_snapshot`、`user_app_configs`、`key_confusion` |
| `game-core-svc` | `game_record`、`game_round`、`game_session`、`monster_data`、`monster_attempt`、`monster_parent_relation`、`string_parents`、`system_monster_configs`、`user_monster_configs`、`user_game_mode_configs`、`game_rule_config`、`character_stats`、`user_daily_stats`、`user_model_params`、`user_model_adapt_logs`、`model_constants`、`model_init_params`、`leaderboard_entry`、`system_params`、`config_template`、`character_type_configs`、`game_mode_configs`、`system_monster_commo_configs`、`system_monster_type_configs` |
| `dict-svc` | `system_dictionaries`、`system_dictionary_words`、`custom_dictionaries`、`dictionary_words` |
| `chat-svc` | `friend_relation`、`friend_chat_message`、`user_conversation`、`chat_attachment`、`system_notification` |
| `ai-svc` | `ai_conversation`、`ai_chat_message` |
| `lab-svc` | `lab_algorithm`、`lab_preset`、`lab_run` |
| `admin-svc` | `admin_operation_log` |

**需要拆解的跨域读**（改造时必须显式处理）：
- `game-core-svc` 读 `dictionaries*`（抽词）→ 改为调用 `dict-svc` 的只读 API，或由 dict-svc 发布“抽词只读视图/缓存”；
- `user-svc` 读 `game*`/`message*`（注册引导、好友校验）→ 改为调用对应服务 API；
- `chat-svc`/`ai-svc` 读 `user_info` → 改为调用 `user-svc` 内部 API（缓存 60s）；
- `character_stats`/`user_daily_stats`/`key_confusion` 由 game-core 写、user-svc 统计读 → 明确写归属 game-core，读走 API 或只读视图。

### 3.5 数据与部署标准

- 数据：**先同实例分 schema**（`typing_game_user`、`typing_game_core`、…），每个服务只连自己的 schema；后续再考虑多实例/多库；
- 迁移：Flyway，`ddl-auto=validate`（生产）或 `none`；`DB/` 下历史 SQL 整理为首个 baseline；
- 配置：统一 `application.yml`（端口/名称）+ `application-local.yml`（本地连接）+ `application-prod.yml`；去掉 `config.properties` 与 `jdbc.properties`；开启 Nacos 配置中心（可选）；
- 部署：`deploy/docker/` 下每服务一个 Dockerfile，`deploy/docker-compose.yml` 起全套，健康检查 `/actuator/health`；
- 质量门：`mvn verify`（编译 + 单测 + ArchUnit）、`springdoc` 生成文档、CI 里跑。

---

## 四、迁移路线（每步可独立验证）

| 阶段 | 内容 | 验收 | 风险 |
|---|---|---|---|
| **P0 规范化基线（1–2 天）** | 写 `docs/backend-structure-spec.md`；引入 ArchUnit（先只禁用“跨服务 import”）；统一配置写法（先改 `lab-svc` 内联 → application-local.yml）；修根 `Dockerfile` 与 compose（至少能构建一个服务）；网关路由表合并为单份 | `mvn -q -DskipTests compile` 全绿；每个服务本地 `GET /actuator/health` 200；`docker build` 可产出 1 个服务镜像 | 低 |
| **P1 拆分 common（2–3 天）——已完成** | 建 `libs/kernel`、`libs/security`；`common` 全部内容按职责迁入（kernel：entity/dto/enums/util/vo/exception/admin/im；security：security），删除 `observability` 死代码与未使用的 `SecurityConfigBase`；7 个服务改为依赖 `typing-game-kernel` + `typing-game-security`；**Java 包名保持不变**，零 import 改动 | `mvn -o test` 全绿（11 模块、39 个测试）；8 个 ArchUnit 护栏通过 | 低 |
| **P2 实体/DTO 归位（1–2 周）** | 按 3.4 表归属把 `libs/kernel` 里的 entity/dto/repository 迁到各服务；跨域读先改 API（带缓存与降级）；kernel 只留真正的横切类型 | 全量编译 + 测试通过；kernel 内不再包含业务实体；ArchUnit 的“禁止跨服务 entity import”规则打开后通过；每个服务启动只加载自己 schema 的表 | **中高**（数据边界改动，建议按域逐个提交可回滚） |
| **P3 统一内部结构（3–5 天）** | 每个服务按 3.2 模板重排包；统一 interface+impl 规则；清理领域包（`chat/application`、`game/rule` 等归入 `service`/`support`） | ArchUnit 全规则通过；`mvn verify` 全绿 | 中（机械移动，但文件多） |
| **P4 数据与部署（1–2 周）** | 分 schema + Flyway baseline；`ddl-auto=validate`；网关路由仓库化；每服务 Dockerfile + compose 全链路 | `docker compose up` 起全套；冒烟脚本跑通 171 个端点的核心路径；Flyway 在空库可完整迁移 | **高**（需要停机窗口/双写方案） |
| **P5 契约与文档（2–3 天）** | springdoc + 网关文档；重写 `docs/backend-architecture.md`、`项目公共约束.md §2.1/2.2`；README 更新 | 文档与代码一致；新同学能按 spec 一次放对文件 | 低 |

**最关键的一步是 P0 的“规范文件 + ArchUnit 护栏”**：没有护栏，P2/P3 迁完还会退回原样。

---

## 五、风险与取舍

| 风险 | 说明 | 对策 |
|---|---|---|
| 拆库导致线上故障 | 44 张表跨 7 个服务读写 | 先 schema 隔离（同实例），再逐域拆表；game/dict 的跨读先做缓存 + API，不直接删表 |
| `game-core-svc` 体量 | 69 个端点、6 个域 | 先在 P2 内部按域分包（`service/game`、`service/stats`、`service/model`），后续再评估拆成 `stats-svc`/`model-svc` |
| API 路径变化影响前端 | 前端 base 为 `http://localhost:8080`，走 `/api/v1/**` | 目标结构保持 `/api/v1/**` 不变；只去掉服务端 context-path，由网关统一转发，前端零改动 |
| 迁移期双份代码 | 迁移中 common 与新 libs 并存 | 每个域一个提交、可回滚；迁移完成前不发布业务功能 |
| 测试不足 | ai-svc 0 测试 | P2 落地前给 ai-svc 补 controller/service 基础测试 |

---

## 六、需要拍板的 3 个决策

1. **内部结构模板**：分层 7 包（推荐，贴合现有规范、改动小）还是 DDD 四层（更彻底、改动大）？
2. **数据隔离节奏**：先“同实例分 schema”（推荐）还是直接“多库多实例”？
3. **服务边界**：先保持 8 个服务（推荐），还是这一轮就把 `game-core-svc` 拆成 `game-svc` + `stats-svc` + `model-svc`？

## 七、建议的第一步

只做 P0，1–2 天内可验证：

1. 新增 `docs/backend-structure-spec.md`（3.2/3.3 的规则 + 目录模板 + 配置模板）；
2. 引入 `archunit-junit5`，先写 3 条规则：禁止跨服务 import、controller 不得依赖 repository、entity 不得出现在 controller 签名；
3. `lab-svc` 配置迁到 `application-local.yml` + `config.properties` 风格；
4. 根 `Dockerfile` 改为 8 个服务各自的 Dockerfile（先做 `user-svc` 一个样板）；
5. 网关路由表合并为单份 `application.yml`。

P0 完成后再决定 P1/P2 的节奏。

---

## 八、剩余任务清单（执行顺序，逐项提交不停顿）

> 生成时间：2026-10-30。P0–P3 已完成，以下为 P4 起直到全部收尾的完整清单，按顺序执行，每项完成即提交并推送 gitee。

### P4 数据与部署（进行中）

- [x] **P4-1 分 schema Flyway 脚本**（`f9544fa`）：以 `DB/typing_word_game.sql` 为唯一源，切成 7 份 `V1__init.sql`（44 表 / 21 条同 schema 外键），跨 schema 21 条外键降级并登记 `DB/cross-schema-foreign-keys.md`；本地 MySQL 8.0.30 逐份导入 0 ERROR。
- [x] **P4-2 Flyway 接入**（`f9544fa`）：7 个服务加 `flyway-core` + `flyway-mysql`；`spring.flyway.*` 开启、`baseline-on-migrate`；数据源默认库改为各自 schema；`ddl-auto` 默认 `validate`。
- [x] **P4-3 部署全栈**（`bc3f466`）：`deploy/docker/Dockerfile` 通用构建 + `docker-compose.yml` 补齐 nacos/redis/mysql + 8 个服务（各自 schema、健康检查、依赖顺序）+ `deploy/smoke.ps1`（8 个健康检查 + 登录/核心接口 + 内部接口经网关必须 404 断言）。
- [x] **P4-4 空库验证**：新增 `deploy/verify-migrations.ps1` 作为门禁——7 个服务各建临时 schema、执行自己的 `V1__init.sql`、用 `information_schema` 比对表数与脚本声明、断言无跨 schema 外键，实测 7/7 PASS（44 表 / 21 外键），随后删除临时 schema；`mvn -o test` 全量门禁同步。
- [x] **P4-5 网关路由仓库化**：新增 `api-gateway/.../config/GatewayRouteConsistencyTest`——解析网关路由表，断言每个 `lb://<svc>` 的 `PrefixPath` 必为 `/<svc>`、路由 id 不重复，并在能读到兄弟模块源码时比对服务 `application.yml` 的 `context-path`，把「改一边忘另一边就 404」变成编译期可发现的测试失败。

### P5 契约与文档

- [x] **P5-1 springdoc/OpenAPI**：7 个 Web 服务引入 `springdoc-openapi-starter-webmvc-ui`（父 POM 统一 `springdoc.version`），网关引入 `springdoc-openapi-starter-webflux-ui`；`/v3/api-docs` 默认开启、`swagger-ui` 默认关闭；内部接口统一 `@Tag(name = "internal")`。见 `typingGame/docs/backend-structure-spec.md §7.1`。
- [x] **P5-2 文档重写**：重写 `docs/README.md`（文档索引，标清现状类/历史快照类）、`README.md`（根，部署与数据库描述）、`docs/backend-architecture.md`（顶部加历史存档说明）、`AGENTS.md`（远端、7 包、内部接口、schema、门禁）、`.trae/rules/project-rules.md`（门禁与提交规范）、`项目公共约束.md §2.1`（kernel 共享实体边界、DB/deploy 职责、词库改内部接口）、`typingGame/docs/backend-structure-spec.md §2`（目录过渡态与共享实体清单）。
- [x] **P5-3 新同学检查单**（临时模块已删除，不进仓库）：按 spec 新建一个"占位服务"模板分支验证一次（只动 pom + 目录 + 配置），确认 spec 可照抄：按 spec 建 `p53-check-svc`（父 POM + libs 依赖 + 7 包模板 + `@EntityScan` + Flyway 配置 + 复制 `ArchitectureTest`），`mvn -o test -pl p53-check-svc` 4 条 ArchUnit 全绿后删除模块并回滚父 POM —— 全程未改 spec 一处。

### 遗留与工程化

- [x] **L1 真机联调冒烟**（2026-10-30 完成：8 服务全部健康 + 登录/资料/排行榜/词库/规则链路 200，内部接口经网关 404；明细见 `typingGame/docs/smoke-report.md`）——原计划项：：待 8080–8087 空闲时启动 8 个服务 + MySQL + Redis + Nacos，跑 `/api/v1/auth/login`、`/api/v1/game/round`、`/api/v1/internal/**` 等核心路径；记录一份 `docs/smoke-report.md`。
- [x] **L2 前端小收口**：`tabs.tsx` 改为纸面直角分段控件（激活即墨底反白）、`Header` 未读徽章改直角、`ChatPage` 高度改 `calc(100dvh-3.75rem)` 并加 `min-h-[420px]`、趋势页「最近采样」改「时间聚合」（口径文案与之对齐；逐局序列需后端新增逐局接口，另立待办）。验证：前端 52 例测试、`npm run build` 8.56s、tsgo 仍为 17 处既有错误（未新增）。
- [x] **L3 ai-svc 测试补齐**：新增 `AiServiceImplTest`（2 例：弱项字符+近况聚合、空数据降级）、`AiChatServiceImplTest`（2 例：空消息与空请求直接拒绝且不触库）、`InternalAdminServiceImplTest`（2 例：分页夹取与排序），ai-svc 测试从 4 个 ArchUnit 增至 10 个。
- [x] **L4 github 补推**（已与 gitee 同步到同一 HEAD `d7c6f67`）：网络恢复后 `git push github master`，保持与 gitee 一致。
- [ ] **L5 CI（可选）**：Gitee/GitHub Actions 跑 `mvn -o test` + 前端 `npm run build`；未拍板前不做。

### 判定标准

- 所有阶段以「本模块测试 + 全量 `mvn -o test` + 工作区干净 + gitee 已推送」为完成条件；
- 每完成一项在本文档勾选并写明提交号；
- 跨服务行为以 `/api/v1/internal/**` 为唯一通道，网关对 `/internal/` 一律 404。

