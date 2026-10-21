# 过夜方案：好友会话 → 即时通讯（IM）

> 适用：Cloud Agent / 本地长跑一夜  
> 目标：把现有「REST + 3s 轮询」好友聊天，升级为「REST 存历史 + WebSocket 实时推送」的可用 IM 最小闭环  
> 范围：仅 **好友一对一聊天**（不含 AI 会话、群聊、音视频）

---

## 0. 现状结论（开工前必读）

| 能力 | 现状 |
|------|------|
| 发消息 / 历史 / 会话列表 / 未读 | `chat-svc` HTTP 已通 |
| 好友关系 | `user-svc` HTTP 已通 |
| 「实时」体验 | 前端 `ChatPage` **每 3 秒轮询** |
| WebSocket / STOMP | **未实现**（依赖在 classpath，无 handler；网关无 WS 路由；前端无客户端） |
| 在线状态 / 正在输入 / 对端已读推送 | **无** |

关键路径：

```
app-64 ChatPage (poll 3s)
  → gateway /api/v1/messages/** → chat-svc
  → gateway /api/v1/friends/**  → user-svc
```

文档里写的 `ws://…/ws`、`/topic/messages/{id}` 是目标态，不是现状。

---

## 1. 一夜总目标（Definition of Done）

明早打开仓库，应同时满足：

1. **两个浏览器账号**互发消息，接收端 **无需刷新、无需等 3 秒轮询** 即可看到新消息（目标延迟 < 500ms，本机局域网）。
2. REST 发消息、历史拉取、未读标记 **仍然可用**（WS 挂了可降级回轮询）。
3. 网关可代理 WebSocket（或明确本机直连 chat-svc 的临时方案并写进报告）。
4. 产出 `docs/overnight-im-report.md`：每阶段 Done/Failed/Blocked + 复现步骤。
5. **可选加分**：在线状态、对端已读推送、正在输入（见阶段 D/E）。

非目标（明确不做）：

- 群聊、撤回完整产品化、图片/语音、离线推送（APNs/FCM）、跨机房消息队列
- 重写好友系统、改 AI 聊天
- 大范围 UI 重构

---

## 2. 夜间执行节奏（建议 23:00 → 07:00）

| 时段 | 阶段 | 必须产出 |
|------|------|----------|
| 23:00–00:30 | **A** 通道打通 | chat-svc STOMP + 鉴权握手；网关 WS 路由（或 documented 直连） |
| 00:30–02:30 | **B** 发送即推送 | `sendMessage` 后推到接收方 user queue |
| 02:30–04:30 | **C** 前端换轮询 | `websocket.ts` + ChatPage 订阅；保留 REST 历史/发送 |
| 04:30–05:30 | **D** 未读/已读推送 | 未读数或已读事件推给对端 |
| 05:30–06:30 | **E** 加分项 | 在线状态 或 正在输入（二选一优先做完） |
| 06:30–07:00 | **F** 验收与报告 | 跑验收清单，写报告，提交/开 PR |

规则：

- **严格按 A→B→C 顺序**；D/E 可砍，不可拖垮 C。
- 每阶段结束必须跑该阶段验收；失败先修，修不动标 `Blocked` 并写原因，再进入下一阶段（C 未过不得宣称 IM 完成）。
- 每阶段改动保持可独立回滚（小提交或清晰文件列表）。

---

## 3. 分阶段方案

### 阶段 A — WebSocket / STOMP 通道打通

**目标**  
chat-svc 能建立带 JWT 的 WebSocket 连接；消息 broker 可用。

**建议实现要点**

1. `WebSocketConfig`：启用 STOMP，endpoint 建议 `/ws`（注意 `context-path: /chat-svc` → 实际 `…/chat-svc/ws`）。
2. 握手鉴权：从 query `?token=` 或 header 取 JWT，校验后写入 Principal（userId）。
3. 目的地约定（落地后写进报告，勿与文档空想冲突）：
   - 客户端订阅：`/user/queue/im`（点对点）或 `/topic/user.{userId}`
   - 应用前缀：`/app`（若需要上行）
4. api-gateway：增加 WebSocket 路由（若一夜搞不定网关 WS，允许临时 **前端直连 `ws://localhost:8084/chat-svc/ws`**，但必须在报告写明，并留 TODO）。
5. Security：允许 `/ws/**` 握手；业务仍靠 JWT。

**阶段性成果**

- [ ] `WebSocketConfig` + JWT HandshakeInterceptor / ChannelInterceptor
- [ ] 本地用 wscat / Postman / 小脚本能 CONNECT + SUBSCRIBE
- [ ] 简短说明：连接 URL、token 怎么传、订阅地址

**验收标准**

| # | 标准 | 怎么验 |
|---|------|--------|
| A1 | 无 token 连接被拒绝 | 握手失败或 STOMP ERROR |
| A2 | 有效 JWT 可 CONNECT | 连接成功 |
| A3 | 可 SUBSCRIBE 到约定 destination | 订阅成功，无报错 |
| A4 | chat-svc 启动无 WS 相关 Fatal | 日志干净 |

**卡死条件**：连不上 STOMP → 停在 A，不要改前端。

---

### 阶段 B — 发送后实时推送（后端闭环）

**目标**  
保持现有 `POST /api/v1/messages`；落库成功后，向 **接收方** 推一条事件。

**建议实现要点**

1. 在 `MessageServiceImpl.sendMessage` 保存双份消息后，`SimpMessagingTemplate.convertAndSendToUser(receiverId, "/queue/im", payload)`。
2. payload 建议统一 envelope：

```json
{
  "type": "MESSAGE",
  "message": { /* MessageDTO 字段 */ },
  "ts": 1710000000000
}
```

3. 发送方可选也推一条 `MESSAGE_ACK`（非必须）。
4. 不删 REST；轮询可暂时并存。

**阶段性成果**

- [ ] 发送路径触发推送
- [ ] 两个 STOMP 客户端：A HTTP 发消息，B 订阅端收到 JSON

**验收标准**

| # | 标准 | 怎么验 |
|---|------|--------|
| B1 | REST 发送仍返回 200 + MessageDTO | curl / 前端旧流程 |
| B2 | 接收方订阅端收到 `type=MESSAGE` | 第二次连接收到推送 |
| B3 | 未订阅时发消息不拖垮服务 | 仅日志/忽略，HTTP 仍成功 |
| B4 | 库表仍有消息记录 | DB 或再 GET history |

**卡死条件**：推送不稳定但 REST 正常 → 标 Failed，仍可进入 C（C 做订阅，但验收会失败）。

---

### 阶段 C — 前端替换轮询（用户可感知 IM）

**目标**  
`ChatPage` 打开会话后走 WS 收消息；发送仍可用 REST；历史仍 REST。

**建议实现要点**

1. 新增 `app-64/src/services/websocket.ts`（或 `imSocket.ts`）：连接、断线重连、带 token、订阅 `/user/queue/im`。
2. `ChatPage.tsx`：去掉（或降级）3s `setInterval`；onMessage 追加气泡。
3. WS 断开时：自动 fallback 回轮询（建议保留），并在 UI 角标显示「实时连接中 / 已降级」。
4. 进入页：先 REST 拉历史，再连 WS。

**阶段性成果**

- [ ] FE WebSocket 客户端
- [ ] ChatPage 实时收消息
- [ ] 断线降级策略（至少代码路径存在）

**验收标准**

| # | 标准 | 怎么验 |
|---|------|--------|
| C1 | 双浏览器登录两好友，A 发 B 收，B **不刷新**可见 | 手工 2 账号 |
| C2 | 不再依赖「等满 3 秒」才出现 | 体感即时 |
| C3 | 刷新页面后历史仍完整 | REST history |
| C4 | token 失效后不会死循环刷屏报错 | 控制台无狂刷；可提示重新登录 |
| C5 | （建议）WS 断开后仍能靠轮询收到 | 停 chat-svc WS 或断网模拟 |

**本阶段是一夜「主交付」；C 过才算 IM MVP 成功。**

---

### 阶段 D — 未读 / 已读实时同步

**目标**  
接收方打开会话标记已读后，发送方能感知「已读」；或全局未读角标实时更新。

**建议实现要点（二选一做透，勿半吊子两个）**

- **D-读回执**：`markAsReadByFriend` 后推 `{ type: "READ", friendId, conversationId }` 给对端。
- **D-未读角标**：推 `{ type: "UNREAD", count }`；MessageCenter / Header 订阅更新。

**阶段性成果**

- [ ] 至少一种事件类型端到端打通
- [ ] 前端有最小展示（已读字样或角标数字）

**验收标准**

| # | 标准 | 怎么验 |
|---|------|--------|
| D1 | 事件 payload 有稳定 `type` | 抓 WS 帧 |
| D2 | 对端 UI 有可见变化且无需刷新 | 双浏览器 |
| D3 | 不影响发消息主路径 | B/C 验收仍过 |

---

### 阶段 E — 加分项（时间不够可砍）

**E1 在线状态（优先）**

- CONNECT → Redis `presence:{userId}` TTL；心跳刷新；DISCONNECT / 过期 → offline
- 聊天顶栏显示 Online/Offline  
- 验收：开/关页面，对端状态变化（允许 ≤ TTL 秒延迟）

**E2 正在输入**

- 客户端发 `/app/typing`；服务端转发给对端；不落库  
- 验收：A 输入时 B 显示「对方正在输入…」，停止后消失

---

### 阶段 F — 验收汇总与报告

**必须产出文件**：`docs/overnight-im-report.md`

模板：

```markdown
# Overnight IM Report — YYYY-MM-DD

## 总结果
- MVP (A+B+C): Done / Failed / Blocked
- D: ...
- E: ...

## 各阶段
### A
- 状态:
- 改动文件:
- 验收命令/步骤:
- 证据（日志片段/截图说明）:

### B…F 同上

## 如何本地复现（明早人工）
1. 启动服务：…
2. 连接 WS：…
3. 双账号验证步骤：…

## 风险与后续
- 网关 WS 是否临时直连
- 已知 bug
- 建议下一迭代
```

**验收标准（F）**

| # | 标准 |
|---|------|
| F1 | 报告存在且每阶段有状态 |
| F2 | 复现步骤可独立执行 |
| F3 | 未完成项不标 Done |

---

## 4. 一夜「总验收清单」（早上 5 分钟）

按顺序勾：

1. [ ] chat-svc、gateway（或直连说明）、前端可启动  
2. [ ] 用两个测试账号互加好友（或已有好友）  
3. [ ] A 打开与 B 的聊天页，B 打开与 A 的聊天页  
4. [ ] A 发送「ping-1」，B **1 秒内**看到（C）  
5. [ ] 刷新 B，历史仍有「ping-1」（REST）  
6. [ ] （若 D）B 已读后 A 有反馈或角标变化  
7. [ ] （若 E）在线或输入状态可见  
8. [ ] 打开 `docs/overnight-im-report.md`，状态与现象一致  

任一项失败 → 对应阶段标 Failed，并在报告写复现。

---

## 5. 给过夜 Agent 的执行指令（可直接粘贴）

```text
你是过夜执行 Agent。请严格按 docs/overnight-im-plan.md 执行「好友会话 → IM」方案。

规则：
1. 按阶段 A→B→C 顺序实现；D/E 有时间再做；最后必须写 docs/overnight-im-report.md。
2. 每阶段完成其验收标准后再进入下一阶段；失败先修，修不动标 Blocked 并说明阻塞原因。
3. 不重构无关模块；不改 AI 聊天；不做群聊。
4. 保持 REST 发送/历史可用；WS 为推送增强。
5. 网关 WS 若卡死，允许前端临时直连 chat-svc:8084，但必须写入报告。
6. 最终以阶段 C 双端实时收消息为 MVP 成功标准。
7. 所有验收命令与手工步骤写入报告，方便明早人工复查。
```

---

## 6. 风险与前置依赖

| 风险 | 缓解 |
|------|------|
| 本机 MySQL / Nacos / Redis 未起 | 过夜前先起齐；Cloud Agent 需配好环境或仅改代码 + 单测 |
| Gateway WebSocket 代理难配 | 临时直连 chat-svc，次日再补网关 |
| JWT 握手与 Security 冲突 | 单独放行 `/ws/**`，业务鉴权放 STOMP channel |
| 双写消息模型（收发各一份） | 推送用 receiver 视角 DTO，勿改存储模型 |
| 前端端口 / CORS | WS 鉴权走 token query，减少 CORS 困扰 |

过夜前建议人工确认：

- [ ] `user-svc` / `chat-svc` / `api-gateway` 能启动  
- [ ] 至少 2 个可登录账号且已是好友  
- [ ] Redis 可用（若做阶段 E 在线状态）

---

## 7. 成功评级（明早看这个就行）

| 评级 | 含义 |
|------|------|
| **S** | A+B+C+D 全过，报告完整 |
| **A** | A+B+C 过（真·IM MVP） |
| **B** | A+B 过，C 未完成（后端可推，前端仍轮询） |
| **C** | 仅 A 或部分代码，无端到端 |
| **F** | 主路径不可用且无清晰 Blocked 说明 |

目标：**至少拿到 A**；S 为超额。
