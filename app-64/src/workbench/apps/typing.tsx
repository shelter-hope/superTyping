import { lazy } from 'react';
import type { WorkbenchAppManifest } from '../types';

const HomePage = lazy(() => import('@/pages/HomePage'));
const PreparePage = lazy(() => import('@/pages/PreparePage'));
const GamePage = lazy(() => import('@/pages/GamePage'));
const ResultPage = lazy(() => import('@/pages/ResultPage'));
const ProfilePage = lazy(() => import('@/pages/ProfilePage'));
const TrendsPage = lazy(() => import('@/pages/TrendsPage'));
const LeaderboardPage = lazy(() => import('@/pages/LeaderboardPage'));
const DictionaryPage = lazy(() => import('@/pages/DictionaryPage'));

export const typingApp: WorkbenchAppManifest = {
  id: 'typing',
  title: '打字进化',
  blurb: '对局、词库、趋势与个人档案',
  icon: 'keyboard',
  order: 10,
  enabled: true,
  basePath: '/apps/typing',
  homePath: '/apps/typing',
  routes: [
    { path: '', name: '概览', element: HomePage, appNav: true },
    { path: 'prepare', name: '准备', element: PreparePage, requireAuth: true, immersive: false },
    { path: 'game', name: '对局', element: GamePage, requireAuth: true, immersive: true },
    { path: 'result', name: '结算', element: ResultPage, requireAuth: true },
    { path: 'profile', name: '档案', element: ProfilePage, requireAuth: true, appNav: true },
    { path: 'trends', name: '趋势', element: TrendsPage, requireAuth: true, appNav: true },
    { path: 'leaderboard', name: '排行', element: LeaderboardPage, appNav: true },
    { path: 'dictionary', name: '词库', element: DictionaryPage, requireAuth: true, appNav: true },
  ],
  activityProvider: async (ctx) => {
    const userId = ctx.user?.userId;
    if (!userId) return [];
    try {
      const { getProgressTrend } = await import('@/services/stats');
      const trend = await getProgressTrend(userId, 'week');
      const rows = Array.isArray(trend) ? trend : trend?.data || trend?.points || [];
      const points = (Array.isArray(rows) ? rows : []).slice(-3).reverse();
      return points.map((point: { date?: string; day?: string; accuracy?: number; speed?: number }, index: number) => {
        const date = point.date || point.day || '';
        return {
          id: `typing-trend-${date || index}`,
          appId: 'typing',
          title: '最近训练',
          subtitle: `${date} · 准度 ${point.accuracy ?? '—'} · 速度 ${point.speed ?? '—'}`,
          href: '/apps/typing/trends',
          at: date ? `${date}T12:00:00` : new Date().toISOString(),
        };
      });
    } catch {
      return [];
    }
  },
};
