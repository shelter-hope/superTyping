import { lazy } from 'react';
import type { WorkbenchAppManifest } from '../types';

const LabPage = lazy(() => import('@/pages/LabPage'));
const LabHistoryPage = lazy(() => import('@/pages/lab/LabHistoryPage'));

export const labApp: WorkbenchAppManifest = {
  id: 'lab',
  title: '群体智能',
  blurb: '算法演示与实验快照历史',
  icon: 'flask',
  order: 20,
  enabled: true,
  basePath: '/apps/lab',
  homePath: '/apps/lab',
  routes: [
    { path: '', name: '实验台', element: LabPage, appNav: true },
    { path: 'history', name: '历史', element: LabHistoryPage, requireAuth: true, appNav: true },
    { path: ':algorithmId', name: '算法', element: LabPage },
  ],
  activityProvider: async (ctx) => {
    if (!ctx.user) return [];
    try {
      const { fetchLabRuns } = await import('@/services/lab');
      const page = await fetchLabRuns({ page: 0, size: 5 });
      return (page.content || []).map((run) => ({
        id: `lab-run-${run.id}`,
        appId: 'lab',
        title: run.title || `实验 #${run.id}`,
        subtitle: `${run.algorithmId} · seed ${run.seed}`,
        href: '/apps/lab/history',
        at: run.createdAt || new Date().toISOString(),
      }));
    } catch {
      return [];
    }
  },
};
