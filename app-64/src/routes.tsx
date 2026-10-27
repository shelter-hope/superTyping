// 路由配置：工作台 Manifest 展开 + 壳层登录/启动器
import type { ComponentType, LazyExoticComponent } from 'react';
import { buildWorkbenchRoutes } from '@/workbench/buildRoutes';

export interface RouteConfig {
  name: string;
  path: string;
  element: LazyExoticComponent<ComponentType>;
  requireAuth?: boolean;
  requireAdmin?: boolean;
  visible?: boolean;
}

const routes: RouteConfig[] = buildWorkbenchRoutes();

export default routes;
