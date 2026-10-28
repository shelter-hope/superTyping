# Workbench App Manifest

工作台壳只负责身份、应用切换与启动器；每个应用自带路由与导航。

## Manifest 契约

```ts
type WorkbenchAppManifest = {
  id: string;                 // typing | lab | social | assistant | settings | admin
  title: string;
  icon: string;               // lucide 名或 emoji 短码，壳层映射图标
  order: number;
  enabled: boolean | ((ctx: { user: unknown; isAdmin: boolean }) => boolean);
  basePath: string;           // /apps/typing
  homePath: string;           // 进入应用的默认路径
  routes: Array<{
    path: string;             // 相对 basePath，如 prepare → /apps/typing/prepare
    name: string;
    requireAuth?: boolean;
    requireAdmin?: boolean;
    nav?: boolean;            // 应用内导航是否展示
    immersive?: boolean;      // 沉浸式：壳 chrome=minimal
  }>;
  permissions?: string[];
  activityProvider?: () => Promise<ActivityItem[]>;
};

type ActivityItem = {
  id: string;
  appId: string;
  title: string;
  subtitle?: string;
  href: string;
  at: string;                 // ISO time
};
```

## 首期应用划分

| appId | basePath | homePath | 迁入 |
|-------|----------|----------|------|
| typing | `/apps/typing` | `/apps/typing` | 原首页营销、prepare/game/result、dictionary、trends、profile、leaderboard |
| lab | `/apps/lab` | `/apps/lab` | Lab 演示、实验历史 |
| social | `/apps/social` | `/apps/social/messages` | friends/messages/chat |
| assistant | `/apps/assistant` | `/apps/assistant` | AI 助手 |
| settings | `/apps/settings` | `/apps/settings` | 设置 |
| admin | `/apps/admin` | `/apps/admin` | 管理台（仅 admin） |

启动器 `/`：应用磁贴 + 跨应用最近活动。登录/注册仍在壳层：`/login`、`/register`。

## 旧路径别名

实现阶段将 `/lab`、`/prepare`、`/game` 等 **Navigate** 到对应 `/apps/...`。书签与外链不立刻失效。

## 壳 UI

- 顶栏左：中性名「实验台」+ AppSwitcher
- 顶栏右：消息（深链 social）、用户菜单
- 沉浸式路径：`chrome=minimal`（对局、聊天、Lab 全屏）
- typing 应用内可保留「打字进化」纸墨品牌；壳不使用游戏营销文案
