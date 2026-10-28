import { lazy } from 'react';
import type { RouteConfig } from '@/routes';
import { workbenchApps } from './registry';

const LauncherPage = lazy(() => import('@/pages/LauncherPage'));
const LoginPage = lazy(() => import('@/pages/LoginPage'));
const RegisterPage = lazy(() => import('@/pages/RegisterPage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));

function alias(from: string, to: string): RouteConfig {
  return {
    name: `alias:${from}`,
    path: from,
    element: lazy(() =>
      import('./LegacyRedirect').then((mod) => ({
        default: function BoundAlias() {
          return <mod.default to={to} />;
        },
      }))
    ),
    visible: false,
  };
}

/** 工作台路由：启动器 + /apps/* + 旧路径别名。 */
export function buildWorkbenchRoutes(): RouteConfig[] {
  const appRoutes: RouteConfig[] = [];
  for (const app of workbenchApps) {
    for (const route of app.routes) {
      const fullPath = route.path ? `${app.basePath}/${route.path}` : app.basePath;
      appRoutes.push({
        name: `${app.title}/${route.name}`,
        path: fullPath,
        element: route.element,
        requireAuth: route.requireAuth,
        requireAdmin: route.requireAdmin,
        visible: false,
      });
    }
  }

  const legacy: RouteConfig[] = [
    alias('/prepare', '/apps/typing/prepare'),
    alias('/game', '/apps/typing/game'),
    alias('/result', '/apps/typing/result'),
    alias('/profile', '/apps/typing/profile'),
    alias('/trends', '/apps/typing/trends'),
    alias('/leaderboard', '/apps/typing/leaderboard'),
    alias('/dictionary', '/apps/typing/dictionary'),
    alias('/lab', '/apps/lab'),
    alias('/lab/:algorithmId', '/apps/lab/:algorithmId'),
    alias('/ai-assistant', '/apps/assistant'),
    alias('/messages', '/apps/social/messages'),
    alias('/friends', '/apps/social/friends'),
    alias('/chat/:friendId', '/apps/social/chat/:friendId'),
    alias('/settings', '/apps/settings'),
    alias('/admin', '/apps/admin'),
  ];

  return [
    {
      name: '启动器',
      path: '/',
      element: LauncherPage,
      requireAuth: false,
      visible: false,
    },
    {
      name: '登录',
      path: '/login',
      element: LoginPage,
      requireAuth: false,
      visible: false,
    },
    {
      name: '注册',
      path: '/register',
      element: RegisterPage,
      requireAuth: false,
      visible: false,
    },
    ...appRoutes,
    ...legacy,
    {
      name: '404',
      path: '*',
      element: NotFoundPage,
      requireAuth: false,
      visible: false,
    },
  ];
}
