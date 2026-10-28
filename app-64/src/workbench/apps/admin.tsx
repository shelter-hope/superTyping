import { lazy } from 'react';
import type { WorkbenchAppManifest } from '../types';

const AdminPage = lazy(() => import('@/pages/AdminPage'));

export const adminApp: WorkbenchAppManifest = {
  id: 'admin',
  title: '管理台',
  blurb: '运营与规则配置',
  icon: 'shield',
  order: 90,
  enabled: (ctx) => ctx.isAdmin,
  basePath: '/apps/admin',
  homePath: '/apps/admin',
  routes: [{ path: '', name: '管理', element: AdminPage, requireAuth: true, requireAdmin: true, appNav: true }],
};
