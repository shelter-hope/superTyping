# 文档索引

> 这里只做导航与状态标注：**现状类**文档描述今天的代码，**历史类**文档是被取代的快照，读之前先看状态。
> 后端结构与代码规范以 `typingGame/docs/backend-structure-spec.md` 为准，改造进度看 `docs/backend-structure-audit-and-plan.md` 顶部状态行。

## 从哪开始

| 目的 | 读什么 |
|------|--------|
| 跑起来（本地 / Docker） | 仓库根 `README.md` |
| 后端目录、分层、依赖与内部接口约定 | `typingGame/docs/backend-structure-spec.md` |
| 微服务规范化改造做了什么、还剩什么 | `docs/backend-structure-audit-and-plan.md` |
| 难度模型与公式 | `docs/math-model.md` |
| 遗传算法与父本/变异 | `docs/genetic-algorithm.md` |

## 现状类

| 文档 | 内容 |
|------|------|
| `math-model.md` | 难度状态方程 v2：logit 调节器、死区、步长、输出映射、弱项倍率 |
| `genetic-algorithm.md` | 遗传算法 v2：精英保留、锦标赛选择、双点交叉、混淆定向变异、SQL 与测试 |
| `frontend-backend-integration.md` | 前后端接口约定与错误码 |
| `production-architecture.md` | 生产形态：网关、鉴权、可观测性、容量 |
| `cloud-native-evolution-plan.md` | 云原生化路线（容器、配置中心、CI） |
| `typingGame/docs/backend-structure-spec.md` | 后端规范：目录模板、允许依赖、内部接口、迁移与配置、检查清单 |

## 历史 / 快照类

| 文档 | 状态 |
|------|------|
| `backend-architecture.md` | 旧单体时代描述（6 服务、`/typingWord` 前缀），**仅存档**，勿据此开发 |
| `code-refactoring-plan.md` | 2026-10 校正段在文末，前半部分为历史计划 |
| `design-patterns.md` | 模式清单，含 2026-10 校正段 |
| `function-implementation-audit.md` | 功能清单快照 |
| `admin-fix-plan.md` / `admin-fix-report.md` | 后台修复的历史记录 |
| `overnight-im-plan.md` | IM 通宵改造计划（已落地） |
| `project-flow.md` | 业务流程说明（结构部分已过时） |

## 写作约定

- 新文档放 `docs/`；先改主题文档，不新增「阶段 N」式补丁文档。
- 结构或接口有变更时，同步更新 `typingGame/docs/backend-structure-spec.md` 与审计计划的状态表。
- 文档里的命令必须能在本机直接跑通；脚本一律 UTF-8 **带 BOM**（Windows PowerShell 5.1 的解析要求）。
