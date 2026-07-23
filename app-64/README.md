# 打字进化游戏 · 前端

<div align="center">
  <h1>⌨️ 打字进化游戏</h1>
  <p>前端应用：提供游戏界面渲染、用户交互、数据可视化与实时通信能力。</p>
</div>

后端为 **Spring Boot 微服务架构** 工程，位于同级目录 **`typingGame`**，前端通过 API 网关（`VITE_BACKEND_BASE_URL`）与各微服务联调。

---

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境变量](#环境变量)
- [安装与运行](#安装与运行)
- [脚本说明](#脚本说明)
- [路由与页面](#路由与页面)
- [API 服务层](#api-服务层)
- [游戏引擎](#游戏引擎)
- [开发规范](#开发规范)
- [许可证](#许可证)

---

## 项目概述

### 核心功能

- **自适应进化**：怪物与难度随表现变化，针对薄弱字符加强训练
- **数据驱动**：记录对局与字符级统计，支持趋势、对比与训练建议
- **游戏化**：无尽适应、词库、限时等模式，配合教程与实时反馈
- **社交**：消息中心、好友列表、私聊（当前 REST 可用，WebSocket 推送为后续增强）
- **AI 助手**：对话走后端 `/api/v1/ai/chat` 接口，训练摘要会读取真实对局与字符统计数据

### 管理后台（管理员）

- 数据概览、用户管理、游戏记录、怪物配置
- Dₜ 趋势、系统参数、系统词库、排行榜条目、字符统计总览
- 当前 `/admin/**` 由后端独立 `admin-svc` 聚合提供，并通过各领域服务 `/internal/admin/**` 内部接口获取数据

### 架构适配

前端通过 **API 网关**（`http://localhost:8080`）统一访问后端微服务，前端请求层自动拼接 `/api/v1`：

| API 路径前缀 | 后端服务 | 职责 |
|-------------|----------|------|
| `/api/v1/auth/**` | user-svc | 认证 |
| `/api/v1/user/**`、`/api/v1/profiles/**` | user-svc | 用户管理 |
| `/api/v1/friends/**` | user-svc | 好友管理 |
| `/api/v1/game/**` | game-core-svc | 游戏核心 |
| `/api/v1/stats/**` | game-core-svc | 统计分析 |
| `/api/v1/admin/**` | admin-svc | 管理后台聚合接口 |
| `/api/v1/ai/**` | ai-svc | AI 助手 |
| `/api/v1/messages/**` | chat-svc | 消息中心 |
| `/api/v1/leaderboard/**` | chat-svc | 排行榜 |
| `/api/v1/dictionary/**`、`/api/v1/dictionaries/**` | chat-svc | 词库管理 |

---

## 技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 前端框架 | React | 18.x | 函数组件 + Hooks |
| 语言 | TypeScript | 5.9.x | 严格类型检查 |
| 构建工具 | Vite | 5.1.x | 快速开发与构建 |
| 样式方案 | Tailwind CSS | 3.4.x | 原子化样式 |
| UI 组件库 | shadcn/ui | - | 基于 Radix UI 的企业级组件 |
| 路由 | React Router | 7.9.x | 前端路由管理 |
| HTTP 客户端 | axios | 1.x | REST API 请求 |
| 表单处理 | react-hook-form + zod | 7.x / 3.x | 表单验证与数据校验 |
| 图表 | Recharts | 2.15.x | 数据可视化 |
| 实时通信 | WebSocket | - | 好友聊天、消息推送 |
| 测试 | Vitest | 2.1.x | 单元测试与集成测试 |
| 代码检查 | Biome + tsgo | 2.3.x | 代码规范与类型检查 |

---

## 项目结构

```
app-64/
├── src/
│   ├── components/              # 业务与 UI 组件
│   │   ├── admin/               # 管理后台组件
│   │   ├── ai/                  # AI 助手组件
│   │   ├── common/              # 通用组件
│   │   ├── game/                # 游戏核心组件
│   │   ├── profile/             # 用户资料组件
│   │   ├── stats/               # 统计分析组件
│   │   ├── trends/              # 趋势图表组件
│   │   └── ui/                  # UI 基础组件 (shadcn/ui)
│   ├── contexts/                # React Context
│   │   ├── AuthContext.tsx      # 认证状态管理
│   │   └── GameConfigContext.tsx # 游戏配置管理
│   ├── db/                      # API 客户端配置
│   │   ├── client.ts            # axios 实例配置（含拦截器）
│   │   └── api.ts               # 统一 API 导出入口（过渡层）
│   ├── game/                    # 游戏引擎与逻辑
│   │   ├── AdaptiveSystem.ts    # 自适应系统
│   │   ├── DataAnalyzer.ts      # 数据分析仪
│   │   ├── DifficultyController.ts # 难度控制器
│   │   ├── GameEngine.ts        # 游戏引擎核心
│   │   ├── InputHandler.ts      # 输入处理器
│   │   ├── Monster.ts           # 怪物类
│   │   ├── MonsterGenerator.ts  # 怪物生成器
│   │   ├── SoundSystem.ts       # 音效系统
│   │   └── types.ts             # 游戏类型定义
│   ├── hooks/                   # 自定义 Hooks
│   ├── lib/                     # 工具库
│   ├── pages/                   # 页面组件
│   ├── services/                # API 服务层（按业务域拆分）
│   │   ├── auth.ts              # 认证服务 → user-svc
│   │   ├── user.ts              # 用户服务 → user-svc
│   │   ├── game.ts              # 游戏服务 → game-core-svc
│   │   ├── stats.ts             # 统计服务 → game-core-svc
│   │   ├── dictionary.ts        # 词库服务 → chat-svc
│   │   ├── social.ts            # 社交服务 → user-svc / chat-svc
│   │   ├── ai.ts                # AI 服务 → ai-svc
│   │   └── admin.ts             # 管理服务 → 各服务
│   ├── types/                   # TypeScript 类型定义
│   ├── utils/                   # 工具函数
│   │   ├── index.ts             # 通用工具（含统一错误处理）
│   │   ├── trendData.ts         # 趋势数据处理
│   │   └── trendData.test.ts    # 趋势数据测试
│   ├── routes.tsx               # 路由配置
│   ├── App.tsx                  # 应用根组件
│   ├── main.tsx                 # 应用入口
│   ├── index.css                # 全局样式
│   └── global.d.ts              # 全局类型声明
├── public/                      # 静态资源
├── docs/                        # 文档（API、PRD、SQL）
├── .env.example                 # 环境变量示例
├── vite.config.ts               # Vite 配置
├── vitest.config.ts             # Vitest 配置
├── tailwind.config.ts           # Tailwind CSS 配置
├── tsconfig.json                # TypeScript 配置
└── package.json                 # 依赖配置
```

---

## 环境变量

复制 `.env.example` 为 `.env`。

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_BACKEND_BASE_URL` | API 网关地址（后端统一入口，不含 `/api/v1`） | `http://localhost:8080` |

---

## 安装与运行

### 环境要求

- **Node.js ≥ 18**
- **pnpm ≥ 8**

### 步骤

1. **启动后端服务**

   后端需先启动基础设施（Nacos、MySQL、Redis）和各微服务，详见 [typingGame/README.md](../typingGame/README.md)。

2. **安装依赖**

   ```bash
   pnpm install
   ```

3. **开发模式**

   ```bash
   pnpm dev
   ```

4. **构建生产版本**

   ```bash
   pnpm build
   ```

5. **预览生产版本**

   ```bash
   pnpm preview
   ```

---

## 脚本说明

| 命令 | 说明 |
|------|------|
| `pnpm dev` | 启动开发服务器（Vite） |
| `pnpm build` | 构建生产版本 |
| `pnpm preview` | 预览生产构建 |
| `pnpm lint` | 代码检查（tsgo、Biome、Tailwind） |
| `pnpm test` | 运行单元测试（Vitest） |
| `pnpm test:watch` | 监听模式运行测试 |

---

## 路由与页面

| 路径 | 页面组件 | 功能 |
|------|----------|------|
| `/` | HomePage | 首页 |
| `/login` | LoginPage | 登录 |
| `/register` | RegisterPage | 注册 |
| `/prepare` | PreparePage | 游戏准备 |
| `/game` | GamePage | 游戏进行中 |
| `/result` | ResultPage | 游戏结果 |
| `/profile` | ProfilePage | 用户资料 |
| `/trends` | TrendsPage | 趋势分析 |
| `/leaderboard` | LeaderboardPage | 排行榜 |
| `/dictionary` | DictionaryPage | 词库管理 |
| `/ai-assistant` | AIAssistantPage | AI 助手 |
| `/messages` | MessageCenterPage | 消息中心 |
| `/friends` | FriendsPage | 好友列表 |
| `/chat/:friendId` | ChatPage | 好友聊天 |
| `/settings` | SettingsPage | 设置 |
| `/admin` | AdminPage | 管理后台 |

游戏模式常见值：**endless**（无尽适应）、**dictionary**（词源界定）、**limit**（限时冲刺）。

---

## API 服务层

API 服务按业务域拆分为独立模块，每个模块负责特定业务的接口调用，统一通过 API 网关路由到对应微服务：

| 模块 | 文件 | 职责 | 目标服务 |
|------|------|------|----------|
| 认证 | services/auth.ts | 登录、注册、令牌刷新 | user-svc |
| 用户 | services/user.ts | 用户资料、设置 | user-svc |
| 游戏 | services/game.ts | 游戏核心、记录、配置、排行榜封装 | game-core-svc / chat-svc |
| 统计 | services/stats.ts | 数据统计、趋势分析 | game-core-svc |
| 词库 | services/dictionary.ts | 词库管理、单词查询 | chat-svc |
| 社交 | services/social.ts | 好友、聊天、消息 | user-svc / chat-svc |
| AI | services/ai.ts | AI 助手对话 | ai-svc |
| 管理 | services/admin.ts | 管理后台接口 | admin-svc（代理各领域服务内部管理接口） |

各服务模块统一从 `db/client.ts` 导入 axios 实例，确保请求配置一致（自动附加 JWT、统一错误处理）。

### 统一错误处理

前端已实现统一的错误处理机制：

- `db/client.ts`：响应拦截器自动识别后端 `ResultMessage` 格式，转换为 `ApiError`
- `utils/index.ts`：提供 `isApiError`、`getErrorMessage`、`handleApiError`、`isAuthError` 工具函数
- `AuthContext.tsx`：自动处理 401 认证过期，清理 token 并跳转登录页

---

## 游戏引擎

游戏核心逻辑位于 `src/game/` 目录：

| 文件 | 职责 |
|------|------|
| GameEngine.ts | 游戏主循环、状态管理 |
| Monster.ts | 怪物类定义 |
| MonsterGenerator.ts | 怪物生成策略 |
| InputHandler.ts | 键盘输入处理 |
| DifficultyController.ts | 难度调节 |
| AdaptiveSystem.ts | 自适应进化算法 |
| DataAnalyzer.ts | 实时数据分析 |
| SoundSystem.ts | 音效管理 |

---

## 开发规范

- TypeScript 严格模式；函数组件 + Hooks。
- 提交前执行 `pnpm lint`；工具函数配合 `pnpm test`。
- 提交前缀建议：`feat`、`fix`、`docs`、`style`、`refactor`。
- UI 组件遵循 shadcn/ui 规范，使用 Tailwind CSS 原子化样式。
- API 服务按业务域拆分，避免超大文件。
- 新增服务模块时，确保 API 路径与后端路由一致（见路由表）。
- 管理后台已独立为 `admin-svc`，并转发到各领域服务内部管理接口；新增管理能力时优先在领域服务补内部接口，再由 `admin-svc` 代理。

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏
