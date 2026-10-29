# 本地联调冒烟报告（L1）

> 记录 `typingGame/deploy/smoke.ps1` 在本地 7 库环境下的运行结果与排障过程。

## 环境

| 项 | 值 |
| --- | --- |
| 时间 | 2026-10-30 05:5x（Asia/Shanghai） |
| 代码 | `main` 分支，HEAD `c86abfb` 之上（本轮解耦 5 个共享实体 + V1 脚本校正） |
| 数据库 | 本机 MySQL 8.0.30，全新 7 schema（`typing_game_user/game/ai/chat/admin/lab/dict`） |
| 启动方式 | `java -jar` 逐个启动 8 个服务（未走 Docker），Nacos 注册中心未启动、服务直连 |
| 服务端口 | gateway 8080、user 8081、game-core 8082、ai 8083、chat 8084、admin 8085、lab 8086、dict 8087 |

## 结果

冒烟脚本 3 组断言全部通过：

```
== 1/3 等待服务健康 ==
[ OK ] api-gateway / user-svc / game-core-svc / ai-svc / chat-svc / admin-svc / lab-svc / dict-svc 均就绪

== 2/3 业务链路（经网关）==
[ OK ] 登录 test/test123 -> 200
[ OK ] 当前用户配置 /api/v1/user/config -> 200
[ OK ] 排行榜 /api/v1/leaderboard -> 200
[ OK ] 词库列表 /api/v1/dictionaries/system -> 200
[ OK ] 游戏规则 /api/v1/game/config/char-types -> 200
[ OK ] 内部接口经网关 404（符合预期）

== 3/3 结果 ==
冒烟通过：8 个服务健康 + 核心链路可用
```

`/api/v1/internal/**` 经网关一律 404（`GatewayGuardFilter` 拦截），直连服务可访问，符合设计。

## 本轮暴露并修复的问题

1. **kernel 残留实体造成 schema 校验串库**：`KeyConfusion` 等 5 个实体只有 game-core 使用，却仍放在 `libs/kernel`；各服务 `@EntityScan` 都扫 `com.typinggame.entity`，导致 user/dict 等服务启动时校验 game 库的表。
   → 5 个实体迁入 `com.typinggame.game.entity`，`@EntityScan` 收窄为本服务包。
2. **`key_confusion` 归属错位**：曾写在 user-svc 的 V1 里，但唯一读写方是 game-core。
   → 迁移到 `game-core-svc` 的 `V1__init.sql`。
3. **`user_daily_stats.stat_date` 类型不符**：实体改 `LocalDate` 后期望 `DATE`，V1 仍是 `DATETIME`。
   → V1 改为 `DATE`。
4. **全新库缺默认账号**：`user_info` 没有任何种子，登录 401。
   → user-svc V1 补 `admin`/`test` 两个 bcrypt 账号（与 README 一致）。
5. **聊天布尔列类型不符**：`is_top/is_mute/is_recall/is_read/is_persistent` 实体是 `Boolean`（`TINYINT(1)`），V1 生成为 `INT`。
   → V1 还原为 `TINYINT(1)`；`read_status` 实体为 `Integer`，保持 `INT`。
6. **ai-svc 双 `RestTemplate` 冲突**：`GameCoreClient` 构造器注入未加限定。
   → 加 `@Qualifier("restTemplate")`。
7. **冒烟脚本自身缺陷**：登录用 GET 断言（405）、未带鉴权头（403）、端点路径过时。
   → 新增 `CheckPost` + 鉴权头透传，端点改为 `/api/v1/user/config`、`/api/v1/dictionaries/system`、`/api/v1/game/config/char-types`；脚本保持 UTF-8 带 BOM（PowerShell 5.1 需要）。

## 注意

- 所有服务 Flyway 均以 `ddl-auto=validate` 校验实体与库表，交叉验证通过，说明「实体归属 = schema 归属」已一致。
- 本轮改动 V1 迁移脚本（相当于重建基线），本地已通过 `DELETE FROM flyway_schema_history` 重新执行；已有数据的环境需自行决定重置策略。
