# ⌨️ 打字进化游戏

<div align="center">
  <h1>打字进化游戏</h1>
  <p>以锻炼打字能力为核心的网页游戏：记录习惯与盲区，用遗传重组和难度模型动态出题，把练习做成可持续的对局。</p>
</div>

---

## 目录

- [项目概述](#项目概述)
- [架构说明](#架构说明)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [配置与环境变量](#配置与环境变量)
- [服务与接口](#服务与接口)
- [核心特性](#核心特性)
- [本地账号](#本地账号)
- [开发与校验](#开发与校验)
- [文档索引](#文档索引)
- [许可证](#许可证)

---

## 项目概述

打字进化游戏分析玩家的速度、准确率和易错字符，让字符怪物随对局进化，把训练从固定词表变成针对薄弱项的挑战。

- **科学训练**：字符级统计、趋势和训练建议定位薄弱环节
- **游戏化体验**：无尽适应、词源界定、限时冲刺三种模式
- **个性化方案**：遗传重组、定向变异和难度系数 `Dₜ` 随表现调整
- **社交互动**：好友申请、拉黑和一对一私聊。私聊走一条 WebSocket，断线时回退轮询
- **微服务架构**：API 网关统一入口，领域规则留在各自服务，管理后台只做聚合

---

## 架构说明

前端只访问 API 网关。网关按路径把请求转到对应服务，并补上该服务的 `context-path`。领域服务之间的管理数据通过 `/api/v1/internal/admin/**` 内部接口交换，`admin-svc` 不直接写领域规则。

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (app-64)                          │
│  React 18 + TypeScript + Vite 5 + Tailwind CSS + shadcn/ui  │
│  REST：http://localhost:8080/api/v1                         │
│  IM  ：ws://localhost:8080/ws/im?token=...                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway (8080)                        │
│            路由、CORS、负载均衡（Nacos）                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
     ┌────────────┬────┴────┬────────────┬────────────┐
     ▼            ▼         ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌────────┐ ┌─────────┐ ┌──────────┐
│user-svc │ │game-core │ │ ai-svc │ │chat-svc │ │admin-svc │
│  8081   │ │   8082   │ │  8083  │ │  8084   │ │  8085    │
│认证/资料│ │记录/统计 │ │对话/摘要│ │消息/IM  │ │后台聚合  │
│好友     │ │排行榜    │ │        │ │         │ │          │
└─────────┘ └──────────┘ └────────┘ └─────────┘ └──────────┘
     │            │          │           │            │
     └────────────┴──────────┴───────────┴────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 当前服务实际依赖：Nacos(8848) · MySQL(3306) · Redis(6379)    │
│ Compose 一并启动、代码尚未接入：Zipkin(9411) · RocketMQ      │
└─────────────────────────────────────────────────────────────┘
```

各业务服务的外部路径形如 `http://localhost:{port}/{context-path}/api/v1/...`。日常联调走网关即可，不必直连服务端口。

| 服务 | 端口 | context-path | 健康检查 |
|------|------|----------------|----------|
| api-gateway | 8080 | 无 | `http://localhost:8080/actuator/health` |
| user-svc | 8081 | `/user-svc` | `http://localhost:8081/user-svc/actuator/health` |
| game-core-svc | 8082 | `/game-core-svc` | `http://localhost:8082/game-core-svc/actuator/health` |
| ai-svc | 8083 | `/ai-svc` | `http://localhost:8083/ai-svc/actuator/health` |
| chat-svc | 8084 | `/chat-svc` | `http://localhost:8084/chat-svc/actuator/health` |
| admin-svc | 8085 | `/admin-svc` | `http://localhost:8085/admin-svc/actuator/health` |
| lab-svc | 8086 | `/lab-svc` | `http://localhost:8086/lab-svc/actuator/health` |
| dict-svc | 8087 | `/dict-svc` | `http://localhost:8087/dict-svc/actuator/health` |

---

## 技术栈

版本以仓库声明为准：后端见 `typingGame/pom.xml`，前端见 `app-64/package.json`。

### 前端

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | React | 18.x | 函数组件 + Hooks |
| 语言 | TypeScript | 5.9.x | 严格类型检查 |
| 构建 | Vite | 5.1.x | 开发与生产构建 |
| 样式 | Tailwind CSS | 3.4.x | 原子化样式 |
| 组件 | shadcn/ui（Radix UI） | - | 基础交互组件 |
| 路由 | React Router | 7.9.x | 页面路由 |
| HTTP | axios | 1.x | REST，统一走 `src/db/client.ts` |
| 图表 | Recharts | 2.15.x | 趋势与统计 |
| 实时 | WebSocket | - | 好友私聊，路径 `/ws/im` |
| 测试 / 检查 | Vitest、Biome | 2.1.x / 2.3.x | `pnpm test`、`pnpm lint` |

### 后端

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 运行时 | JDK | 22 | `java.version` |
| 框架 | Spring Boot | 3.3.13 | 各服务独立进程 |
| 微服务 | Spring Cloud | 2023.0.4 | Gateway、负载均衡 |
| 注册发现 | Spring Cloud Alibaba / Nacos | 2023.0.1.0 / 镜像 v2.3.0 | 默认 profile 为 `local` |
| 数据访问 | Spring Data JPA、Druid | Druid 1.2.23 | MySQL 8.0 |
| 缓存 / 锁 | Redis、Redisson | Redis 7.2、Redisson 3.26.0 | 会话与热点数据；锁在游戏核心服务 |
| 认证 | JJWT | 0.12.5 | 无状态 JWT |
| 对象映射 | MapStruct | 1.5.5 | DTO / Entity |
| 数值 | Commons Math3 | 3.6.1 | 难度与统计计算 |

`typingGame/docker-compose.yml` 还会拉起 Zipkin 和 RocketMQ 5.3。当前 Java 模块没有接入这两项，本地玩游戏不依赖它们。

---

## 项目结构

```
superTyping/
├── app-64/                          # 前端（React + TypeScript）
│   ├── src/
│   │   ├── components/              # 业务与 UI 组件
│   │   ├── contexts/                # 登录态、游戏配置
│   │   ├── db/                      # axios 客户端
│   │   ├── game/                    # 游戏循环、怪物、自适应
│   │   ├── pages/                   # 路由页面
│   │   ├── services/                # 按业务域拆分的 API
│   │   └── routes.tsx               # 路由表
│   ├── docs/                        # 前端 PRD、接口说明
│   └── package.json
│
├── typingGame/                      # 后端聚合工程
│   ├── pom.xml
│   ├── common/                      # 实体、DTO、JWT、全局异常
│   ├── api-gateway/                 # 8080
│   ├── user-svc/                    # 8081 认证、资料、好友
│   ├── game-core-svc/               # 8082 对局、统计、自适应、排行榜
│   ├── ai-svc/                      # 8083 对话与训练摘要
│   ├── chat-svc/                    # 8084 消息、通知、IM
│   ├── admin-svc/                   # 8085 管理聚合，转发内部接口
│   ├── lab-svc/                     # 8086 群体智能实验室
│   ├── dict-svc/                    # 8087 系统词库、自定义词库、词库文件
│   ├── DB/                          # 建库脚本与增量 SQL
│   └── docker-compose.yml
│
├── docs/                            # 架构、算法、联调与流程
├── 项目公共约束.md                   # 前后端日常开发约束
└── README.md
```

---

## 快速启动

### 环境要求

| 项目 | 要求 |
|------|------|
| 前端 | Node.js ≥ 18，pnpm ≥ 8 |
| 后端 | JDK 22，Maven 3.6+ |
| 基础设施 | Docker 与 Docker Compose。需要 MySQL 8、Redis 6+、Nacos 2.3 |

### 1. 启动基础设施

在 `typingGame` 目录执行：

```bash
cd typingGame
docker compose up -d
```

Compose 会创建网络和数据卷，并映射这些端口：

| 组件 | 地址 | 本地默认凭据 |
|------|------|----------------|
| MySQL 8.0 | `127.0.0.1:3306` | root / `123456`，库名 `typing_word_game`。另有账号 `typing_word_game` / `123456` |
| Redis 7.2 | `127.0.0.1:6379` | 密码 `123456` |
| Nacos 2.3.0 | `http://127.0.0.1:8848/nacos` | 控制台 `nacos` / `nacos`。Compose 打开了鉴权 |
| Zipkin | `http://127.0.0.1:9411` | 无鉴权。服务尚未上报链路 |
| RocketMQ | NameServer `9876` | 服务尚未生产或消费消息 |

首次创建 MySQL 数据卷时，入口脚本会执行 `DB/typing_word_game.sql`（已包含 `004` 的后台加固）。数据卷已经存在时不会再次导入。

增量脚本 `001`、`002`、`003` **没有**并进这份初始化 SQL，游戏会话、轮次和怪物遗传表需要单独执行。在库可用后按顺序导入：

```bash
cd typingGame
mysql -h127.0.0.1 -uroot -p123456 typing_word_game < DB/001-game-session-round-plan-20261006-2200.sql
mysql -h127.0.0.1 -uroot -p123456 typing_word_game < DB/002-monster-attempt-genetics-20261006-2310.sql
mysql -h127.0.0.1 -uroot -p123456 typing_word_game < DB/003-fix-round-payload-and-leaderboard-20261007.sql
```

`004-admin-hardening-20261022.sql` 已合并进初始化脚本，并且本身可重复执行。如果初始化日志在 `SET GLOBAL` 处中断，补跑一次 `004`。

服务配置默认连接 `root` / `123456`，与 Compose 的 root 密码一致。改密码时同时改 `MYSQL_PASSWORD`，或改各服务 `application-local.yml`。

### 2. 启动后端

默认 profile 是 `local`，会读取各模块的 `application-local.yml`。Nacos 就绪后再启动服务。

```bash
cd typingGame
mvn clean package -DskipTests

java -jar user-svc/target/user-svc-0.0.1-SNAPSHOT.jar
java -jar game-core-svc/target/game-core-svc-0.0.1-SNAPSHOT.jar
java -jar ai-svc/target/ai-svc-0.0.1-SNAPSHOT.jar
java -jar chat-svc/target/chat-svc-0.0.1-SNAPSHOT.jar
java -jar admin-svc/target/admin-svc-0.0.1-SNAPSHOT.jar
java -jar lab-svc/target/lab-svc-0.0.1-SNAPSHOT.jar
java -jar dict-svc/target/dict-svc-0.0.1-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

领域服务可以并行启动。网关依赖 Nacos 里的服务名做负载均衡，放在最后更不容易在第一次请求时找不到实例。开发时也可以按模块启动，例如：

```bash
mvn -pl user-svc -am spring-boot:run
```

仓库根下的 `typingGame/Dockerfile` 复制的是 `target/typingGame-0.0.1-SNAPSHOT.jar`。当前父工程是多模块，`mvn package` 不会产出这个包，本地请按上面的方式启动各服务。

### 3. 启动前端

```bash
cd app-64
pnpm install
pnpm dev
```

浏览器打开 `http://localhost:5173`。前端 axios 的 baseURL 是 `http://localhost:8080/api/v1`，网关 CORS 默认放行这个源。

### 4. 确认联通

1. Nacos 控制台能看到 `user-svc`、`game-core-svc`、`ai-svc`、`chat-svc`、`admin-svc`、`lab-svc`、`dict-svc`、`api-gateway`。
2. `curl http://localhost:8080/actuator/health` 返回 UP。
3. 用下面的种子账号登录。管理页是 `/admin`，需要 `admin` 角色。

---

## 配置与环境变量

未设置变量时使用 `application-local.yml` 与前端代码里的默认值，可直接连本机 Compose。

### 后端（`local` profile）

| 变量 | 默认值 | 作用 |
|------|--------|------|
| `NACOS_SERVER_ADDR` | `127.0.0.1:8848` | 注册中心 |
| `MYSQL_URL` | `jdbc:mysql://127.0.0.1:3306/typing_word_game?...` | 数据源 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / `123456` | 数据源账号 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `127.0.0.1` / `6379` / `123456` | 缓存 |
| `JWT_SECRET` | 各服务 local 配置中的开发密钥 | 签发与校验令牌。多服务必须使用同一密钥 |
| `CORS_ALLOWED_ORIGIN` | `http://localhost:5173` | 网关跨域来源 |
| `AI_LLM_URL` | `https://api.coze.cn/v1/workflow/run` | AI 对话的工作流地址 |
| `AI_LLM_API_KEY` | 空 | 为空时 AI 接口会提示未配置，其余功能不受影响 |
| `AI_LLM_WORKFLOW_ID` | local 配置中的开发工作流 ID | Coze workflow |

`dev` profile 不提供这些默认值，部署时必须自行注入。敏感项不要写入仓库。

Compose 里的 Nacos 开启了鉴权，服务配置只写了地址、没有用户名和密码。如果注册失败并返回 403，可以在本地把 `NACOS_AUTH_ENABLE` 设为 `false` 后重建 Nacos 容器，或在 discovery 配置中补上控制台账号 `nacos` / `nacos`。

### 前端

复制 `app-64/.env.example` 为 `app-64/.env`。

| 变量 | 默认值 | 作用 |
|------|--------|------|
| `VITE_BACKEND_BASE_URL` | `http://localhost:8080` | 网关根地址，代码会再拼接 `/api/v1` |
| `VITE_IM_WS_URL` | 由网关地址推导为 `ws://localhost:8080/ws/im` | 私聊 WebSocket。令牌走查询参数 `token` |

---

## 服务与接口

对外统一入口：`http://localhost:8080`。下面的路径都已经包含在网关断言中。

| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| `/api/v1/auth/**` | user-svc | 注册、登录、刷新令牌 |
| `/api/v1/user/**` | user-svc | 用户设置 |
| `/api/v1/profiles/**` | user-svc | 资料、头像、个人统计 |
| `/api/v1/friends/**` | user-svc | 好友关系 |
| `/uploads/**` | user-svc | 头像等上传文件 |
| `/api/v1/game/**`、`/api/v1/model/**` | game-core-svc | 对局、记录、模型参数 |
| `/api/v1/stats/**` | game-core-svc | 字符统计、趋势、训练建议 |
| `/api/v1/ai/**` | ai-svc | AI 对话与摘要 |
| `/api/v1/messages/**` | chat-svc | 消息中心 |
| `/api/v1/leaderboard/**` | game-core-svc | 排行榜。公开读不包含 `/my-rank` |
| `/api/v1/dictionary/**`、`/api/v1/dictionaries/**` | dict-svc | 词库。`/dictionary` 是兼容别名 |
| `/ws/**` | chat-svc | 即时通信 WebSocket，只使用 `/ws/im?token=` |
| `/api/v1/admin/**` | admin-svc | 管理后台。由 admin-svc 转发到各服务 `/api/v1/internal/admin/**` |

响应体使用 `ResultMessage`：`code`、`msg`、`data`。未登录返回 401，无权限返回 403。

### 前端页面

| 路径 | 页面 | 登录 |
|------|------|------|
| `/` | 首页 | 否 |
| `/login`、`/register` | 登录、注册 | 否 |
| `/prepare`、`/game`、`/result` | 准备、对局、结算 | 是 |
| `/profile`、`/trends`、`/settings` | 资料、趋势、设置 | 是 |
| `/leaderboard` | 排行榜 | 否 |
| `/dictionary` | 词库 | 是 |
| `/ai-assistant` | AI 助手 | 是 |
| `/messages`、`/friends`、`/chat/:friendId` | 消息、好友、私聊 | 是 |
| `/admin` | 管理后台 | 是，且角色为 admin |

游戏模式取值：`endless`（无尽适应）、`dictionary`（词源界定）、`limit`（限时冲刺）。

---

## 核心特性

### 自适应进化

- **遗传算法**：按上一轮存活时间、消灭耗时和错误次数挑选父本，切片重组后生成下一轮字符串。说明见 [docs/genetic-algorithm.md](docs/genetic-algorithm.md)
- **定向变异**：把部分字符替换成该玩家的易错字符
- **难度模型**：难度系数跟踪准确率相对 85% 的偏差；轮次只在难度升高后增加怪物数量。说明见 [docs/math-model.md](docs/math-model.md)

### 模式

| 模式 | 标识 | 说明 |
|------|------|------|
| 无尽适应 | `endless` | 核心模式。字符池和父本随表现变化，直到被怪物碰到 |
| 词源界定 | `dictionary` | 使用系统词库或上传的 txt / doc / docx |
| 限时冲刺 | `limit` | 在同一套生成规则上增加倒计时 |

### 操作

- 敲击目标字符攻击当前锁定的怪物
- `Tab` 取消锁定
- `Shift` 切换中英文；中文模式下空格或 `Ctrl` 发射
- `Caps Lock` 或 `Shift` + 字母切换大小写

| 怪物类型 | 效果 | 基础积分 |
|----------|------|----------|
| NORMAL | 匀速前进 | 1 |
| ATTACKER | 周期性召唤普通小怪 | 3 |
| EVASIVE | 有概率回避一次正确输入 | 2 |
| SMART | 周围普通小怪向其靠拢 | 3 |

### 统计、社交、AI 与后台

- 字符级速度、准确率、趋势和训练建议来自 `game-core-svc`
- 好友关系在 `user-svc`；消息和私聊在 `chat-svc`；排行榜读写在 `game-core-svc`；词库在 `dict-svc`
- 私聊只走 `ws://<网关>/ws/im?token=`。打开时发送、正在输入和回执都在这条连接上，关闭时发送回退 `POST /api/v1/messages`，历史每 3 秒轮询一次
- 消息类型：文本、内置表情、图片和普通文件。图片和文件先上传到 `POST /api/v1/messages/attachments`，正文只存附件 id，下载要带登录令牌。发送方可以在 2 分钟内撤回，双方会话行一起标记
- 好友申请可以带 200 字附言。拉黑后不能再发消息，只有拉黑方能解除。在线状态看 Redis `im:online:{userId}`
- 已有数据库执行 `typingGame/DB/010-im-chat.sql`
- AI 对话读取真实对局和字符统计。未配置 `AI_LLM_API_KEY` 时只返回配置提示
- `/admin` 覆盖用户、记录、词库、排行榜、统计、模型参数和日志趋势。领域查询在各服务的内部管理接口中完成

---

## 本地账号

初始化脚本写入了两个开发账号，只用于本机。任何共享或公网环境都要先改掉密码和 `JWT_SECRET`。

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | admin，可进 `/admin` |
| `test` | `test123` | user |

---

## 开发与校验

约束全文在 [项目公共约束.md](项目公共约束.md)。和本次仓库直接相关的检查命令：

```bash
# 前端
cd app-64
pnpm lint
pnpm test
pnpm build

# 后端
cd typingGame
mvn compile
mvn test
```

接口、网关路由或表结构有变更时，同步改本 README 或对应模块文档，并给出迁移 SQL。管理能力先加在所属服务的 `/api/v1/internal/admin/**`，再由 `admin-svc` 转发。

---

## 文档索引

| 文档 | 内容 |
|------|------|
| [app-64/README.md](app-64/README.md) | 前端结构、脚本、路由、请求层 |
| [app-64/docs/prd.md](app-64/docs/prd.md) | 产品需求 |
| [app-64/docs/API.md](app-64/docs/API.md) | 前端侧接口说明 |
| [typingGame/README.md](typingGame/README.md) | 后端模块、玩法与运行说明 |
| [typingGame/DB/数据库设计说明文档.md](typingGame/DB/数据库设计说明文档.md) | 表结构 |
| [docs/backend-architecture.md](docs/backend-architecture.md) | 后端架构 |
| [docs/frontend-backend-integration.md](docs/frontend-backend-integration.md) | 联调过程记录 |
| [docs/project-flow.md](docs/project-flow.md) | 注册、对局、结算流程 |
| [docs/production-architecture.md](docs/production-architecture.md) | 分层、异常和设计模式落点 |
| [docs/genetic-algorithm.md](docs/genetic-algorithm.md) | 遗传出题 |
| [docs/math-model.md](docs/math-model.md) | 动态难度模型 |
| [项目公共约束.md](项目公共约束.md) | 开发、接口和安全约束 |

联调文档里若仍出现 `http://localhost:8080/typingWord`，以当前代码为准：网关没有 `typingWord` 前缀，前端 baseURL 是 `http://localhost:8080/api/v1`。

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏
