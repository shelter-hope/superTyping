# ⌨️ 打字进化游戏

<div align="center">
  <h1>打字进化游戏</h1>
  <p>以锻炼打字能力为核心的智能网页游戏：通过自适应算法分析习惯与盲区，动态调整难度与字符生成，帮助玩家突破舒适区。</p>
</div>

---

## 目录

- [项目概述](#项目概述)
- [架构说明](#架构说明)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [模块文档](#模块文档)
- [许可证](#许可证)

---

## 项目概述

打字进化游戏是一款以锻炼玩家打字能力为核心目的的网页游戏，通过算法分析玩家的打字习惯和盲区，让字符怪兽自适应进化，迫使玩家跳出打字舒适区，提升综合打字水平。

### 核心特色

- **自适应进化机制**：根据玩家打字数据实时调整游戏难度和字符怪物特性
- **多模式游戏体验**：支持无尽适应模式、词源界定模式、限时冲刺模式
- **完整的社交系统**：好友添加、实时聊天、消息通知等功能
- **详细的数据分析**：记录打字行为数据，生成分析报告和训练建议
- **自定义词库支持**：支持上传和管理自定义词库
- **AI 助手**：集成 AI 对话功能，提供训练指导

---

## 架构说明

项目采用前后端分离架构：

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (app-64)                          │
│  React 18 + TypeScript + Vite + Tailwind CSS + shadcn/ui    │
│  - 游戏界面渲染                                              │
│  - 用户交互处理                                              │
│  - 数据可视化展示                                            │
│  - 实时通信 (WebSocket)                                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST API + WebSocket
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    后端 (typingGame)                         │
│  Spring Boot 3 + JPA + MySQL + Redis                        │
│  - 用户认证与授权                                            │
│  - 游戏核心逻辑                                              │
│  - 自适应算法引擎                                            │
│  - 数据统计与分析                                            │
│  - AI 服务集成                                               │
│  - 社交系统 (好友/聊天)                                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端框架 | React 18 + TypeScript |
| 前端构建 | Vite 5 |
| 前端样式 | Tailwind CSS + shadcn/ui |
| 前端路由 | React Router 7 |
| 后端框架 | Spring Boot 3.x |
| 数据库 | MySQL 8.0+ |
| 缓存 | Redis 6.0+ |
| 认证 | JWT |
| 实时通信 | WebSocket |
| AI 集成 | LLM API |

---

## 项目结构

```
superTyping/
├── app-64/              # 前端项目（React + TypeScript）
│   ├── src/             # 源代码
│   ├── docs/            # 文档（API、PRD、SQL）
│   └── package.json     # 前端依赖配置
│
├── typingGame/          # 后端项目（Spring Boot）
│   ├── src/main/        # 源代码
│   ├── DB/              # 数据库脚本
│   └── pom.xml          # Maven 依赖配置
│
└── README.md            # 项目总览（本文档）
```

---

## 快速启动

### 环境要求

- **前端**：Node.js ≥ 18，pnpm ≥ 8
- **后端**：JDK 17+，MySQL 8.0+，Redis 6.0+，Maven 3.6+

### 步骤

1. **启动后端服务**

   ```bash
   cd typingGame
   # 创建数据库（执行 DB/typing_word_game.sql）
   # 配置 src/main/resources/application.yml
   mvn clean package -DskipTests
   java -jar target/typingGame-1.0.0.jar
   ```

   后端服务默认运行在 `http://localhost:8080/typingWord`

2. **启动前端服务**

   ```bash
   cd app-64
   pnpm install
   pnpm dev
   ```

   前端服务默认运行在 `http://localhost:5173`

3. **访问应用**

   打开浏览器访问 `http://localhost:5173`

---

## 模块文档

- **前端详细文档**：[app-64/README.md](app-64/README.md)
- **后端详细文档**：[typingGame/README.md](typingGame/README.md)

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏