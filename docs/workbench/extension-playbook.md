# 新应用接入手册

## 每次照抄

1. 在 `app-64/src/workbench/apps/` 增加 Manifest，并登记到 `registry.ts`
2. 路由挂到 `/apps/{id}/**`；旧路径如需兼容则加 Navigate 别名
3. 数据优先新服务或现有 svc 新前缀；禁止无共享语义的业务塞进 `common` 实体
4. Admin：首期可直接改 Admin 页签；二期再做 `adminContributions[]`
5. 网关若新服务则加一条路由
6. 启动器磁贴文案 + 空状态；可选 `activityProvider`

## 个人维度内核（延后 · P4）

**触发条件**：出现 ≥2 个「个人状态」类应用（如技能树 + 健康）时再引入：

| 表 | 用途 |
|----|------|
| dimension_def | 维度定义，按 app 命名空间 |
| dimension_sample | (userId, dimId, value, observedAt, sourceApp) |
| dimension_link | 可选：维度 → 雷达轴 |

雷达应用只读聚合；技能树/健康写自己的权威表，可选同步抽样到 `dimension_sample`。

**本期不建这些表。** 新 App 若要上启动器「最近」与未来雷达，预留 `activityProvider` + 可选 `dimensionEmitter` 接口即可。

## 示例占位

| 应用 | 最小切片 | 数据归属 |
|------|----------|----------|
| 技能树 | 节点进度 + 笔记 | 新 knowledge-svc 或 user 扩展 |
| 健康 | 体重/睡眠打卡 | 新 svc，敏感隔离 |
| 人生雷达 | 读 dimension_sample | 只读聚合 |

## 暂缓

服务端算法重跑、公开分享 run、portal BFF。
