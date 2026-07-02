# 前后端联调文档

## 一、项目概述

### 1.1 前端项目现状

| 项目信息 | 说明 |
| :--- | :--- |
| 项目名称 | 打字进化游戏前端 |
| 项目路径 | `app-64` |
| 技术栈 | React 18 + TypeScript + Vite 5 + Tailwind CSS |
| 主要功能 | 打字游戏、AI对话、统计分析、个人资料管理、管理员功能 |

### 1.2 后端项目概况

| 项目信息 | 说明 |
| :--- | :--- |
| 技术栈 | Spring Boot 3.3 + Spring Security + JWT + Spring Data JPA + Redis + WebSocket |
| 架构 | 微服务架构（API网关 + 5个微服务） |
| 数据库 | MySQL 8.0+ |
| API风格 | RESTful API + WebSocket |
| 主要功能 | 用户认证、游戏记录管理、字符统计、词库管理、遗传进化算法、好友系统、AI对话系统 |

### 1.3 架构关系

```
前端 (app-64)
    │
    ├── API Gateway (8080/typingWord)
    │       ├── user-svc (8081)      - 认证、用户、好友
    │       ├── game-core-svc (8082) - 游戏、记录、统计
    │       ├── ai-svc (8083)        - AI对话
    │       └── chat-svc (8084)      - 消息、排行榜、词库
    │
    └── WebSocket (8080/typingWord/ws)
            └── chat-svc (8084)      - 实时聊天
```

---

## 二、适配目标

| 目标 | 说明 |
| :--- | :--- |
| API对齐 | 确保前端API调用与后端接口定义一致 |
| 数据模型匹配 | 前端数据结构与后端DTO/Entity保持一致 |
| 认证机制集成 | 实现JWT认证与授权，统一通过API网关 |
| WebSocket集成 | 支持实时通信功能（通过API网关转发） |
| 业务逻辑适配 | 确保前端业务流程与后端业务规则匹配 |
| 错误处理统一 | 前后端错误码和错误信息格式统一 |

---

## 三、技术栈适配

| 前端技术 | 后端对应技术 | 适配说明 |
| :--- | :--- | :--- |
| React 18 + TypeScript | Spring Boot 3.3 + 微服务 | 保持现有技术栈，重点关注类型定义与后端DTO对齐 |
| Axios | Spring Cloud Gateway | 配置Axios拦截器处理JWT认证和统一响应格式，通过API网关访问所有服务 |
| React Router 7 | RESTful API | 确保路由与后端API路径匹配 |
| Context API | Spring Security | 实现前端认证状态管理 |
| WebSocket | Spring WebSocket | 集成WebSocket客户端实现实时通信，通过API网关转发 |

---

## 四、API接口适配

### 4.1 基础配置

前端通过 API 网关（`http://localhost:8080/typingWord`）统一访问后端微服务：

```typescript
// src/db/client.ts
import axios from 'axios';

const API_BASE_URL = '/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：自动带上 Token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// 响应拦截器：处理统一响应结构和错误
apiClient.interceptors.response.use((response) => {
  const res = response.data as ResultMessage<any>;
  if (res.code && res.code !== 200) {
    const error = new Error(res.msg || '请求失败') as ApiError;
    error.code = res.code;
    error.data = res.data;
    return Promise.reject(error);
  }
  return response;
}, (error) => {
  let apiError = error as ApiError;
  
  if (error.response) {
    const status = error.response.status;
    const res = error.response.data as ResultMessage<any>;
    
    apiError.code = res?.code || status;
    apiError.message = res?.msg || res?.message || getDefaultErrorMessage(status);
    apiError.data = res?.data;
    
    if (status === 401) {
      localStorage.removeItem('auth_token');
    }
  } else if (error.request) {
    apiError.message = '网络连接失败，请检查网络';
    apiError.code = 0;
  }
  
  return Promise.reject(apiError);
});

export interface ApiError extends Error {
  code?: number;
  data?: any;
}

function getDefaultErrorMessage(status: number): string {
  const messages: Record<number, string> = {
    400: '请求参数错误',
    401: '未登录或登录已过期',
    403: '无权访问',
    404: '资源未找到',
    500: '服务器内部错误',
  };
  return messages[status] || '请求失败';
}
```

### 4.2 核心API模块

| 功能模块 | 前端服务文件 | 主要API接口 | 后端服务 | 后端Controller |
| :--- | :--- | :--- | :--- | :--- |
| 用户认证 | `src/services/auth.ts` | `/api/v1/auth/login`, `/api/v1/auth/signup`, `/api/v1/auth/refresh` | user-svc | `AuthController` |
| 用户管理 | `src/services/user.ts` | `/api/v1/user/profile`, `/api/v1/user/avatar` | user-svc | `UserController` |
| 好友系统 | `src/services/social.ts` | `/api/v1/friends`, `/api/v1/friends/request` | user-svc | `FriendController` |
| 游戏记录 | `src/services/game.ts` | `/api/v1/game/records`, `/api/v1/game/config/next-round` | game-core-svc | `GameRecordController`, `GameConfigController` |
| 字符统计 | `src/services/stats.ts` | `/api/v1/stats/character`, `/api/v1/stats/character/user/{userId}` | game-core-svc | `StatsController` |
| 词库管理 | `src/services/dictionary.ts` | `/api/v1/dictionary/custom`, `/api/v1/dictionary/system` | chat-svc | `DictionaryController` |
| 游戏配置 | `src/services/game.ts` | `/api/v1/game/config/next-round`, `/api/v1/game/config/char-types` | game-core-svc | `GameConfigController` |
| 排行榜 | `src/services/game.ts` | `/api/v1/leaderboard/{gameMode}` | chat-svc | `LeaderboardController` |
| 遗传进化 | `src/services/game.ts` | `/api/v1/game/config/next-round` | game-core-svc | `GameConfigController` |
| 消息系统 | `src/services/social.ts` | `/api/v1/messages/conversations`, `/api/v1/messages/send` | chat-svc | `MessageController` |
| AI对话 | `src/services/ai.ts` | `/api/v1/ai/chat`, `/api/v1/ai/training-summary` | ai-svc | `AiController` |

### 4.3 API路由表

| API路径前缀 | 目标服务 | 职责 |
|-------------|----------|------|
| `/api/v1/auth/**` | user-svc | 认证 |
| `/api/v1/user/**` | user-svc | 用户管理 |
| `/api/v1/friends/**` | user-svc | 好友管理 |
| `/api/v1/game/**` | game-core-svc | 游戏核心 |
| `/api/v1/stats/**` | game-core-svc | 统计分析 |
| `/api/v1/ai/**` | ai-svc | AI 助手 |
| `/api/v1/messages/**` | chat-svc | 消息中心 |
| `/api/v1/leaderboard/**` | chat-svc | 排行榜 |
| `/api/v1/dictionary/**` | chat-svc | 词库管理 |

---

## 五、数据模型适配

### 5.1 核心类型定义

#### 5.1.1 认证相关类型

```typescript
// src/types/auth.ts
export interface UserLoginDTO {
  username: string;
  password: string;
}

export interface UserRegisterDTO {
  username: string;
  password: string;
  nickname?: string;
}

export interface TokenResponseDTO {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserInfo;
}

export interface UserInfo {
  userId: number;
  username: string;
  nickname: string;
  signature?: string;
  avatar?: string;
  bestScore: number;
  bestRound: number;
  bestSpeed: number;
  bestAccuracy: number;
  avgSpeed: number;
  avgAccuracy: number;
  peakAccuracy: number;
  gameNum: number;
  role: 'user' | 'admin';
  createTime: string;
  updateTime: string;
}
```

#### 5.1.2 游戏相关类型

```typescript
// src/types/game.ts
export interface GameRecord {
  recordId: number;
  userId: number;
  gameMode: 'endless' | 'dictionary' | 'limit';
  selectedChars?: string;
  roundNum: number;
  totalScore: number;
  totalChars: number;
  correctChars: number;
  avgSpeed: number;
  peakSpeed: number;
  avgAccuracy: number;
  peakAccuracy: number;
  gameDuration: number;
  createTime: string;
}

export interface MonsterData {
  monsterId: number;
  recordId: number;
  monsterType: 'NORMAL' | 'ATTACKER' | 'EVASIVE' | 'SMART';
  targetStr: string;
  speed: number;
  survivalTime: number;
  destroyCostTime?: number;
  totalCount: number;
  inputErrorCount: number;
  isTrueMean: boolean;
  score: number;
  spawnWave?: number;
  isParent: boolean;
  difficultyLevel: number;
}

export interface NextRoundResponseDTO {
  adjustParam: {
    currentDifficulty: number;
    monsterSpeed: number;
    monsterCount: number;
    avgStringLength: number;
    normalRatio: number;
    attackRatio: number;
    evasiveRatio: number;
    smartRatio: number;
  };
  monsters?: MonsterData[];
}
```

#### 5.1.3 统一响应格式

```typescript
// src/types/common.ts
export interface ResultMessage<T = any> {
  code: number;
  msg: string;
  data: T;
}

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export enum ErrorCode {
  SUCCESS = 200,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  INTERNAL_SERVER_ERROR = 500,
  VALIDATION_ERROR = 422,
  USER_NOT_FOUND = 1001,
  PASSWORD_INCORRECT = 1002,
  GAME_NOT_FOUND = 2001,
  INVALID_GAME_STATE = 2002,
}
```

---

## 六、认证机制适配

### 6.1 认证状态管理

```typescript
// src/contexts/AuthContext.tsx
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { Profile } from '@/types/database';
import { getProfile } from '@/db/api';
import { apiClient, type ApiError } from '@/db/client';

interface AuthContextType {
  user: Profile | null;
  profile: Profile | null;
  loading: boolean;
  signIn: (username: string, password: string) => Promise<void>;
  signUp: (username: string, password: string, nickname?: string) => Promise<void>;
  signOut: () => Promise<void>;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<Profile | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkSession();
  }, []);

  async function checkSession() {
    const token = localStorage.getItem('auth_token');
    if (token) {
      try {
        const response = await apiClient.get('/auth/me');
        const userData = response.data.data;
        
        const initialProfile = userData as unknown as Profile;
        setUser(initialProfile);
        setProfile(initialProfile);

        const userId = userData.userId || (userData as any).id;
        
        if (userId) {
          await loadProfile(userId);
        }
      } catch (error) {
        const apiError = error as ApiError;
        if (apiError.code === 401) {
          localStorage.removeItem('auth_token');
        }
        setUser(null);
        setProfile(null);
      }
    }
    setLoading(false);
  }

  async function loadProfile(userId: number) {
    try {
      const profileData = await getProfile(userId);
      setProfile(profileData);
    } catch (error) {
      console.error('Failed to load profile:', error);
    }
  }

  async function signIn(username: string, password: string) {
    try {
      const response = await apiClient.post('/auth/login', { username, password });
      const { accessToken, user: userData } = response.data.data;
      
      localStorage.setItem('auth_token', accessToken);
      
      const initialProfile = userData as unknown as Profile;
      setUser(initialProfile);
      setProfile(initialProfile);
      
      const userId = userData.userId || (userData as any).id;
      if (userId) {
        await loadProfile(userId);
      }
    } catch (error) {
      const apiError = error as ApiError;
      throw new Error(apiError.message || '登录失败');
    }
  }

  async function signUp(username: string, password: string, nickname?: string) {
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      throw new Error('用户名只能包含字母、数字和下划线');
    }

    try {
      const response = await apiClient.post('/auth/signup', { 
        username, 
        password,
        nickname: nickname || username 
      });
      const { accessToken, user: userData } = response.data.data;
      
      localStorage.setItem('auth_token', accessToken);
      
      const initialProfile = userData as unknown as Profile;
      setUser(initialProfile);
      setProfile(initialProfile);
      
      const userId = userData.userId || (userData as any).id;
      if (userId) {
        await loadProfile(userId);
      }
    } catch (error) {
      const apiError = error as ApiError;
      throw new Error(apiError.message || '注册失败');
    }
  }

  async function signOut() {
    localStorage.removeItem('auth_token');
    setUser(null);
    setProfile(null);
  }

  const isAdmin = user?.role === 'admin';

  const value: AuthContextType = {
    user,
    profile,
    loading,
    signIn,
    signUp,
    signOut,
    isAdmin,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

### 6.2 受保护路由

```typescript
// src/components/common/ProtectedRoute.tsx
import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

interface ProtectedRouteProps {
  requiredRole?: 'user' | 'admin';
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ requiredRole }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRole === 'admin' && user.role !== 'admin') {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};
```

---

## 七、WebSocket集成

### 7.1 WebSocket配置

前端通过 API 网关访问 WebSocket 服务：

- **端点**: `ws://localhost:8080/typingWord/ws`
- **订阅主题**: `/topic/messages/{conversationId}`

```typescript
// src/services/websocket.ts
import { Client } from '@stomp/stompjs';

class WebSocketService {
  private client: Client | null = null;
  private subscribers: Map<string, ((message: any) => void)[]> = new Map();

  connect(token: string) {
    const baseUrl = import.meta.env.VITE_BACKEND_BASE_URL || 'http://localhost:8080/typingWord';
    const wsUrl = baseUrl.replace('http://', 'ws://').replace('https://', 'wss://') + '/ws';
    
    this.client = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        this.subscribers.forEach((callbacks, topic) => {
          this.client?.subscribe(topic, (message) => {
            const parsedMessage = JSON.parse(message.body);
            callbacks.forEach(callback => callback(parsedMessage));
          });
        });
      },
      onDisconnect: () => console.log('WebSocket disconnected'),
      onStompError: (frame) => console.error('WebSocket error:', frame),
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  subscribe(topic: string, callback: (message: any) => void) {
    if (!this.subscribers.has(topic)) {
      this.subscribers.set(topic, []);
      if (this.client?.connected) {
        this.client.subscribe(topic, (message) => {
          const parsedMessage = JSON.parse(message.body);
          this.subscribers.get(topic)?.forEach(cb => cb(parsedMessage));
        });
      }
    }
    this.subscribers.get(topic)?.push(callback);
  }

  unsubscribe(topic: string, callback: (message: any) => void) {
    if (this.subscribers.has(topic)) {
      const callbacks = this.subscribers.get(topic)?.filter(cb => cb !== callback);
      if (callbacks?.length) {
        this.subscribers.set(topic, callbacks);
      } else {
        this.subscribers.delete(topic);
        this.client?.unsubscribe(topic);
      }
    }
  }

  send(destination: string, body: any) {
    if (this.client?.connected) {
      this.client.publish({ destination, body: JSON.stringify(body) });
    }
  }
}

export const websocketService = new WebSocketService();
```

---

## 八、游戏流程适配

### 8.1 游戏开始流程

```
前端流程：
1. 用户进入准备页 (PreparePage)
2. 选择游戏模式和字符类型
3. 点击「开始游戏」，调用 POST /api/v1/game/config/next-round
4. 获取到参数后初始化游戏引擎和自适应系统
5. 启动游戏，开始接收用户输入

后端流程（game-core-svc）：
1. 接收 next-round 请求 (GameConfigController)
2. 构建调节参数，根据用户历史数据计算难度
3. 调用 EvolutionService.generateNextRoundMonsters 生成首轮怪物
4. 根据游戏模式获取外部词源（词库模式）
5. 生成怪物列表并返回给前端
```

### 8.2 游戏进行流程

```
前端流程：
1. 处理用户键盘输入，更新游戏状态
2. AdaptiveSystem 根据用户表现实时调整难度
3. 每轮结束时，调用 POST /api/v1/game/config/next-round 获取下一轮参数
4. 应用新的难度参数到游戏引擎
5. 实时更新游戏统计数据

后端流程（game-core-svc）：
1. 接收 next-round 请求
2. 使用遗传算法生成下一轮内容：
   - 选择父本怪物
   - 基因重组与变异生成新单词
   - 根据用户表现调整难度
3. 返回新的怪物列表和难度参数
```

### 8.3 游戏结束流程

```
前端流程：
1. 游戏结束时收集游戏数据：
   - 怪物历史记录
   - 字符统计数据
2. 按字符展开为本局每条输入，用于更新 character_stats
3. 调用 POST /api/v1/game/records 上传游戏记录
4. 显示游戏结果页面，包括详细的统计数据和易错字符分析

后端流程（game-core-svc）：
1. 接收游戏记录数据 (GameRecordController)
2. 保存游戏记录和怪物数据
3. 在无尽适应模式下，使用遗传算法选择父本并保存
4. 使用数学模型计算并更新难度系数
5. 记录适配日志，保存难度调整历史
6. 返回结算摘要，包含当前难度系数
```

---

## 九、错误处理适配

### 9.1 统一响应格式

后端返回统一的 `ResultMessage` 格式：

```typescript
export interface ResultMessage<T = any> {
  code: number;      // 状态码，200 表示成功
  msg: string;       // 消息描述
  data: T;          // 数据内容
}
```

### 9.2 前端统一错误处理

前端已实现统一的错误处理机制：

| 层级 | 文件 | 职责 |
| :--- | :--- | :--- |
| 拦截器 | `db/client.ts` | 响应拦截器自动识别后端 `ResultMessage` 格式，转换为 `ApiError` |
| 工具函数 | `utils/index.ts` | 提供 `isApiError`、`getErrorMessage`、`handleApiError`、`isAuthError` |
| 认证上下文 | `AuthContext.tsx` | 自动处理 401 认证过期，清理 token |

#### 工具函数示例

```typescript
// src/utils/index.ts
export interface ApiError {
  code?: number;
  message: string;
  data?: any;
}

export function isApiError(error: unknown): error is ApiError {
  return typeof error === 'object' && error !== null && 'message' in error;
}

export function getErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return '未知错误';
}

export function handleApiError(error: unknown, defaultMessage?: string): string {
  const message = getErrorMessage(error);
  return message || defaultMessage || '操作失败';
}

export function isAuthError(error: unknown): boolean {
  if (isApiError(error)) {
    return error.code === 401;
  }
  return false;
}
```

---

## 十、环境变量配置

### 10.1 前端环境变量

| 变量 | 说明 | 默认值 |
| :--- | :--- | :--- |
| VITE_BACKEND_BASE_URL | API 网关地址 | `http://localhost:8080/typingWord` |

### 10.2 Vite 代理配置

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080/typingWord',
        changeOrigin: true,
        rewrite: (path) => path,
      },
      '/ws': {
        target: 'ws://localhost:8080/typingWord',
        ws: true,
        changeOrigin: true,
      },
    },
  },
});
```

---

## 十一、联调步骤

### 11.1 开发环境配置

1. **启动基础设施**（使用 Docker Compose）

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
   ```

3. **前端配置**

   设置正确的 `VITE_BACKEND_BASE_URL`（默认 `http://localhost:8080/typingWord`）

4. **启动前端**

   ```bash
   cd app-64
   pnpm install
   pnpm dev
   ```

### 11.2 联调测试清单

| 优先级 | 测试模块 | 测试内容 | 对应服务 |
| :--- | :--- | :--- | :--- |
| P0 | 认证与授权 | 登录/注册功能，JWT认证 | user-svc |
| P0 | 游戏核心功能 | 游戏流程完整，与后端API交互正常 | game-core-svc |
| P1 | 统计与分析 | 数据统计和图表展示正常 | game-core-svc |
| P1 | 遗传进化算法 | 进化算法调用正常，难度调整生效 | game-core-svc |
| P2 | 好友系统 | 好友添加、聊天功能正常 | user-svc / chat-svc |
| P2 | AI对话 | AI对话功能正常 | ai-svc |
| P3 | 管理员功能 | 管理员权限和功能正常 | 各服务 |

### 11.3 问题排查

| 工具 | 用途 |
| :--- | :--- |
| 浏览器开发者工具 | 查看网络请求、响应、错误信息 |
| 后端日志 | 查看各服务的请求日志、错误日志 |
| Nacos 控制台 | 查看服务注册状态 |
| Zipkin | 查看分布式链路追踪 |

---

## 十二、风险与应对措施

| 风险 | 应对措施 |
| :--- | :--- |
| API接口变更 | 建立API版本控制机制，定期同步API文档 |
| 数据类型不匹配 | 使用TypeScript严格类型检查，建立类型定义同步机制 |
| 认证机制复杂 | 封装认证逻辑，提供统一的认证服务 |
| WebSocket连接不稳定 | 实现重连机制，处理连接异常情况 |
| 业务逻辑差异 | 建立前后端业务规则文档，定期沟通确认 |
| 服务不可用 | API网关配置熔断和降级策略 |

---

## 十三、后续维护

| 维护项 | 说明 |
| :--- | :--- |
| API文档更新 | 定期更新API文档，确保前后端一致 |
| 类型定义同步 | 后端DTO变更时，及时更新前端类型定义 |
| 错误码管理 | 建立统一的错误码管理机制 |
| 性能监控 | 添加前后端性能监控，及时发现问题 |
| 定期联调 | 定期进行前后端联调，确保系统稳定运行 |
| 服务健康检查 | 在前端添加服务健康状态展示 |