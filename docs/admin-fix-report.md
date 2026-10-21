# 后台管理 & 数据库修复报告（2026-10-22）

对应计划：`docs/admin-fix-plan.md`。

## 总验收结果

| 项目 | 结果 |
|---|---|
| `mvn test`（typingGame 全部 8 个模块） | 通过，40 个测试 0 失败（本次新增 25 个） |
| 前端 `tsc --noEmit -p tsconfig.app.json` | 本次改动的文件 0 错误；仍有 24 个历史错误（Footer、GameCanvas、map、video、game/* 等，均为改动前已存在） |
| 前端 `vite build` | 成功 |
| 迁移 `DB/004-admin-hardening-20261022.sql` | 本地 MySQL 连续执行两次均成功，第二次全部跳过；`typing_word_game.sql` 已同步 |
| 启动服务后的端到端点击测试 | **未做**（本次未启动整套服务），重启后建议按文末清单手动点一遍 |

## 各阶段结果

### P1 显示修复 — 完成
- 游戏记录、排行榜、个人页的数值统一用 `formatNum`，`null` 显示为 `-`，不再白屏。
- 系统词库词条字段改为驼峰（`totalCount/errorCount/errorRate/userAvgInputTime`），词条明细能显示。
- 新增 `PATCH /admin/monster-configs/{type}`（积分 ≥ 0、速度 > 0、长度 ≥ 1，类型不存在返回 400）。
- 删除前端对不存在接口 `/help` 的调用。
- 用户 JSON 不含密码：`password` 已有 `@JsonIgnore`（代码确认，未单独写测试）。

### P2 筛选参数生效 — 完成
- 公共工具 `common/admin/AdminFilter`（计划中叫 AdminSpecs）：空值/空白自动忽略，支持 `user.userId` 这类关联路径。
- user-svc、game-core-svc、chat-svc、ai-svc 的后台列表接口全部接上前端传的筛选参数；聊天消息、AI 消息必须带会话 id；Dₜ 趋势和适应日志按用户/模式/日期过滤。

### P3 假保存改真保存 — 完成
- 字符规则 / 移动规则 / 难度参数存入 `system_params`（`admin.character_rules` 等，JSON），没有记录时返回默认值，保存前做校验。
- 配置模板存入新表 `config_template`，支持列表、读取、按名称覆盖保存、删除。
- 游戏模式配置 PATCH 真正写库。

### P4 数据库迁移 — 完成
- `user_info` 加 `status`（默认 1）、`last_login_time` 及索引；`system_params.param_value` 扩到 2000；新建 `admin_operation_log`、`config_template`；`friend_chat_message` 加两个索引。
- **与计划的偏差**：没有删除 `system_notification` 外键。全员公告改为"给每个用户各写一条"，既保留数据完整性，又让每个用户有独立的已读状态。

### P5 后台增强 — 完成
- 用户管理：状态筛选、状态/最近登录列、禁用/启用按钮（管理员不能被禁用）；编辑框可改昵称和签名，提交到后台专用接口（之前的 bug：会改成管理员自己的资料）。
- 禁用账号登录返回 403"账号已被禁用"，刷新 token 也会被拒；登录成功记录最近登录时间。
- 系统公告页：发布（全员或指定用户）、列表筛选、删除。
- 操作日志：admin-svc 记录所有非 GET 后台操作（操作人、方法、路径、请求体前 2000 字、状态、IP、耗时），有独立页面可筛选、展开看请求体。下游服务的 4xx/5xx 现在原样返回给前端，不再统一变成 500。
- 数据概览：真实统计（用户数、今日新增、今日活跃、已禁用、对局数、今日对局/人数、消息数、词库数、通知数），某个服务挂了只隐藏那一块并提示。
- 新增"高级配置"页：三类规则的 JSON 编辑与保存、配置模板的保存/载入/删除。

### P6 未读数修复 — 完成
- 未读总数 = 各会话 `unread_count` 之和 + 未读系统通知，不再因消息双份存储而翻倍。
- 消息中心显示系统通知，可单独标记已读（新接口 `PATCH /messages/notifications/{id}/read`，只能标记自己的通知）。

## 新增测试

| 模块 | 测试 | 覆盖点 |
|---|---|---|
| user-svc | `AuthServiceImplTest` | 禁用用户登录 403；登录记录最近登录时间 |
| user-svc | `InternalAdminServiceImplTest` | 禁用普通用户；不能禁用管理员；非法状态；改昵称/签名；空昵称拒绝 |
| game-core-svc | `AdminFilterTest` | 空参数不加条件；有值时每项一个条件 |
| game-core-svc | `InternalAdminServiceImplTest` | 规则默认值；保存后读回；速度区间校验；未知规则 404；怪物配置更新与校验 |
| chat-svc | `InternalAdminServiceImplTest` | 全员公告逐人写入；指定用户不存在 404；空标题拒绝 |
| chat-svc | `MessageServiceImplTest` | 未读数求和；标记自己的通知；不能标记别人的通知 |
| admin-svc | `AdminRoutesTest`、`OperationLogServiceTest` | 路径路由；请求体截断；失败状态记录；写日志失败不影响业务 |

## 遗留问题

1. 被禁用的用户手里已签发的 access token 在过期前仍可用（刷新会被拒）。如需立即踢下线，需要在网关或各服务校验 token 时查用户状态，或把该用户的 token 加入吊销列表。
2. 全员公告只发给发布时已存在的用户，之后注册的用户看不到旧公告。
3. 前端还有 24 个历史 TypeScript 错误（不影响 `vite build`）。
4. `typing_word_game.sql` 中有几段早已存在的乱码注释，未处理。

## 重启步骤

1. 已执行的本地库无需再跑迁移；其他环境执行 `DB/004-admin-hardening-20261022.sql`（可重复执行）。
2. `cd typingGame && mvn -q install -DskipTests`（common 改过，必须先安装）。
3. 依次重启：user-svc → game-core-svc → chat-svc → ai-svc → admin-svc → api-gateway。
4. 重启前端 `app-64`（`npm run dev`）。

## 重启后建议手动验证

- 后台"数据概览"显示数字；"用户管理"禁用一个测试账号，用该账号登录应提示"账号已被禁用"。
- "系统公告"发一条全员公告，普通用户的消息中心应出现并可标记已读，未读数随之减少。
- "高级配置"改移动规则保存，刷新后仍是新值；"操作日志"能看到刚才的操作。
