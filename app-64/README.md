# 打字进化游戏 · 前端

<div align="center">
  <h1>⌨️ 打字进化游戏</h1>
  <p>前端应用：提供游戏界面渲染、用户交互、数据可视化与实时通信能力。</p>
</div>

后端为独立 **Java（Spring Boot）** 工程，位于同级目录 **`typingGame`**，前端通过 `VITE_BACKEND_BASE_URL` 环境变量与后端联调。

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
- **社交**：消息中心、好友列表、私聊（依赖后端 WebSocket）
- **AI 助手**：对话走后端 `/api/v1/ai/chat` 接口（需 Java 侧配置 LLM）

### 管理后台（管理员）

- 数据概览、用户管理、游戏记录、怪物配置
- Dₜ 趋势、系统参数、系统词库、排行榜条目、字符统计总览

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
| HTTP 客户端 | axios / ky | 1.x / 1.x | REST API 请求 |
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
│   │   │   ├── AdaptLogTrend.tsx
│   │   │   ├── CharacterGenerationConfigEditor.tsx
│   │   │   ├── CharacterStatsOverview.tsx
│   │   │   ├── GameRecordManagement.tsx
│   │   │   ├── LeaderboardManagement.tsx
│   │   │   ├── MonsterConfigManagement.tsx
│   │   │   ├── SystemDictionaryManagement.tsx
│   │   │   ├── SystemParamsManagement.tsx
│   │   │   └── UserManagement.tsx
│   │   ├── ai/                  # AI 助手组件
│   │   │   └── AIChatInterface.tsx
│   │   ├── common/              # 通用组件
│   │   │   ├── Footer.tsx
│   │   │   ├── Header.tsx
│   │   │   ├── PageMeta.tsx
│   │   │   └── ProtectedRoute.tsx
│   │   ├── game/                # 游戏核心组件
│   │   │   ├── ErrorFeedback.tsx
│   │   │   ├── GameCanvas.tsx
│   │   │   ├── GameTutorial.tsx
│   │   │   ├── GameUI.tsx
│   │   │   └── RoundDataDisplay.tsx
│   │   ├── profile/             # 用户资料组件
│   │   │   ├── EditProfileDialog.tsx
│   │   │   └── UserAvatar.tsx
│   │   ├── stats/               # 统计分析组件
│   │   │   ├── DataAnalysisDashboard.tsx
│   │   │   └── ErrorDistributionChart.tsx
│   │   ├── trends/              # 趋势图表组件
│   │   │   ├── AccuracyChart.tsx
│   │   │   ├── ErrorComparison.tsx
│   │   │   └── SpeedChart.tsx
│   │   └── ui/                  # UI 基础组件 (shadcn/ui)
│   │       ├── accordion.tsx
│   │       ├── alert-dialog.tsx
│   │       ├── button.tsx
│   │       ├── card.tsx
│   │       ├── dialog.tsx
│   │       ├── form.tsx
│   │       ├── input.tsx
│   │       ├── table.tsx
│   │       └── ...
│   ├── contexts/                # React Context
│   │   ├── AuthContext.tsx      # 认证状态管理
│   │   └── GameConfigContext.tsx # 游戏配置管理
│   ├── db/                      # API 客户端配置
│   │   ├── client.ts            # axios 实例配置
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
│   │   ├── use-debounce.ts      # 防抖 Hook
│   │   ├── use-go-back.ts       # 返回上一页
│   │   ├── use-mobile.ts        # 移动端检测
│   │   └── use-toast.tsx        # Toast 提示
│   ├── lib/                     # 工具库
│   │   └── avatars.ts           # 头像生成工具
│   ├── pages/                   # 页面组件
│   │   ├── HomePage.tsx         # 首页
│   │   ├── LoginPage.tsx        # 登录页
│   │   ├── RegisterPage.tsx     # 注册页
│   │   ├── PreparePage.tsx      # 游戏准备页
│   │   ├── GamePage.tsx         # 游戏进行页
│   │   ├── ResultPage.tsx       # 游戏结果页
│   │   ├── ProfilePage.tsx      # 用户资料页
│   │   ├── TrendsPage.tsx       # 趋势分析页
│   │   ├── LeaderboardPage.tsx  # 排行榜页
│   │   ├── DictionaryPage.tsx   # 词库管理页
│   │   ├── AIAssistantPage.tsx  # AI 助手页
│   │   ├── MessageCenterPage.tsx # 消息中心
│   │   ├── FriendsPage.tsx      # 好友列表
│   │   ├── ChatPage.tsx         # 好友聊天
│   │   ├── SettingsPage.tsx     # 设置页
│   │   ├── AdminPage.tsx        # 管理后台
│   │   ├── SamplePage.tsx       # 示例页
│   │   └── NotFoundPage.tsx     # 404 页
│   ├── services/                # API 服务层（按业务域拆分）
│   │   ├── auth.ts              # 认证服务
│   │   ├── user.ts              # 用户服务
│   │   ├── game.ts              # 游戏服务
│   │   ├── stats.ts             # 统计服务
│   │   ├── dictionary.ts        # 词库服务
│   │   ├── social.ts            # 社交服务
│   │   ├── ai.ts                # AI 服务
│   │   └── admin.ts             # 管理服务
│   ├── types/                   # TypeScript 类型定义
│   │   ├── database.ts          # 数据库实体类型
│   │   ├── game.ts              # 游戏相关类型
│   │   └── index.ts             # 类型导出入口
│   ├── utils/                   # 工具函数
│   │   ├── index.ts             # 通用工具函数
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
| `VITE_BACKEND_BASE_URL` | Java 后端根路径 | `http://localhost:8080/typingWord` |

---

## 安装与运行

### 环境要求

- **Node.js ≥ 18**
- **pnpm ≥ 8**

### 步骤

1. **安装依赖**

   ```bash
   pnpm install
   ```

2. **开发模式**

   ```bash
   pnpm dev
   ```

3. **构建生产版本**

   ```bash
   pnpm build
   ```

4. **预览生产版本**

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

API 服务按业务域拆分为独立模块，每个模块负责特定业务的接口调用：

| 模块 | 文件 | 职责 |
|------|------|------|
| 认证 | services/auth.ts | 登录、注册、令牌刷新 |
| 用户 | services/user.ts | 用户资料、设置 |
| 游戏 | services/game.ts | 游戏核心、记录、配置 |
| 统计 | services/stats.ts | 数据统计、趋势分析 |
| 词库 | services/dictionary.ts | 词库管理、单词查询 |
| 社交 | services/social.ts | 好友、聊天、消息 |
| AI | services/ai.ts | AI 助手对话 |
| 管理 | services/admin.ts | 管理后台接口 |

各服务模块统一从 `db/client.ts` 导入 axios 实例，确保请求配置一致。

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

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏