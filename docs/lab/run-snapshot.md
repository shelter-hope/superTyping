# Lab Run 客户端快照

预设 = 命名参数模板。Run = 带结果的一次执行（参数 + 种子 + 指标）。引擎固定 `client_snapshot`，服务端不重跑算法。

## 表 `lab_run`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | INT NOT NULL | 仅登录用户可写 |
| algorithm_id | VARCHAR(64) | 如 pso |
| plugin_id | VARCHAR(64) | 与前端 registry 一致，通常等于 algorithm_id |
| schema_version | INT | 冻结当时 schema |
| engine_kind | VARCHAR(32) | 固定 `client_snapshot` |
| title | VARCHAR(120) NULL | |
| notes | VARCHAR(1000) NULL | |
| seed | INT NOT NULL | |
| params_json | JSON | ParamLimits 校验 |
| preset_id | BIGINT NULL | 来源预设 |
| status | VARCHAR(16) | running / completed / aborted |
| started_at / ended_at | DATETIME | |
| duration_ms | INT NULL | |
| summary_json | JSON | 终态摘要 |
| metrics_json | JSON | 降采样序列；列表接口不返回 |
| client_meta_json | JSON NULL | 画布、UA 等 |
| created_at | DATETIME | |

与打字 `GeneticEngine` 隔离：Lab genetic 命名空间为 `lab.genetic`，不对局权威难度混谈。

## Metrics envelope

```json
{
  "sampleIntervalMs": 250,
  "maxPoints": 240,
  "policy": "stride",
  "t": [0, 250, 500],
  "series": { "bestFitness": [1.2, 1.1] }
}
```

- 入库前 stride 压到 ≤240 点；body 建议 ≤256KB。
- 超限只留 summary + 稀疏序列。

## 各算法 summary 建议键

| algorithm | summary 键 |
|-----------|------------|
| boids | avgSpeed, cohesionApprox |
| pso | bestFitness, meanFitness |
| ant-colony | bestTour, meanTour |
| genetic | bestFitness, generation, meanFitness |

前端以 `step()` 返回的 metric 键为准写入 series；summary 取缓冲末值或运行中最优。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/lab/runs` | 一次提交 completed 快照（推荐） |
| PATCH | `/api/v1/lab/runs/{id}` | 更新状态/metrics |
| GET | `/api/v1/lab/runs` | 我的列表（无 metrics_json） |
| GET | `/api/v1/lab/runs/{id}` | 详情 |
| DELETE | `/api/v1/lab/runs/{id}` | 本人 |
| POST | `/api/v1/lab/runs/{id}/fork-preset` | 从 run 生成个人预设 |

`schema_version` 与目录不一致仍允许入库；列表标 `schemaStale`。
