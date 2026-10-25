# 架构迁移备注（阶段 3 评估项）

## 消息双行存储

当前 `friend_chat_messages` 对一次私聊发送落 **两条** 行（发送方会话一行、接收方会话一行），撤回依赖 `clientMsgId`；无 `clientMsgId` 时用内容 ±2s 启发式配对。

**结论**：短期保持双行，避免与现有会话未读/列表预览强耦合。若改为单行消息表：

1. 独立迁移脚本 + 双写期
2. 会话表改为引用 message_id，不再复制 content 预览以外的行
3. 撤回改为按 message_id 单点更新 + fan-out 推送

勿与包结构重构放在同一 PR。

## 难度双源

| 来源 | 职责 |
|------|------|
| `game-core` `DifficultyEngine` / `user_model_params` | 权威难度 D、遗传权重、结算后处理 |
| 前端 `AdaptiveSystem` | 本地速度/刷怪节奏表现 |

**约定**：表现层 `AdaptiveSystem` **不得**改服务端权威 D。新功能只读服务端下发参数；删除客户端调节需单独回归生存/无尽模式手感。

## 词库读端口

`game.word.WordSource` + `JpaWordSource` 已收口对局热路径对字典表的读取。日后改 HTTP/本地缓存只需换适配器，不必改 `DictionaryWordSourceStrategy`。
