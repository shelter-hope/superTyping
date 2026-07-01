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
- [路由与功能](#路由与功能)
- [数据与文档](#数据与文档)
- [开发规范](#开发规范)
- [许可证](#许可证)

---

## 项目概述

### 游戏与训练

- **自适应进化**：怪物与难度随表现变化，针对薄弱字符加强训练。
- **数据驱动**：记录对局与字符级统计，支持趋势、对比与训练建议。
- **游戏化**：无尽、词库、限时等模式，配合教程与实时反馈。
- **社交**：消息中心、好友、私聊（依赖后端会话与消息接口）。
- **AI 助手**：对话走后端 `/api/v1/ai/chat` 等接口（需 Java 侧配置 LLM）。

### 管理后台（管理员）

- 数据概览、用户管理、游戏记录、怪物配置
- Dₜ 趋势、系统参数、系统词库、排行榜条目、字符统计总览

---

## 技术栈

| 分类 | 技术 | 说明 |
|------|------|------|
| 前端框架 | React 18 + TypeScript | 函数组件 + Hooks，严格类型检查 |
| 构建工具 | Vite 5 | 快速开发与构建 |
| 样式方案 | Tailwind CSS + shadcn/ui | 原子化样式与组件库 |
| 路由 | React Router 7 | 前端路由管理 |
| HTTP 客户端 | axios | REST API 请求 |
| 表单处理 | react-hook-form + zod | 表单验证与数据校验 |
| 图表 | Recharts | 数据可视化 |
| 测试 | Vitest | 单元测试与集成测试 |
| 代码检查 | Biome + tsgo | 代码规范与类型检查 |

---

## 项目结构

```
app-64/
├── src/
│   ├── components/          # 业务与 UI 组件
│   │   ├── admin/           # 管理后台组件
│   │   ├── ai/              # AI 助手组件
│   │   ├── common/          # 通用组件
│   │   ├── game/            # 游戏相关组件
│   │   ├── profile/         # 用户资料组件
│   │   ├── stats/           # 统计分析组件
│   │   ├── trends/          # 趋势图表组件
│   │   └── ui/              # UI 基础组件（shadcn/ui）
│   ├── contexts/            # React Context（Auth、GameConfig）
│   ├── db/                  # API 客户端（client.ts、api.ts）
│   ├── game/                # 游戏引擎与逻辑
│   ├── hooks/               # 自定义 Hooks
│   ├── lib/                 # 工具库与工具函数
│   ├── pages/               # 页面组件
│   ├── types/               # TypeScript 类型定义
│   ├── utils/               # 工具函数与测试
│   ├── routes.tsx           # 路由配置
│   └── main.tsx             # 应用入口
├── public/                  # 静态资源
├── docs/                    # 文档（API、PRD、SQL）
├── .env.example             # 环境变量示例
├── vite.config.ts           # Vite 配置
├── vitest.config.ts         # Vitest 配置
└── package.json             # 依赖配置
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

## 路由与功能

见 `src/routes.tsx`。主要路径：

| 路径 | 功能 |
|------|------|
| `/` | 首页 |
| `/login` | 登录 |
| `/register` | 注册 |
| `/prepare` | 游戏准备 |
| `/game` | 游戏进行中 |
| `/result` | 游戏结果 |
| `/profile` | 用户资料 |
| `/trends` | 趋势分析 |
| `/leaderboard` | 排行榜 |
| `/dictionary` | 词库管理 |
| `/ai-assistant` | AI 助手 |
| `/messages` | 消息中心 |
| `/friends` | 好友列表 |
| `/chat/:friendId` | 好友聊天 |
| `/settings` | 设置 |
| `/admin` | 管理后台 |

游戏模式常见值：**endless**（无尽适应）、**dictionary**（词源界定）、**limit**（限时冲刺）。

---

## 数据与文档

- **MySQL 参考脚本**：`docs/typing_word_game.sql`
- **接口清单**：`docs/API.md`（与 `db/api.ts` 对齐）
- **产品需求文档**：`docs/prd.md`
- 后端详细说明：[typingGame/README.md](../typingGame/README.md)

---

## 开发规范

- TypeScript 严格模式；函数组件 + Hooks。
- 提交前执行 `pnpm lint`；工具函数配合 `pnpm test`。
- 提交前缀建议：`feat`、`fix`、`docs`、`style`、`refactor`。
- UI 组件遵循 shadcn/ui 规范，使用 Tailwind CSS 原子化样式。

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏