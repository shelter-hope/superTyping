# 生产级架构落地说明

## 目标

本项目采用前后端分离与后端微服务架构。后端以领域边界拆分为 `user-svc`、`game-core-svc`、`chat-svc`、`ai-svc`、`admin-svc`，公共实体、DTO、枚举、工具放在 `common`。所有新增功能默认遵循 Controller、Service、Repository 分层，Controller 不承载业务规则。

## 标准分层

以 `game-core-svc` 为例：

```text
game-core-svc/src/main/java/com/typinggame/game/
├── controller/      # HTTP 入参、认证上下文、统一返回
├── service/         # 业务接口，定义领域能力
├── service/impl/    # 业务实现、事务边界、流程编排
├── service/word/    # 词源策略扩展点
├── repository/      # Spring Data JPA 数据访问
└── config/          # 服务级配置
```

## 已落地的设计模式

| 模式 | 落地点 | 目的 |
|------|--------|------|
| 策略模式 | `WordSourceStrategy` | 不同游戏模式使用不同词源，不在 Controller 或 Service 中堆 if/else |
| 依赖注入 | Spring Bean 构造注入 | 解耦实现，便于测试和替换 |
| 门面/编排服务 | `GameConfigServiceImpl` | 对外提供稳定业务能力，内部编排模型、词源、怪物构造 |
| 仓储模式 | `*Repository` | 隔离 JPA 查询与业务逻辑 |

## 统一异常处理

后端统一使用 `common` 模块中的 `GlobalExceptionHandler` 处理异常。各微服务启动类只扫描本服务包和 `com.typinggame.common`，并显式声明本服务 Repository 与公共 Entity 扫描范围，避免全包扫描导致微服务边界漂移。

启动类约束：

| 服务 | 组件扫描 | JPA Repository 扫描 | Entity 扫描 |
|------|----------|---------------------|-------------|
| `user-svc` | `com.typinggame.user`, `com.typinggame.common` | `com.typinggame.user.repository` | `com.typinggame.entity` |
| `game-core-svc` | `com.typinggame.game`, `com.typinggame.common` | `com.typinggame.game.repository` | `com.typinggame.entity` |
| `chat-svc` | `com.typinggame.chat`, `com.typinggame.common` | `com.typinggame.chat.repository` | `com.typinggame.entity` |
| `ai-svc` | `com.typinggame.ai`, `com.typinggame.common` | `com.typinggame.ai.repository` | `com.typinggame.entity` |
| `admin-svc` | `com.typinggame.admin`, `com.typinggame.common` | 当前无 Repository | 当前无 JPA 边界配置 |

当前统一处理范围：

| 异常类型 | HTTP 状态 | 返回说明 |
|----------|-----------|----------|
| `BusinessException` | 异常内业务码 | 业务明确失败，如无权限、资源不存在、业务冲突 |
| `IllegalArgumentException` | 400 | 入参非法、业务参数不满足约束 |
| `IllegalStateException` | 409 | 当前状态不允许执行该操作 |
| `AccessDeniedException` | 403 | 已登录但无权限 |
| `MethodArgumentNotValidException` | 400 | `@Valid` 参数校验失败，返回字段错误详情 |
| `ConstraintViolationException` | 400 | 路径或查询参数校验失败 |
| `MissingServletRequestParameterException` | 400 | 缺少必要查询参数 |
| `MethodArgumentTypeMismatchException` | 400 | 参数类型错误 |
| `HttpRequestMethodNotSupportedException` | 405 | HTTP 方法不支持 |
| `MaxUploadSizeExceededException` | 400 | 上传文件超过限制 |
| `Exception` | 500 | 未预期服务端异常，服务端记录堆栈，前端只展示通用消息 |

服务内禁止再新增重复的局部 `GlobalExceptionHandler`。如确需服务级特殊异常处理，应先抽象到 common 或明确说明无法通用的原因。

## Spring Security 多模块规范

`common` 模块的 `SecurityConfigBase` 只提供跨服务可复用的安全基础组件，例如默认 `PasswordEncoder`、默认 `CorsConfigurationSource`、JWT 解析与过滤器组件。公共模块禁止定义 `SecurityFilterChain`，避免所有微服务扫描 common 后重复注册同名核心 Bean。

各业务微服务必须在本服务 `config.SecurityConfig` 中独立定义唯一的 `securityFilterChain`，并在该类内声明本服务的路由放行、权限拦截、JWT 过滤器接入规则。

强制约束：

- 禁止在 `common` 基础配置类中定义 `SecurityFilterChain`、服务级拦截规则等业务安全链路。
- 禁止开启 `spring.main.allow-bean-definition-overriding` 规避 Bean 冲突。
- 公共 Bean 如 `PasswordEncoder`、`CorsConfigurationSource` 必须使用 `@ConditionalOnMissingBean`，避免和服务级定制 Bean 冲突。
- 多过滤链场景必须使用不同 Bean 名称和明确匹配范围，不能依赖 Bean 覆盖顺序。
- 新增微服务时，必须新增本服务 `SecurityConfig`，不要复用其他服务安全链。

## 游戏配置核心链路

1. 前端调用 `GET /api/v1/game/config/next-round`。
2. `GameConfigController` 获取当前用户 ID 并调用 `GameConfigService`。
3. `GameConfigServiceImpl` 查询用户字符类型配置、用户易错字符、训练建议。
4. `DifficultyModelServiceImpl` 读取模型参数表与怪物边界表，输出 `GameAdjustParam`。
5. `WordSourceStrategy` 根据游戏模式选择词源：
   - 无尽/限时：`EndlessAdaptiveWordSourceStrategy`
   - 词库模式：`DictionaryWordSourceStrategy`
6. `GameConfigServiceImpl` 根据怪物类型配置构造 `MonsterDataDTO` 返回前端。

## 游戏结算核心链路

1. 前端调用 `POST /api/v1/game/records`，提交游戏记录、怪物历史、字符统计和词库 ID。
2. `GameRecordServiceImpl` 保存 `game_record` 和 `monster_data`，并批量更新 `character_stats`。
3. `GameRecordPostProcessServiceImpl` 作为结算后处理门面，统一维护衍生数据：
   - `user_daily_stats`：按日累计场次、分数、速度、准确率、输入字符。
   - `user_model_params`：更新用户当前难度、轮次和连续不匹配次数。
   - `user_model_adapt_logs`：记录调参前后难度、触发规则和关键参数。
   - `string_parents`：保存高价值怪物父本，并标记 `monster_data.is_parent`。
   - `dictionary_words`：回写自定义词库词条练习次数、错误率、熟练度。
4. `GameRecordServiceImpl` 将 `recordId` 与结算后的 `currentDifficulty` 返回前端。

## 生产约束

- 禁止在 Controller 中写业务规则。
- 表已存在的功能优先复用现有表，不重复建表。
- 查询用户私有资源必须校验 `userId` 归属，例如自定义词库抽词只允许读取当前用户词库。
- 所有可配置参数应优先读数据库配置，缺失时使用安全兜底值。
- 新增接口必须同步更新 `app-64/docs/API.md` 或功能审计文档。
- 业务失败优先抛 `BusinessException` 或明确语义的参数/状态异常，禁止直接吞异常或返回不一致结构。
