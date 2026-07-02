# 打字进化游戏 · 后端

<div align="center">
  <h1>⌨️ 打字进化游戏</h1>
  <p>基于 Spring Boot 3.3 的微服务架构后端，提供游戏核心逻辑、自适应算法、数据统计、社交系统与 AI 集成能力。</p>
</div>

前端代码位于同级目录 **`app-64`**，技术栈为 React + TypeScript + Vite。联调时需同时启动后端与前端，前端通过 API 网关访问后端接口。

---

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [微服务架构](#微服务架构)
- [项目结构](#项目结构)
- [服务列表](#服务列表)
- [API 路由](#api-路由)
- [核心功能模块](#核心功能模块)
- [游戏玩法说明](#游戏玩法说明)
- [安装与运行](#安装与运行)
- [开发规范](#开发规范)
- [部署与运维](#部署与运维)
- [许可证](#许可证)

---

## 项目概述

打字进化游戏后端采用微服务架构，将原有单体应用拆分为多个独立服务，通过 API 网关统一对外提供服务。核心能力包括：

- **用户认证与授权**：JWT 令牌机制，支持注册、登录、权限管理
- **游戏核心逻辑**：字符怪物生成、自适应难度调节、积分计算
- **数据统计与分析**：打字行为记录、趋势分析、训练建议生成
- **社交系统**：好友管理、消息中心、好友私聊（当前 REST 可用，WebSocket 推送为后续增强）
- **词库管理**：系统词库、自定义词库上传与管理
- **AI 服务**：LLM 对话集成，提供智能训练指导
- **管理后台**：用户管理、游戏配置、数据监控。当前 `/admin/**` 由独立 `admin-svc` 聚合提供，并代理调用各领域服务的 `/internal/admin/**` 内部接口。

---

## 技术栈

### 核心技术

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 应用框架 | Spring Boot | 3.3.x | Java 后端主流框架 |
| 微服务治理 | Spring Cloud | 2023.0.x | 服务注册发现、配置管理 |
| 服务注册 | Nacos | 2.3.x | 服务发现与配置中心 |
| API 网关 | Spring Cloud Gateway | - | 统一入口、路由转发 |
| 数据访问 | Spring Data JPA | - | ORM 框架，简化数据操作 |
| 关系数据库 | MySQL | 8.0+ | 存储用户、游戏记录、词库等核心数据 |
| 连接池 | Druid | 1.2.x | 高性能数据库连接池 |
| 缓存 | Redis | 6.0+ | 会话管理、缓存热点数据 |
| 分布式锁 | Redisson | 3.26.x | Redis 分布式锁与缓存 |
| 认证 | JWT (JJWT) | 0.12.x | 无状态身份认证 |
| 实时通信 | WebSocket | - | 好友聊天、消息推送 |
| 消息队列 | RocketMQ | 2.3.x | 异步消息处理 |
| 对象映射 | MapStruct | 1.5.x | DTO/Entity 转换 |
| 工具库 | Lombok | - | 简化 Java 代码 |
| 数学计算 | Apache Commons Math3 | 3.6.x | 难度系数计算、统计分析 |
| Excel 处理 | EasyPOI | 4.4.x | 数据导出 |
| 文件解析 | Apache POI | 5.2.x | Word/PDF 文件解析 |
| 链路追踪 | Zipkin | - | 分布式链路追踪 |

---

## 微服务架构

```
┌─────────────────────────────────────────────────────────────┐
│                       客户端 / 前端                          │
└───────────────────────────────┬─────────────────────────────┘
                                │ HTTP/WebSocket
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway (8080)                      │
│                    统一入口、路由转发                         │
└───────────────────────────────┬─────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   User Svc    │     │  Game-Core Svc  │     │     AI Svc      │
│   (8081)      │     │    (8082)       │     │    (8083)       │
│  ├─ 认证      │     │  ├─ 游戏核心    │     │  ├─ AI对话      │
│  ├─ 用户管理  │     │  ├─ 游戏记录    │     │  ├─ 训练建议    │
│  └─ 好友管理  │     │  ├─ 自适应算法  │     │  └─ LLM集成     │
└───────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Chat Svc    │     │                 │     │                 │
│   (8084)      │     │                 │     │                 │
│  ├─ 消息中心  │     │                 │     │                 │
│  ├─ 排行榜    │     │                 │     │                 │
│  └─ 词库管理  │     │                 │     │                 │
└───────────────┘     └─────────────────┘     └─────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    基础设施层                                │
│  Nacos(8848) | Redis(6379) | MySQL(3306) | RocketMQ        │
│  Zipkin(9411) | Prometheus/Grafana                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目结构

```
typingGame/
├── pom.xml                          # 聚合父项目，管理依赖版本
├── common/                          # 公共模块（共享代码）
│   ├── src/main/java/com/typinggame/
│   │   ├── common/                  # 公共组件
│   │   │   ├── config/              # 安全配置基类
│   │   │   ├── exception/           # 全局异常处理
│   │   │   └── security/            # 统一JWT安全组件
│   │   ├── dto/                     # 共享DTO（按功能分组）
│   │   ├── entity/                  # 共享实体（按功能分组）
│   │   ├── enums/                   # 共享枚举
│   │   ├── util/                    # 共享工具类
│   │   └── vo/                      # 共享视图对象
│   └── pom.xml
├── api-gateway/                     # API网关
│   ├── src/main/java/com/typinggame/gateway/
│   │   └── GatewayApplication.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── user-svc/                        # 用户服务
│   ├── src/main/java/com/typinggame/user/
│   │   ├── controller/              # 认证、用户、好友控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── game-core-svc/                   # 游戏核心服务
│   ├── src/main/java/com/typinggame/game/
│   │   ├── controller/              # 游戏、记录控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── ai-svc/                          # AI服务
│   ├── src/main/java/com/typinggame/ai/
│   │   ├── controller/              # AI控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── chat-svc/                        # 社交聊天服务
│   ├── src/main/java/com/typinggame/chat/
│   │   ├── controller/              # 消息、排行榜、词库控制器
│   │   ├── service/                 # 业务逻辑
│   │   ├── repository/              # 数据访问
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── admin-svc/                       # 管理后台聚合服务
│   ├── src/main/java/com/typinggame/admin/
│   │   ├── controller/              # 管理后台聚合接口
│   │   └── config/                  # 安全配置
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── monolith-legacy/                 # 旧单体代码（迁移参考，不参与父工程构建）
├── DB/                              # 数据库脚本
│   ├── typing_word_game.sql
│   └── 数据库设计说明文档.md
├── docker-compose.yml               # 基础设施编排
├── Dockerfile
└── README.md
```

---

## 服务列表

| 服务名称 | 端口 | 职责 | 状态 |
|----------|------|------|------|
| api-gateway | 8080 | 统一入口、路由转发 | ✅ |
| user-svc | 8081 | 用户认证、用户管理、好友管理 | ✅ |
| game-core-svc | 8082 | 游戏核心逻辑、游戏记录、统计分析、自适应算法 | ✅ |
| ai-svc | 8083 | AI 对话、训练建议、LLM 集成 | ✅ |
| chat-svc | 8084 | 消息中心、排行榜、词库管理 | ✅ |
| admin-svc | 8085 | 管理后台聚合接口，代理调用各领域服务内部管理接口 | ✅ |

---

## API 路由

API 网关统一入口：`http://localhost:8080/typingWord`

| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| `/api/v1/auth/**` | user-svc | 注册、登录、令牌刷新 |
| `/api/v1/user/**` | user-svc | 用户设置等兼容路径 |
| `/api/v1/profiles/**` | user-svc | 用户资料、头像、用户统计 |
| `/api/v1/friends/**` | user-svc | 好友管理 |
| `/api/v1/game/**` | game-core-svc | 游戏核心逻辑、记录 |
| `/api/v1/stats/**` | game-core-svc | 字符统计、趋势分析、训练建议 |
| `/api/v1/admin/**` | admin-svc | 管理后台聚合接口，转发到各服务 `/api/v1/internal/admin/**` |
| `/api/v1/ai/**` | ai-svc | AI 助手对话 |
| `/api/v1/messages/**` | chat-svc | 消息中心 |
| `/api/v1/leaderboard/**` | chat-svc | 排行榜 |
| `/api/v1/dictionary/**` | chat-svc | 词库管理兼容路径 |
| `/api/v1/dictionaries/**` | chat-svc | 词库管理 |

### monolith-legacy 说明

`monolith-legacy` 保存旧单体后端代码，仅作为迁移参考和功能对照，不在父工程 `<modules>` 中参与构建。当前微服务仍有部分功能来源于该目录的迁移经验，确认所有接口、测试和文档完全收敛前不建议删除。

---

## 核心功能模块

### 游戏核心模块

- **玩家系统**：玩家操作、状态管理、生命值系统
- **字符怪物系统**：怪物生成、移动、攻击、类型系统
- **游戏模式**：无尽适应模式、词源界定模式、限时冲刺模式
- **自适应机制**：实时调节下落速度、字符串难度、三维自适应
- **积分系统**：积分计算、排行榜

### 自适应进化算法

- **父本选择**：根据玩家表现选择优秀的字符字符串作为父本
- **切片重组**：将多个父本字符串切片后重新组合
- **定向变异**：针对玩家薄弱字符进行强化训练
- **难度系数 Dₜ**：综合评估玩家当前水平的难度指标

### 数据统计与分析模块

- **打字行为记录**：速度、准确率、易错单词/字母等
- **分析报告生成**：薄弱点标注、针对性训练建议
- **历史趋势图**：打字速度、准确率变化趋势

### 词库系统

- **自定义词库上传**：支持 txt、doc、docx 格式
- **词库管理**：查看、使用、删除词库
- **单词熟练度**：记录单词练习情况，调整熟练度

### 社交系统

- **好友管理**：添加、删除、查看好友
- **好友聊天**：当前提供 REST 拉取、发送、已读、未读计数接口；WebSocket 推送为后续增强
- **消息中心**：系统消息、好友消息统一管理

### AI 服务模块

- **AI 助手对话**：智能训练指导、问题解答
- **训练建议生成**：基于打字数据生成个性化建议

### 系统管理模块

- **用户管理**：用户信息、权限管理
- **游戏记录管理**：查看、删除游戏记录
- **怪物管理**：怪物属性、行为参数配置
- **字符规则配置**：可视化配置字符生成和行进规则
- **难度调节**：调整游戏难度系数和权重

---

## 游戏玩法说明

### 基本操作

- **键盘输入**：敲击对应字符攻击字符怪物
- **Tab 键**：取消当前锁定的字符怪物
- **Shift 键**：切换中英文输入法
- **Caps Lock**：切换大小写
- **Shift + 字母**：临时切换大小写

### 字符怪物类型

| 类型 | 特殊效果 | 积分 |
|------|----------|------|
| NORMAL | 普通型，无特殊效果，匀速前进 | 1 |
| ATTACKER | 攻击型，生成后每 5 秒新增 1 个普通小怪 | 3 |
| EVASIVE | 回避型，30% 概率规避玩家单次输入的正确字符 | 2 |
| SMART | 智慧型，下落过程中周围小怪会向其聚拢 | 3 |

### 游戏模式

#### 无尽适应模式（核心）

1. 玩家勾选设置构成字符串的字符类型
2. 系统根据选定字符类型生成第一轮字符怪物
3. 玩家开始攻击，系统记录打字数据
4. 下一轮系统挑选上一轮表现好的字符怪物作为父本，进行字符切片重组和定向变异
5. 重复以上过程，直到玩家失败

#### 词源界定模式

1. 玩家上传自定义词库文件或选择系统词库
2. 系统随机抽取选定词库中的字符串生成字符怪物
3. 玩家开始攻击，系统记录打字数据
4. 下一轮系统结合上一轮表现好的字符怪物和词库数据生成新的字符怪物
5. 重复以上过程，直到玩家失败

#### 限时冲刺（趣味模式）

在相同 Dₜ 与怪物生成规则下，增加限时等规则变体，具体玩法见前端与配置。

---

## 安装与运行

### 环境要求

- **JDK 22**：Java 开发环境
- **MySQL 8.0+**：关系型数据库
- **Redis 6.0+**：缓存数据库
- **Maven 3.6+**：项目构建工具
- **Docker & Docker Compose**：容器化部署（可选）

### 快速启动（推荐）

使用 Docker Compose 启动基础设施：

```bash
cd typingGame
docker-compose up -d
```

等待 Nacos、Redis、MySQL 等服务启动完成（约 1-2 分钟）。

### 构建与运行

1. **构建项目**

```bash
mvn clean package -DskipTests
```

2. **启动各服务**（按顺序启动）

```bash
# 启动 API 网关
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar

# 启动用户服务
java -jar user-svc/target/user-svc-0.0.1-SNAPSHOT.jar

# 启动游戏核心服务
java -jar game-core-svc/target/game-core-svc-0.0.1-SNAPSHOT.jar

# 启动 AI 服务
java -jar ai-svc/target/ai-svc-0.0.1-SNAPSHOT.jar

# 启动聊天服务
java -jar chat-svc/target/chat-svc-0.0.1-SNAPSHOT.jar

# 启动管理后台聚合服务
java -jar admin-svc/target/admin-svc-0.0.1-SNAPSHOT.jar
```

### 访问服务

- API 网关：`http://localhost:8080/typingWord`
- Nacos 控制台：`http://localhost:8848/nacos`（用户名/密码：nacos/nacos）
- Zipkin 追踪：`http://localhost:9411`

---

## 开发规范

### 代码规范

- 遵循 Java 编码规范（Google Java Style）
- 使用 Lombok 简化代码（@Data、@Slf4j 等）
- 采用 RESTful API 设计风格
- 分层架构：Controller → Service → Repository
- 统一响应封装：使用 `ResultMessage` 封装所有 API 响应
- 当前 `/admin/**` 已独立为 `admin-svc`，并通过内部 API 调用领域服务；各领域服务内部接口已按 Controller → InternalAdminService 分层，内部管理 Service 已去除直接 `EntityManager` 查询，改为 Repository-backed 实现。新功能开发时应优先在所属领域服务补 `/internal/admin/**` 能力，避免管理聚合层承载领域规则。

### 设计模式应用

项目实际应用的核心设计模式：

- **策略模式**：不同游戏难度调节策略
- **工厂模式**：创建不同类型的字符怪物
- **观察者模式**：游戏事件通知机制
- **模板方法模式**：定义游戏流程骨架
- **代理模式**：权限控制和日志记录

### 命名规范

- 包名：全小写，使用点分隔（`com.typinggame.controller.game`）
- 类名：首字母大写，驼峰命名（`GameController`）
- 方法名：首字母小写，驼峰命名（`getGameRecords`）
- 变量名：首字母小写，驼峰命名（`gameMode`）

---

## 部署与运维

### 部署方式

- **本地开发**：使用 Docker Compose 启动基础设施，手动启动各服务
- **Docker 部署**：为每个服务构建 Docker 镜像，使用 Docker Compose 编排
- **Kubernetes 部署**：生产环境推荐使用 K8s 部署，支持自动扩缩容

### 监控与日志

- 使用 Spring Boot Actuator 进行系统监控
- 使用 Zipkin 进行分布式链路追踪
- 配置日志记录，便于问题排查
- 定期备份数据库

### 配置管理

- 数据库连接信息通过各服务的 `application.yml` 配置
- Redis 连接信息通过各服务的 `application.yml` 配置
- JWT 密钥等敏感信息建议使用 Nacos 配置中心或环境变量管理

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏
