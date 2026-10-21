# 后台管理 & 数据库修复计划

> 目标：让后台管理"能看、能搜、能改、改了真生效"，并补齐数据库中后台必需的结构。
> 执行方式：按阶段顺序执行，每阶段完成后按验收标准自检，结果汇总到 `docs/admin-fix-report.md`。

## 阶段 P1：显示修复（白屏 / 字段不匹配 / 缺接口）

| 任务 | 验收标准 |
|---|---|
| 游戏记录、排行榜条目中数值为 `null` 时不再崩溃 | 所有 `.toFixed` 调用都有空值保护；`null` 显示为 `-` |
| 系统词库词条字段统一为驼峰（`totalCount`/`errorCount`/`errorRate`/`userAvgInputTime`） | 实体 JSON 输出为驼峰；前端词条明细能显示数值 |
| 补 `PATCH /admin/monster-configs/{type}` | 修改积分后再次查询返回新值；类型不存在返回 400 |
| 清理前端调用但后端不存在的接口（`/help`） | 前端不再调用不存在的接口 |
| 后台用户列表不暴露密码 | 用户 JSON 中无 `password` 字段（已有 `@JsonIgnore`，补测试确认） |

## 阶段 P2：筛选参数生效

| 任务 | 验收标准 |
|---|---|
| 公共工具 `AdminSpecs`：按"字段=值 / 字段 like 值"拼 JPA Specification，空值自动忽略 | 单元测试覆盖：空参数不加条件、字符串模糊匹配 |
| user-svc：好友关系、用户配置、模型参数、每日统计、快照按 `userId` 等过滤 | 前端 `services/admin.ts` 中每个筛选参数，后端控制器都有同名 `@RequestParam` |
| game-core-svc：游戏记录按 `gameMode`、字符统计按 `userId/charType/charValue`、拆分父串按 `userId` | 同上 |
| chat-svc：系统词库按 `name/category/isActive`、自定义词库、排行榜按 `userId/gameMode`、会话按 `userId`、聊天消息按 `conversationId`、通知按 `userId/notificationType/isRead` | 聊天消息只返回指定会话的消息 |
| ai-svc：AI 会话按 `userId/aiRobotId`、AI 消息按 `conversationId` | 同上 |
| 适应日志 & Dₜ 趋势按 `userId/gameMode` 过滤 | 趋势只统计所选用户 |

## 阶段 P3：假保存改为真保存

| 任务 | 验收标准 |
|---|---|
| 字符规则 / 移动规则 / 难度参数：读写 `system_params`（JSON 值），无记录时返回默认值 | PATCH 后 GET 返回新值；重启后仍在 |
| 配置模板：新表 `config_template`，保存 / 列表 / 读取 | 保存后列表能看到，按 id 能读回完整配置 |
| 游戏模式配置 PATCH：真正更新 `game_mode_configs` | 不存在的 id 返回 400 |

## 阶段 P4：数据库迁移 `004-admin-hardening-20261022.sql`

| 任务 | 验收标准 |
|---|---|
| ~~删除 `system_notification` 外键，用 `user_id=0` 表示全员公告~~ 执行时改为"按用户逐条写入"，保留外键（见 P5） | 外键仍在，数据完整性不降级 |
| `system_params.param_value` 扩到 `VARCHAR(2000)` | 能存 JSON 规则 |
| `user_info` 新增 `status`（1 正常 / 0 禁用）、`last_login_time` | 字段存在，默认 1 |
| 新表 `admin_operation_log` | 表存在 |
| 新表 `config_template` | 表存在 |
| `friend_chat_message` 新增 `(conversation_id, send_time)`、`(receiver_id, read_status)` 索引 | 索引存在 |
| 脚本幂等 | 连续执行两次不报错 |
| `typing_word_game.sql` 同步上述结构 | 新库初始化即包含新结构 |

## 阶段 P5：后台功能增强

| 任务 | 验收标准 |
|---|---|
| 禁用 / 启用用户；禁用用户登录返回 403 "账号已被禁用"；登录成功记录 `last_login_time` | 单元测试覆盖禁用登录 |
| 编辑用户昵称（后台编辑框真正提交） | PATCH 后昵称改变 |
| 发布系统公告（全员 = 给每个现有用户各写一条，或指定用户）；用户消息中心能看到并单独标记已读 | 单元测试：3 个用户 → 写入 3 条未读通知 |
| 后台操作日志：admin-svc 代理对所有非 GET 请求记录操作人、方法、路径、请求体、结果状态 | 修改操作后日志表有记录；后台有日志页面 |
| 数据概览：真实统计（用户数、今日活跃、对局数、今日对局、消息数、词库数） | 概览页显示数值而非静态文字 |
| 后台新增页面入口：系统公告、操作日志、高级配置（规则 + 模板） | 侧边栏可进入 |

## 阶段 P6：聊天未读数修复

| 任务 | 验收标准 |
|---|---|
| 未读总数改为汇总 `user_conversation.unread_count` + 未读系统通知 | 单条消息只计 1 个未读 |

## 总验收

- `mvn -q test`（全模块）通过。
- 前端 `tsc -p tsconfig.app.json` 无新增错误。
- 迁移脚本在本地 MySQL 执行两次成功。
- 输出 `docs/admin-fix-report.md`：各阶段结果、评级、遗留问题、重启步骤。
