import { lazy } from 'react';
import type { WorkbenchAppManifest } from '../types';

const SettingsPage = lazy(() => import('@/pages/SettingsPage'));

export const settingsApp: WorkbenchAppManifest = {
  id: 'settings',
  title: '设置',
  blurb: '账号与偏好',
  icon: 'settings',
  order: 50,
  enabled: (ctx) => !!ctx.user,
  basePath: '/apps/settings',
  homePath: '/apps/settings',
  routes: [{ path: '', name: '设置', element: SettingsPage, requireAuth: true, appNav: true }],
};
