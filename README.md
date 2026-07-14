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
- [核心特性](#核心特性)
- [模块文档](#模块文档)
- [许可证](#许可证)

---

## 项目概述

打字进化游戏是一款以锻炼玩家打字能力为核心目的的网页游戏，通过算法分析玩家的打字习惯和盲区，让字符怪兽自适应进化，迫使玩家跳出打字舒适区，提升综合打字水平。

### 核心价值

- **科学训练**：基于数据驱动的自适应算法，精准定位薄弱环节
- **游戏化体验**：将枯燥的打字练习转化为有趣的游戏挑战
- **个性化方案**：根据每位玩家的特点定制训练路径
- **社交互动**：好友系统、实时聊天、排行榜，增强游戏粘性
- **微服务架构**：高可用、易扩展的云原生架构设计

---

## 架构说明

项目采用前后端分离 + 微服务架构，前端负责界面渲染与用户交互，后端通过 API 网关统一管理多个微服务。

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (app-64)                          │
│  React 18 + TypeScript + Vite 5 + Tailwind CSS + shadcn/ui  │
│  - 游戏界面渲染                                              │
│  - 用户交互处理                                              │
│  - 数据可视化展示                                            │
│  - 实时通信 (WebSocket)                                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST API + WebSocket
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway (8080)                        │
│                  统一入口、路由转发                           │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
┌─────────────┐ ┌───────────────┐ ┌─────────────┐
│  User Svc   │ │ Game-Core Svc │ │    AI Svc   │
│   (8081)    │ │    (8082)     │ │    (8083)   │
│ ├─ 认证     │ │ ├─ 游戏核心   │ │ ├─ AI对话   │
│ ├─ 用户管理 │ │ ├─ 游戏记录   │ │ ├─ 训练建议 │
│ └─ 好友管理 │ │ ├─ 自适应算法 │ │ └─ LLM集成  │
└─────────────┘ └───────────────┘ └─────────────┘
        │              │              │
        ▼              ▼              ▼
┌─────────────┐ ┌───────────────┐ ┌─────────────┐
│  Chat Svc   │ │   Common      │ │             │
│   (8084)    │ │   公共模块    │ │             │
│ ├─ 消息中心 │ │ ├─ 实体/DTO   │ │             │
│ ├─ 排行榜   │ │ ├─ 安全组件   │ │             │
│ └─ 词库管理 │ │ └─ 异常处理   │ │             │
└─────────────┘ └───────────────┘ └─────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    基础设施层                                │
│  Nacos(8848) | Redis(6379) | MySQL(3306) | RocketMQ        │
│  Zipkin(9411) | Prometheus/Grafana                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 技术栈

### 前端

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 前端框架 | React | 18.x | 函数组件 + Hooks |
| 前端语言 | TypeScript | 5.9.x | 严格类型检查 |
| 前端构建 | Vite | 5.1.x | 快速开发与构建 |
| 前端样式 | Tailwind CSS | 3.4.x | 原子化样式 |
| UI 组件库 | shadcn/ui | - | 基于 Radix UI 的组件库 |
| 前端路由 | React Router | 7.9.x | 前端路由管理 |
| HTTP 客户端 | axios | 1.x | REST API 请求 |
| 实时通信 | WebSocket | - | 好友聊天、消息推送 |

### 后端

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 后端框架 | Spring Boot | 3.3.x | Java 后端主流框架 |
| 微服务治理 | Spring Cloud | 2023.0.x | 服务注册发现 |
| 服务注册 | Nacos | 2.3.x | 服务发现与配置中心 |
| API 网关 | Spring Cloud Gateway | - | 统一入口、路由转发 |
| 数据访问 | Spring Data JPA | - | ORM 框架 |
| 数据库 | MySQL | 8.0+ | 核心数据存储 |
| 缓存 | Redis | 6.0+ | 会话管理、热点缓存 |
| 分布式锁 | Redisson | 3.26.x | Redis 分布式锁 |
| 认证 | JWT (JJWT) | 0.12.x | 无状态身份认证 |
| 消息队列 | RocketMQ | 2.3.x | 异步消息处理 |
| 对象映射 | MapStruct | 1.5.x | DTO/Entity 转换 |

---

## 项目结构

```
superTyping/
├── app-64/                          # 前端项目（React + TypeScript）
│   ├── src/
│   │   ├── components/              # 组件目录
│   │   ├── contexts/                # React Context
│   │   ├── db/                      # API 客户端配置
│   │   ├── game/                    # 游戏引擎与逻辑
│   │   ├── hooks/                   # 自定义 Hooks
│   │   ├── pages/                   # 页面组件
│   │   ├── services/                # API 服务层（按业务域拆分）
│   │   ├── types/                   # TypeScript 类型定义
│   │   ├── utils/                   # 工具函数
│   │   └── main.tsx                 # 应用入口
│   ├── docs/                        # 文档（API、PRD、SQL）
│   └── package.json                 # 前端依赖配置
│
├── typingGame/                      # 后端项目（Spring Boot 微服务）
│   ├── pom.xml                      # 聚合父项目
│   ├── common/                      # 公共模块
│   │   ├── src/main/java/com/typinggame/common/
│   │   │   ├── config/              # 安全配置基类
│   │   │   ├── exception/           # 全局异常处理
│   │   │   ├── security/            # 统一JWT安全组件
│   │   │   ├── dto/                 # 共享DTO
│   │   │   ├── entity/              # 共享实体
│   │   │   ├── enums/               # 共享枚举
│   │   │   ├── util/                # 共享工具类
│   │   │   └── vo/                  # 共享VO
│   │   └── pom.xml
│   ├── api-gateway/                 # API网关
│   ├── user-svc/                    # 用户服务（认证、用户、好友）
│   ├── game-core-svc/               # 游戏核心服务（游戏、记录、统计）
│   ├── ai-svc/                      # AI服务（AI对话、训练建议）
│   ├── chat-svc/                    # 社交聊天服务（消息、排行榜、词库）
│   ├── admin-svc/                   # 管理后台聚合服务
│   ├── DB/                          # 数据库脚本
│   ├── docker-compose.yml           # 基础设施编排
│   └── README.md                    # 后端文档
│
├── docs/                            # 项目文档
│   ├── backend-architecture.md      # 后端架构文档
│   ├── frontend-backend-integration.md # 前后端联调文档
│   └── project-flow.md              # 项目流程说明
│
└── README.md                        # 项目总览（本文档）
```

---

## 快速启动

### 环境要求

| 项目 | 要求 |
|------|------|
| 前端 | Node.js ≥ 18，pnpm ≥ 8 |
| 后端 | JDK 22，MySQL 8.0+，Redis 6.0+，Maven 3.6+ |

### 步骤

1. **启动基础设施（推荐使用 Docker Compose）**

   ```bash
   cd typingGame
   docker-compose up -d
   ```

2. **启动后端微服务**

   ```bash
   cd typingGame
   mvn clean package -DskipTests
   
   # 按顺序启动各服务
   java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
   java -jar user-svc/target/user-svc-0.0.1-SNAPSHOT.jar
   java -jar game-core-svc/target/game-core-svc-0.0.1-SNAPSHOT.jar
    java -jar ai-svc/target/ai-svc-0.0.1-SNAPSHOT.jar
    java -jar chat-svc/target/chat-svc-0.0.1-SNAPSHOT.jar
    java -jar admin-svc/target/admin-svc-0.0.1-SNAPSHOT.jar
   ```

3. **启动前端服务**

   ```bash
   cd app-64
   pnpm install
   pnpm dev
   ```

4. **访问应用**

   打开浏览器访问 `http://localhost:5173`

   前端默认通过 `http://localhost:8080/typingWord/api/v1` 访问后端 API 网关。

---

## 核心特性

### 自适应进化机制

- **遗传算法**：基于玩家表现选择优秀"父本"进行字符切片重组
- **定向变异**：针对薄弱字符进行强化训练
- **三维自适应**：实时调节下落速度、字符串难度、怪物类型

### 多模式游戏体验

| 模式 | 说明 |
|------|------|
| 无尽适应 | 核心模式，难度随表现动态进化 |
| 词源界定 | 使用自定义词库进行训练 |
| 限时冲刺 | 固定时间内挑战最高分 |

### 数据统计与分析

- **字符级统计**：记录每个字符的打字数据
- **趋势分析**：打字速度、准确率变化趋势
- **训练建议**：基于数据分析生成个性化改进方案

### 社交系统

- **好友管理**：添加、删除、查看好友
- **实时聊天**：基于 WebSocket 的即时通讯
- **排行榜**：积分排名展示
- **当前状态**：好友与消息 REST 接口已可用，WebSocket 实时推送为后续增强项

### AI 助手

- **智能对话**：提供训练指导和问题解答
- **个性化建议**：基于打字数据生成改进方案

### 管理后台

- **聚合管理接口**：当前 `/api/v1/admin/**` 由独立 `admin-svc` 提供，覆盖用户、记录、词库、排行榜、统计、模型参数和日志趋势。
- **边界实现**：`admin-svc` 作为代理聚合层，按路径调用 `user-svc`、`game-core-svc`、`chat-svc`、`ai-svc` 的 `/api/v1/internal/admin/**` 内部接口。

---

## 当前边界说明

- `user-svc`：认证、用户资料、好友关系。
- `game-core-svc`：游戏记录、统计、自适应配置。
- `chat-svc`：消息中心、排行榜、词库管理。
- `ai-svc`：AI 对话、训练摘要聚合。
- `admin-svc`：管理后台聚合接口，不直接承载领域规则，负责转发到各领域服务内部管理接口；领域服务内部接口按 Controller → InternalAdminService 分层，内部管理 Service 使用 Repository-backed 实现。

---

## 模块文档

- **前端详细文档**：[app-64/README.md](app-64/README.md)
- **后端详细文档**：[typingGame/README.md](typingGame/README.md)
- **后端架构文档**：[docs/backend-architecture.md](docs/backend-architecture.md)
- **前后端联调文档**：[docs/frontend-backend-integration.md](docs/frontend-backend-integration.md)

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏
