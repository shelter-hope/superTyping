// 路由配置
import { lazy, type ComponentType, type LazyExoticComponent } from 'react';

// 懒加载页面组件
const HomePage = lazy(() => import('@/pages/HomePage'));
const LoginPage = lazy(() => import('@/pages/LoginPage'));
const RegisterPage = lazy(() => import('@/pages/RegisterPage'));
const PreparePage = lazy(() => import('@/pages/PreparePage'));
const GamePage = lazy(() => import('@/pages/GamePage'));
const ResultPage = lazy(() => import('@/pages/ResultPage'));
const ProfilePage = lazy(() => import('@/pages/ProfilePage'));
const TrendsPage = lazy(() => import('@/pages/TrendsPage'));
const LeaderboardPage = lazy(() => import('@/pages/LeaderboardPage'));
const DictionaryPage = lazy(() => import('@/pages/DictionaryPage'));
const AIAssistantPage = lazy(() => import('@/pages/AIAssistantPage'));
const MessageCenterPage = lazy(() => import('@/pages/MessageCenterPage'));
const FriendsPage = lazy(() => import('@/pages/FriendsPage'));
const ChatPage = lazy(() => import('@/pages/ChatPage'));
const SettingsPage = lazy(() => import('@/pages/SettingsPage'));
const AdminPage = lazy(() => import('@/pages/AdminPage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));

export interface RouteConfig {
  name: string;
  path: string;
  element: LazyExoticComponent<ComponentType>;
  requireAuth?: boolean;
  requireAdmin?: boolean;
  visible?: boolean;
}

const routes: RouteConfig[] = [
  {
    name: '首页',
    path: '/',
    element: HomePage,
    requireAuth: false,
    visible: true,
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
  {
    name: '准备',
    path: '/prepare',
    element: PreparePage,
    requireAuth: true,
    visible: false,
  },
  {
    name: '游戏',
    path: '/game',
    element: GamePage,
    requireAuth: true,
    visible: false,
  },
  {
    name: '结算',
    path: '/result',
    element: ResultPage,
    requireAuth: true,
    visible: false,
  },
  {
    name: '个人信息',
    path: '/profile',
    element: ProfilePage,
    requireAuth: true,
    visible: true,
  },
  {
    name: '趋势分析',
    path: '/trends',
    element: TrendsPage,
    requireAuth: true,
    visible: true,
  },
  {
    name: '排行榜',
    path: '/leaderboard',
    element: LeaderboardPage,
    requireAuth: false,
    visible: true,
  },
  {
    name: '词库管理',
    path: '/dictionary',
    element: DictionaryPage,
    requireAuth: true,
    visible: true,
  },
  {
    name: 'AI助手',
    path: '/ai-assistant',
    element: AIAssistantPage,
    requireAuth: true,
    visible: true,
  },
  {
    name: '消息中心',
    path: '/messages',
    element: MessageCenterPage,
    requireAuth: true,
    visible: true,
  },
  {
    name: '好友管理',
    path: '/friends',
    element: FriendsPage,
    requireAuth: true,
    visible: true,
  },
  {
    name: '聊天',
    path: '/chat/:friendId',
    element: ChatPage,
    requireAuth: true,
    visible: false,
  },
  {
    name: '设置',
    path: '/settings',
    element: SettingsPage,
    requireAuth: true,
    visible: true,
  },
  {
    name: '管理',
    path: '/admin',
    element: AdminPage,
    requireAuth: true,
    requireAdmin: true,
    visible: false,
  },
  {
    name: '404',
    path: '*',
    element: NotFoundPage,
    requireAuth: false,
    visible: false,
  },
];

export default routes;
