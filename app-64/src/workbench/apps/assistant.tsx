import { lazy } from 'react';
import type { WorkbenchAppManifest } from '../types';

const AIAssistantPage = lazy(() => import('@/pages/AIAssistantPage'));

export const assistantApp: WorkbenchAppManifest = {
  id: 'assistant',
  title: 'AI 助手',
  blurb: '训练摘要与对话辅导',
  icon: 'bot',
  order: 40,
  enabled: (ctx) => !!ctx.user,
  basePath: '/apps/assistant',
  homePath: '/apps/assistant',
  routes: [{ path: '', name: '助手', element: AIAssistantPage, requireAuth: true, appNav: true }],
};
