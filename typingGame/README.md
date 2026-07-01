# 打字进化游戏 · 后端

<div align="center">
  <h1>⌨️ 打字进化游戏</h1>
  <p>后端服务：提供游戏核心逻辑、自适应算法、数据统计、社交系统与 AI 集成能力。</p>
</div>

前端代码位于同级目录 **`app-64`**，技术栈为 React + TypeScript + Vite。联调时需同时启动后端与前端，前端通过配置的 API 基地址访问后端接口。

---

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [控制器分组](#控制器分组)
- [核心功能模块](#核心功能模块)
- [游戏玩法说明](#游戏玩法说明)
- [安装与运行](#安装与运行)
- [开发规范](#开发规范)
- [测试](#测试)
- [部署与运维](#部署与运维)
- [许可证](#许可证)

---

## 项目概述

打字进化游戏后端是一个基于 Spring Boot 的 RESTful 服务，为前端提供完整的业务支持，包括：

- **用户认证与授权**：JWT 令牌机制，支持注册、登录、权限管理
- **游戏核心逻辑**：字符怪物生成、自适应难度调节、积分计算
- **数据统计与分析**：打字行为记录、趋势分析、训练建议生成
- **社交系统**：好友管理、实时聊天（WebSocket）、消息通知
- **词库管理**：系统词库、自定义词库上传与管理
- **AI 服务**：LLM 对话集成，提供智能训练指导
- **管理后台**：用户管理、游戏配置、数据监控

---

## 技术栈

### 核心技术

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 应用框架 | Spring Boot | 3.3.x | Java 后端主流框架 |
| 数据访问 | Spring Data JPA | - | ORM 框架，简化数据操作 |
| 关系数据库 | MySQL | 8.0+ | 存储用户、游戏记录、词库等核心数据 |
| 连接池 | Druid | 1.2.x | 高性能数据库连接池 |
| 缓存 | Redis | 6.0+ | 会话管理、缓存热点数据 |
| 分布式锁 | Redisson | 3.26.x | Redis 分布式锁与缓存 |
| 认证 | JWT (JJWT) | 0.12.x | 无状态身份认证 |
| 实时通信 | WebSocket | - | 好友聊天、消息推送 |
| 消息队列 | RocketMQ | 2.3.x | 异步消息处理 |
| API 文档 | SpringDoc OpenAPI | 2.5.x | Swagger UI 文档 |
| 对象映射 | MapStruct | 1.5.x | DTO/Entity 转换 |
| 工具库 | Lombok | - | 简化 Java 代码 |
| 数学计算 | Apache Commons Math3 | 3.6.x | 难度系数计算、统计分析 |
| Excel 处理 | EasyPOI | 4.4.x | 数据导出 |
| 文件解析 | Apache POI | 5.2.x | Word/PDF 文件解析 |

---

## 项目结构

```
typingGame/
├── DB/                              # 数据库相关文件
│   ├── typing_word_game.sql         # 数据库建库脚本
│   └── 数据库设计说明文档.md         # 数据库设计文档
├── src/                             # 源代码目录
│   ├── main/                        # 主代码
│   │   ├── java/com/typinggame/
│   │   │   ├── TypingGameApplication.java  # 应用入口
│   │   │   ├── config/                      # 配置类
│   │   │   │   ├── JwtConfig.java           # JWT 配置
│   │   │   │   ├── SecurityConfig.java      # 安全配置
│   │   │   │   ├── RedisConfig.java         # Redis 配置
│   │   │   │   ├── WebSocketConfig.java     # WebSocket 配置
│   │   │   │   └── ...
│   │   │   ├── controller/                  # 控制器层（按功能域分组）
│   │   │   │   ├── auth/                    # 认证模块
│   │   │   │   │   └── AuthController.java
│   │   │   │   ├── game/                    # 游戏模块
│   │   │   │   │   ├── GameController.java
│   │   │   │   │   ├── GameRecordController.java
│   │   │   │   │   ├── GameConfigController.java
│   │   │   │   │   ├── UserModelParamsController.java
│   │   │   │   │   └── UserModelAdaptLogsController.java
│   │   │   │   ├── stats/                   # 统计模块
│   │   │   │   │   ├── StatsController.java
│   │   │   │   │   ├── CharacterStatsController.java
│   │   │   │   │   └── UserDailyStatsController.java
│   │   │   │   ├── user/                    # 用户模块
│   │   │   │   │   └── UserController.java
│   │   │   │   ├── dictionary/              # 词库模块
│   │   │   │   │   └── DictionaryController.java
│   │   │   │   ├── social/                  # 社交模块
│   │   │   │   │   ├── FriendController.java
│   │   │   │   │   ├── LeaderboardController.java
│   │   │   │   │   └── MessageController.java
│   │   │   │   ├── ai/                      # AI 模块
│   │   │   │   │   └── AiController.java
│   │   │   │   └── admin/                   # 管理模块
│   │   │   │       └── AdminController.java
│   │   │   ├── service/                     # 业务逻辑层
│   │   │   │   ├── impl/                    # 服务实现类
│   │   │   │   │   ├── AuthServiceImpl.java
│   │   │   │   │   ├── GameServiceImpl.java
│   │   │   │   │   ├── EvolutionServiceImpl.java  # 遗传进化算法
│   │   │   │   │   └── ...
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── GameService.java
│   │   │   │   └── ...
│   │   │   ├── repository/                  # 数据访问层
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── GameRecordRepository.java
│   │   │   │   └── ...
│   │   │   ├── entity/                      # JPA 实体类
│   │   │   │   ├── User.java
│   │   │   │   ├── GameRecord.java
│   │   │   │   └── ...
│   │   │   ├── dto/                         # 数据传输对象
│   │   │   │   ├── request/                 # 请求 DTO
│   │   │   │   └── response/                # 响应 DTO
│   │   │   ├── enums/                       # 枚举类
│   │   │   │   ├── GameMode.java
│   │   │   │   ├── MonsterType.java
│   │   │   │   └── ...
│   │   │   ├── security/                    # 安全相关
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── util/                        # 工具类
│   │   │   │   ├── DifficultyUtil.java      # 难度计算
│   │   │   │   └── ...
│   │   │   └── vo/                          # 视图对象
│   │   │       └── ResultMessage.java       # 统一响应封装
│   │   └── resources/
│   │       ├── application.yml              # 应用配置
│   │       ├── static/                      # 静态资源
│   │       └── templates/                   # 模板文件
│   └── test/                               # 测试代码
│       └── java/                           # Java 测试代码
├── logs/                                   # 日志文件
├── pom.xml                                 # Maven 依赖配置
└── README.md                               # 项目说明文档
```

---

## 控制器分组

控制器按功能域划分为 8 个子目录，职责清晰：

| 目录 | 控制器 | API 路径前缀 | 职责 |
|------|--------|-------------|------|
| auth | AuthController | `/api/v1/auth` | 用户注册、登录、令牌刷新 |
| game | GameController | `/api/v1/game` | 游戏核心逻辑、开始/结束游戏 |
| game | GameRecordController | `/api/v1/game/records` | 游戏记录查询、统计 |
| game | GameConfigController | `/api/v1/game/config` | 游戏配置、难度参数 |
| game | UserModelParamsController | `/api/v1/model/params` | 用户个性化模型参数 |
| game | UserModelAdaptLogsController | `/api/v1/model/adapt/logs` | 模型适配日志 |
| stats | StatsController | `/api/v1/stats` | 数据统计分析 |
| stats | CharacterStatsController | `/api/v1/stats/characters` | 字符级统计 |
| stats | UserDailyStatsController | `/api/v1/stats/daily` | 每日汇总数据 |
| user | UserController | `/api/v1/user` | 用户资料、设置 |
| dictionary | DictionaryController | `/api/v1/dictionary` | 词库管理、单词查询 |
| social | FriendController | `/api/v1/friends` | 好友管理 |
| social | LeaderboardController | `/api/v1/leaderboard` | 排行榜 |
| social | MessageController | `/api/v1/messages` | 消息中心 |
| ai | AiController | `/api/v1/ai` | AI 助手对话 |
| admin | AdminController | `/api/v1/admin` | 管理后台 |

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
- **实时聊天**：好友间实时文字聊天（WebSocket）
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
| ATTACKER | 攻击型，生成后每 5 秒新增 1 个普通小怪，每 15 秒新增 8 个单字符普通小怪 | 3 |
| EVASIVE | 回避型，30% 概率规避玩家单次输入的正确字符 | 2 |
| SMART | 智慧型，下落过程中周围 1 个普通小怪会向其聚拢 | 3 |

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

### 安装步骤

1. **克隆项目**

   ```bash
   git clone <项目地址>
   cd superTyping/typingGame
   ```

2. **创建数据库**

   执行 `DB/typing_word_game.sql` 脚本创建数据库和表结构。

3. **配置数据库连接**

   修改 `src/main/resources/application.yml` 中的数据库连接信息：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/typing_word_game?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
       username: root
       password: 123456
   ```

4. **配置 Redis 连接**

   修改 `src/main/resources/application.yml` 中的 Redis 连接信息：

   ```yaml
   spring:
     redis:
       host: localhost
       port: 6379
   ```

5. **构建项目**

   ```bash
   mvn clean package -DskipTests
   ```

6. **运行项目**

   ```bash
   java -jar target/typingGame-0.0.1-SNAPSHOT.jar
   ```

7. **访问项目**

   - 后端服务默认运行在 `http://localhost:8080/typingWord`
   - API 文档（Swagger UI）：`http://localhost:8080/typingWord/swagger-ui.html`

---

## 开发规范

### 代码规范

- 遵循 Java 编码规范（Google Java Style）
- 使用 Lombok 简化代码（@Data、@Slf4j 等）
- 采用 RESTful API 设计风格
- 分层架构：Controller → Service → Repository
- 统一响应封装：使用 `ResultMessage` 封装所有 API 响应

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

## 测试

### 单元测试

- 使用 JUnit 5 进行单元测试
- 测试覆盖主要业务逻辑和核心功能
- 测试文件位于 `src/test/java` 目录

### 集成测试

- 测试系统各模块之间的集成
- 测试数据库和 Redis 连接
- 测试 API 接口的正确性

---

## 部署与运维

### 部署方式

- **本地部署**：直接运行 jar 包
- **Docker 部署**：使用 Docker 容器化部署
- **云服务部署**：部署到云服务器

### 监控与日志

- 使用 Spring Boot Actuator 进行系统监控
- 配置日志记录，便于问题排查
- 定期备份数据库

### 配置管理

- 数据库连接信息通过 `application.yml` 配置
- Redis 连接信息通过 `application.yml` 配置
- JWT 密钥等敏感信息建议使用环境变量或配置中心管理

---

## 许可证

MIT License

Copyright © 2026 打字进化游戏