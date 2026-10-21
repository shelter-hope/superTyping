// 管理员页面
import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  Users,
  FileText,
  Bug,
  LayoutDashboard,
  TrendingUp,
  SlidersHorizontal,
  BookOpen,
  BarChart3,
  Megaphone,
  ScrollText,
  Settings2,
  RefreshCw,
  Loader2,
} from 'lucide-react';
import UserManagement from '@/components/admin/UserManagement';
import GameRecordManagement from '@/components/admin/GameRecordManagement';
import MonsterConfigManagement from '@/components/admin/MonsterConfigManagement';
import AdaptLogTrend from '@/components/admin/AdaptLogTrend';
import SystemParamsManagement from '@/components/admin/SystemParamsManagement';
import SystemDictionaryManagement from '@/components/admin/SystemDictionaryManagement';
import LeaderboardManagement from '@/components/admin/LeaderboardManagement';
import CharacterStatsOverview from '@/components/admin/CharacterStatsOverview';
import NotificationManagement from '@/components/admin/NotificationManagement';
import OperationLogView from '@/components/admin/OperationLogView';
import AdvancedConfigManagement from '@/components/admin/AdvancedConfigManagement';
import { adminGetOverview, type AdminOverview } from '@/db/api';
import { useToast } from '@/hooks/use-toast';

type TabType =
  | 'dashboard'
  | 'users'
  | 'records'
  | 'monsters'
  | 'trend'
  | 'systemParams'
  | 'systemDicts'
  | 'leaderboard'
  | 'charStats'
  | 'notifications'
  | 'advanced'
  | 'logs';

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');

  const tabs = [
    { id: 'dashboard' as TabType, name: '数据概览', icon: LayoutDashboard },
    { id: 'users' as TabType, name: '用户管理', icon: Users },
    { id: 'records' as TabType, name: '游戏记录', icon: FileText },
    { id: 'monsters' as TabType, name: '怪物配置', icon: Bug },
    { id: 'trend' as TabType, name: 'Dₜ趋势', icon: TrendingUp },
    { id: 'systemParams' as TabType, name: '系统参数', icon: SlidersHorizontal },
    { id: 'advanced' as TabType, name: '高级配置', icon: Settings2 },
    { id: 'systemDicts' as TabType, name: '系统词库', icon: BookOpen },
    { id: 'leaderboard' as TabType, name: '排行榜条目', icon: BarChart3 },
    { id: 'charStats' as TabType, name: '字符统计', icon: BarChart3 },
    { id: 'notifications' as TabType, name: '系统公告', icon: Megaphone },
    { id: 'logs' as TabType, name: '操作日志', icon: ScrollText },
  ];

  return (
    <div className="flex min-h-[calc(100vh-4rem)]">
      {/* 侧边栏 */}
      <aside className="w-64 border-r bg-muted/30">
        <div className="p-6">
          <h2 className="text-lg font-bold mb-4">管理后台</h2>
          <nav className="space-y-2">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <Button
                  key={tab.id}
                  variant={activeTab === tab.id ? 'default' : 'ghost'}
                  className="w-full justify-start"
                  onClick={() => setActiveTab(tab.id)}
                >
                  <Icon className="w-4 h-4 mr-2" />
                  {tab.name}
                </Button>
              );
            })}
          </nav>
        </div>
      </aside>

      {/* 主内容区 */}
      <main className="flex-1 p-8">
        {activeTab === 'dashboard' && <DashboardView />}
        {activeTab === 'users' && <UserManagement />}
        {activeTab === 'records' && <GameRecordManagement />}
        {activeTab === 'monsters' && <MonsterConfigManagement />}
        {activeTab === 'trend' && <AdaptLogTrend />}
        {activeTab === 'systemParams' && <SystemParamsManagement />}
        {activeTab === 'advanced' && <AdvancedConfigManagement />}
        {activeTab === 'systemDicts' && <SystemDictionaryManagement />}
        {activeTab === 'leaderboard' && <LeaderboardManagement />}
        {activeTab === 'charStats' && <CharacterStatsOverview />}
        {activeTab === 'notifications' && <NotificationManagement />}
        {activeTab === 'logs' && <OperationLogView />}
      </main>
    </div>
  );
}

function StatCard({ title, value, sub }: { title: string; value: number | undefined; sub?: string }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="text-3xl font-bold">{value ?? '—'}</div>
        {sub && <p className="text-xs text-muted-foreground mt-1">{sub}</p>}
      </CardContent>
    </Card>
  );
}

function DashboardView() {
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      setOverview(await adminGetOverview());
    } catch (e) {
      toast({ title: '加载概览失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  }

  const users = overview?.users;
  const games = overview?.games;
  const chat = overview?.chat;
  const unavailable = overview
    ? [!users && '用户服务', !games && '游戏服务', !chat && '聊天服务'].filter(Boolean)
    : [];

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-3xl font-bold">数据概览</h1>
          <p className="text-muted-foreground mt-2">系统运行状态和关键指标（"今日"按服务器时区计算）</p>
        </div>
        <Button variant="outline" onClick={load} disabled={loading}>
          {loading ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <RefreshCw className="w-4 h-4 mr-2" />}
          刷新
        </Button>
      </div>

      {unavailable.length > 0 && (
        <p className="text-sm text-destructive">以下服务暂不可用，对应数据未显示：{unavailable.join('、')}</p>
      )}

      <div className="grid gap-4 md:grid-cols-4">
        <StatCard title="用户总数" value={users?.userCount} sub={users ? `今日新增 ${users.newUsersToday}` : undefined} />
        <StatCard title="今日活跃用户" value={users?.activeUsersToday} sub="今日有登录记录" />
        <StatCard title="已禁用账号" value={users?.disabledUsers} />
        <StatCard title="今日游戏人数" value={games?.playersToday} />
        <StatCard title="游戏记录总数" value={games?.recordCount} sub={games ? `今日 ${games.recordsToday} 局` : undefined} />
        <StatCard title="聊天消息总数" value={chat?.messageCount} sub={chat ? `今日 ${chat.messagesToday} 条` : undefined} />
        <StatCard
          title="词库数量"
          value={chat ? chat.systemDictionaryCount + chat.customDictionaryCount : undefined}
          sub={chat ? `系统 ${chat.systemDictionaryCount} / 自定义 ${chat.customDictionaryCount}` : undefined}
        />
        <StatCard title="已发送通知" value={chat?.notificationCount} />
      </div>
    </div>
  );
}
