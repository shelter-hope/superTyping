import type { ComponentType, LazyExoticComponent } from 'react';

export type WorkbenchUserCtx = {
  user: { userId?: number } | null;
  isAdmin: boolean;
};

export type WorkbenchRoute = {
  path: string;
  name: string;
  element: LazyExoticComponent<ComponentType>;
  requireAuth?: boolean;
  requireAdmin?: boolean;
  nav?: boolean;
  immersive?: boolean;
  /** 是否出现在旧顶栏式应用内导航（typing 等） */
  appNav?: boolean;
};

export type ActivityItem = {
  id: string;
  appId: string;
  title: string;
  subtitle?: string;
  href: string;
  at: string;
};

export type WorkbenchAppManifest = {
  id: string;
  title: string;
  blurb: string;
  icon: string;
  order: number;
  enabled: boolean | ((ctx: WorkbenchUserCtx) => boolean);
  basePath: string;
  homePath: string;
  routes: WorkbenchRoute[];
  activityProvider?: (ctx: WorkbenchUserCtx) => Promise<ActivityItem[]>;
};
