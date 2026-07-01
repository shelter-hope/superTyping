# 前后端联调文档

## 一、项目概述

### 1.1 前端项目现状

| 项目信息 | 说明 |
| :--- | :--- |
| 项目名称 | miaoda-react-admin |
| 项目路径 | `app-64` |
| 技术栈 | React 18 + TypeScript + Vite + Tailwind CSS |
| 主要功能 | 打字游戏、AI对话、统计分析、个人资料管理、管理员功能 |

### 1.2 后端项目概况

| 项目信息 | 说明 |
| :--- | :--- |
| 技术栈 | Spring Boot 3.x + Spring Security + JWT + Spring Data JPA + Redis + WebSocket |
| 数据库 | MySQL 8.0+ |
| API风格 | RESTful API + WebSocket |
| 主要功能 | 用户认证、游戏记录管理、字符统计、词库管理、遗传进化算法、好友系统、AI对话系统 |

---

## 二、适配目标

| 目标 | 说明 |
| :--- | :--- |
| API对齐 | 确保前端API调用与后端接口定义一致 |
| 数据模型匹配 | 前端数据结构与后端DTO/Entity保持一致 |
| 认证机制集成 | 实现JWT认证与授权 |
| WebSocket集成 | 支持实时通信功能 |
| 业务逻辑适配 | 确保前端业务流程与后端业务规则匹配 |
| 错误处理统一 | 前后端错误码和错误信息格式统一 |

---

## 三、技术栈适配

| 前端技术 | 后端对应技术 | 适配说明 |
| :--- | :--- | :--- |
| React 18 + TypeScript | Spring Boot 3.x | 保持现有技术栈，重点关注类型定义与后端DTO对齐 |
| Axios | Spring MVC | 配置Axios拦截器处理JWT认证和统一响应格式 |
| React Router 7 | RESTful API | 确保路由与后端API路径匹配 |
| Context API | Spring Security | 实现前端认证状态管理 |
| WebSocket | Spring WebSocket | 集成WebSocket客户端实现实时通信 |

---

## 四、API接口适配

### 4.1 基础配置

```typescript
// src/services/api.ts
import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
);

export default api;
```

### 4.2 核心API模块

| 功能模块 | 前端服务文件 | 主要API接口 | 后端Controller |
| :--- | :--- | :--- | :--- |
| 用户认证 | `src/services/auth.ts` | `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/change-password` | `AuthController` |
| 用户管理 | `src/services/user.ts` | `/api/users/{userId}`, `/api/users/{userId}/avatar`, `/api/users/{userId}/stats` | `UserController` |
| 游戏记录 | `src/services/gameRecord.ts` | `/api/game/records`, `/api/game/records/user/{userId}` | `GameRecordController` |
| 字符统计 | `src/services/characterStats.ts` | `/api/stats/character`, `/api/stats/character/user/{userId}` | `CharacterStatsController` |
| 词库管理 | `src/services/dictionary.ts` | `/api/dictionary/custom`, `/api/dictionary/system` | `DictionaryController` |
| 游戏配置 | `src/services/gameConfig.ts` | `/api/game/config/next-round`, `/api/game/config/char-types` | `GameConfigController` |
| 排行榜 | `src/services/leaderboard.ts` | `/api/leaderboard/{gameMode}` | `LeaderboardController` |
| 遗传进化 | `src/services/evolution.ts` | `/api/game/config/next-round` | `GameConfigController` |
| 好友系统 | `src/services/friend.ts` | `/api/friends`, `/api/friends/request` | `FriendController` |
| 消息系统 | `src/services/message.ts` | `/api/messages/conversations`, `/api/messages/send` | `MessageController` |
| AI对话 | `src/services/aiChat.ts` | `/api/ai-chat/conversations`, `/api/ai-chat/conversation/{conversationId}/send` | `AiChatController` |

---

## 五、数据模型适配

### 5.1 核心类型定义

#### 5.1.1 认证相关类型

```typescript
// src/types/auth.ts
export interface UserRegisterDTO {
  username: string;
  password: string;
  nickname: string;
}

export interface UserLoginDTO {
  username: string;
  password: string;
}

export interface TokenResponseDTO {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}
```

#### 5.1.2 用户相关类型

```typescript
// src/types/user.ts
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

#### 5.1.3 游戏相关类型

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

---

## 六、认证机制适配

### 6.1 认证状态管理

```typescript
// src/contexts/AuthContext.tsx
import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { UserInfo, TokenResponseDTO } from '../types';
import { login, refreshToken } from '../services/auth';

interface AuthContextType {
  user: UserInfo | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  updateUser: (user: UserInfo) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const storedToken = localStorage.getItem('token');
    if (storedUser && storedToken) {
      setUser(JSON.parse(storedUser));
      setIsAuthenticated(true);
      setupTokenRefresh();
    }
  }, []);

  const setupTokenRefresh = () => {
    const refreshInterval = setInterval(async () => {
      try {
        const response = await refreshToken();
        localStorage.setItem('token', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
      } catch (error) {
        console.error('Token refresh failed:', error);
        logout();
      }
    }, 45 * 60 * 1000);

    return () => clearInterval(refreshInterval);
  };

  const handleLogin = async (username: string, password: string) => {
    const response = await login(username, password);
    localStorage.setItem('token', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.user));
    setUser(response.user);
    setIsAuthenticated(true);
    setupTokenRefresh();
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
    setIsAuthenticated(false);
  };

  const updateUser = (updatedUser: UserInfo) => {
    setUser(updatedUser);
    localStorage.setItem('user', JSON.stringify(updatedUser));
  };

  const value = { user, isAuthenticated, login: handleLogin, logout, updateUser };

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
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};
```

---

## 七、WebSocket集成

```typescript
// src/services/websocket.ts
import { Client } from '@stomp/stompjs';

class WebSocketService {
  private client: Client | null = null;
  private subscribers: Map<string, ((message: any) => void)[]> = new Map();

  connect(token: string) {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
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
3. 点击「开始游戏」，调用 getNextRound(1, gameMode, customDictIds, systemDictIds)
4. 获取到参数后初始化游戏引擎和自适应系统
5. 启动游戏，开始接收用户输入

后端流程：
1. 接收 getNextRound 请求 (GameConfigController)
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
3. 每轮结束时，调用 getNextRound 获取下一轮参数
4. 应用新的难度参数到游戏引擎
5. 实时更新游戏统计数据

后端流程：
1. 接收 getNextRound 请求
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
3. 调用 createGameRecord API 上传游戏记录
4. 显示游戏结果页面，包括详细的统计数据和易错字符分析

后端流程：
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

```typescript
// src/types/common.ts
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

### 9.2 Axios响应拦截器优化

```typescript
// src/services/api.ts (扩展)
import axios, { AxiosError } from 'axios';
import { ApiResponse, ErrorCode } from '../types/common';

api.interceptors.response.use(
  (response) => {
    const apiResponse = response.data as ApiResponse;
    if (apiResponse.code !== ErrorCode.SUCCESS) {
      throw new Error(apiResponse.message);
    }
    return apiResponse.data;
  },
  (error: AxiosError) => {
    const response = error.response?.data as ApiResponse;
    if (response) {
      const errorMessage = response.message || '请求失败';
      if (response.code === ErrorCode.UNAUTHORIZED) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
      throw new Error(errorMessage);
    } else {
      throw new Error('网络错误，请稍后重试');
    }
  }
);
```

---

## 十、适配优先级

| 优先级 | 适配模块 | 完成标准 |
| :--- | :--- | :--- |
| 1 | 认证与授权 | 实现JWT认证，登录/注册功能正常 |
| 2 | 游戏核心功能 | 游戏流程完整，与后端API交互正常 |
| 3 | 统计与分析 | 数据统计和图表展示正常 |
| 4 | 遗传进化算法 | 进化算法调用正常，难度调整生效 |
| 5 | 好友系统 | 好友添加、聊天功能正常 |
| 6 | AI对话 | AI对话功能正常 |
| 7 | 管理员功能 | 管理员权限和功能正常 |

---

## 十一、部署与联调

### 11.1 开发环境配置

| 环境变量 | 前端配置 | 后端对应配置 |
| :--- | :--- | :--- |
| VITE_API_BASE_URL | `http://localhost:8080/api` | server.port=8080 |
| VITE_WEBSOCKET_URL | `ws://localhost:8080/ws` | spring.websocket.path=/ws |

### 11.2 联调步骤

1. **环境准备**：启动MySQL、Redis和后端服务
2. **前端配置**：设置正确的API_BASE_URL和WEBSOCKET_URL
3. **API测试**：使用Postman或Swagger测试后端API
4. **前端开发**：按照适配方案实现前端功能
5. **集成测试**：测试前后端集成效果
6. **问题排查**：使用浏览器开发者工具和后端日志排查问题
7. **性能优化**：根据测试结果进行性能优化

---

## 十二、风险与应对措施

| 风险 | 应对措施 |
| :--- | :--- |
| API接口变更 | 建立API版本控制机制，定期同步API文档 |
| 数据类型不匹配 | 使用TypeScript严格类型检查，建立类型定义同步机制 |
| 认证机制复杂 | 封装认证逻辑，提供统一的认证服务 |
| WebSocket连接不稳定 | 实现重连机制，处理连接异常情况 |
| 业务逻辑差异 | 建立前后端业务规则文档，定期沟通确认 |

---

## 十三、后续维护

| 维护项 | 说明 |
| :--- | :--- |
| API文档更新 | 定期更新API文档，确保前后端一致 |
| 类型定义同步 | 后端DTO变更时，及时更新前端类型定义 |
| 错误码管理 | 建立统一的错误码管理机制 |
| 性能监控 | 添加前后端性能监控，及时发现问题 |
| 定期联调 | 定期进行前后端联调，确保系统稳定运行 |