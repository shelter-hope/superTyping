import { lazy } from 'react';
import type { WorkbenchAppManifest } from '../types';

const MessageCenterPage = lazy(() => import('@/pages/MessageCenterPage'));
const FriendsPage = lazy(() => import('@/pages/FriendsPage'));
const ChatPage = lazy(() => import('@/pages/ChatPage'));

export const socialApp: WorkbenchAppManifest = {
  id: 'social',
  title: '社交',
  blurb: '好友、消息与私聊',
  icon: 'users',
  order: 30,
  enabled: (ctx) => !!ctx.user,
  basePath: '/apps/social',
  homePath: '/apps/social/messages',
  routes: [
    { path: 'messages', name: '消息', element: MessageCenterPage, requireAuth: true, appNav: true },
    { path: 'friends', name: '好友', element: FriendsPage, requireAuth: true, appNav: true },
    { path: 'chat/:friendId', name: '聊天', element: ChatPage, requireAuth: true, immersive: true },
  ],
};
