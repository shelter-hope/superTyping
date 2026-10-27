// 管理员页面
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
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
  FlaskConical,
  Microscope,
} from 'lucide-react';
import GameRuleManagement from '@/components/admin/GameRuleManagement';
import UserManagement from '@/components/admin/UserManagement';
import GameRecordManagement from '@/components/admin/GameRecordManagement';
import MonsterConfigManagement from '@/components/admin/MonsterConfigManagement';
import AdaptLogTrend from '@/components/admin/AdaptLogTrend';
import AdaptLogHistory from '@/components/admin/AdaptLogHistory';
import SystemParamsManagement from '@/components/admin/SystemParamsManagement';
import SystemDictionaryManagement from '@/components/admin/SystemDictionaryManagement';
import LeaderboardManagement from '@/components/admin/LeaderboardManagement';
import CharacterStatsOverview from '@/components/admin/CharacterStatsOverview';
import NotificationManagement from '@/components/admin/NotificationManagement';
import OperationLogView from '@/components/admin/OperationLogView';
import AdvancedConfigManagement from '@/components/admin/AdvancedConfigManagement';
import LabManagement from '@/components/admin/LabManagement';
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
  | 'logs'
  | 'rules'
  | 'lab';

const TAB_IDS: TabType[] = [
  'dashboard',
  'users',
  'records',
  'monsters',
  'trend',
  'systemParams',
  'systemDicts',
  'leaderboard',
  'charStats',
  'notifications',
  'advanced',
  'logs',
  'rules',
  'lab',
];

export default function AdminPage() {
  const [params, setParams] = useSearchParams();
  const requested = params.get('tab') as TabType | null;
  const activeTab: TabType = requested && TAB_IDS.includes(requested) ? requested : 'dashboard';
  function setActiveTab(tab: TabType) {
    setParams({ tab });
  }

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
    { id: 'rules' as TabType, name: '规则引擎', icon: FlaskConical },
    { id: 'lab' as TabType, name: '实验室管理', icon: Microscope },
  ];

  return (
    <div className="flex min-h-[calc(100vh-4rem)] flex-col md:flex-row">
      <aside className="border-b border-border md:w-60 md:shrink-0 md:border-b-0 md:border-r">
        <div className="px-4 py-5 md:px-6">
          <div className="flex items-center gap-3">
            <span className="status-light mt-0.5" />
            <span className="label-mono text-muted-foreground">后台 · ADMIN</span>
          </div>
          <h2 className="mt-3 font-serif text-xl leading-none">管理台</h2>

          <nav className="mt-5 flex gap-1 overflow-x-auto border-t border-border pt-2 md:block md:gap-0 md:overflow-visible">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const active = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex shrink-0 items-center gap-2 border-b border-transparent px-2 py-2 text-left text-[13px] transition-colors md:w-full md:justify-between md:px-0 ${
                    active
                      ? 'text-foreground'
                      : 'text-muted-foreground hover:text-foreground'
                  }`}
                >
                  <span className="flex items-center gap-2">
                    <Icon className="h-3.5 w-3.5" strokeWidth={1.6} />
                    {tab.name}
                  </span>
                  {active ? <span className="text-[9px] text-accent">●</span> : null}
                </button>
              );
            })}
          </nav>
        </div>
      </aside>

      <main className="min-w-0 flex-1 px-4 py-8 md:px-10">
        {activeTab === 'dashboard' && <DashboardView />}
        {activeTab === 'users' && <UserManagement />}
        {activeTab === 'records' && <GameRecordManagement />}
        {activeTab === 'monsters' && <MonsterConfigManagement />}
        {activeTab === 'trend' && (
          <div className="space-y-6">
            <AdaptLogTrend />
            <AdaptLogHistory />
          </div>
        )}
        {activeTab === 'systemParams' && <SystemParamsManagement />}
        {activeTab === 'advanced' && <AdvancedConfigManagement />}
        {activeTab === 'systemDicts' && <SystemDictionaryManagement />}
        {activeTab === 'leaderboard' && <LeaderboardManagement />}
        {activeTab === 'charStats' && <CharacterStatsOverview />}
        {activeTab === 'notifications' && <NotificationManagement />}
        {activeTab === 'logs' && <OperationLogView />}
        {activeTab === 'rules' && <GameRuleManagement />}
        {activeTab === 'lab' && <LabManagement />}
      </main>
    </div>
  );
}

function StatCell({ title, value, sub }: { title: string; value: number | undefined; sub?: string }) {
  return (
    <div className="border-b border-r border-border px-5 py-5">
      <div className="label-mono text-muted-foreground">{title}</div>
      <div className="mono-num mt-3 text-[32px] leading-none">{value ?? '—'}</div>
      {sub ? <p className="mono-num mt-2 text-[11px] text-muted-foreground">{sub}</p> : null}
    </div>
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
  const dictionaries = overview?.dictionaries;
  const unavailable = overview
    ? [!users && '用户服务', !games && '游戏服务', !chat && '聊天服务', !dictionaries && '词库服务'].filter(Boolean)
    : [];

  return (
    <div>
      <div className="flex flex-wrap items-end justify-between gap-4 border-b border-border pb-4">
        <div>
          <span className="label-mono text-muted-foreground">总览 · OVERVIEW</span>
          <h1 className="mt-2 font-serif text-[28px] leading-none">数据概览</h1>
          <p className="mt-2 text-[13px] text-muted-foreground">系统运行状态与关键指标（「今日」按服务器时区计算）</p>
        </div>
        <Button variant="outline" size="sm" onClick={load} disabled={loading}>
          {loading ? '刷新中…' : '刷新'}
        </Button>
      </div>

      {unavailable.length > 0 ? (
        <p className="mt-4 text-[13px] text-accent">
          以下服务暂不可用，对应数据未显示：{unavailable.join('、')}
        </p>
      ) : null}

      <div className="mt-8 grid border-l border-t border-border sm:grid-cols-2 xl:grid-cols-4">
        <StatCell title="用户总数" value={users?.userCount} sub={users ? `今日新增 ${users.newUsersToday}` : undefined} />
        <StatCell title="今日活跃" value={users?.activeUsersToday} sub="今日有登录记录" />
        <StatCell title="已禁用账号" value={users?.disabledUsers} />
        <StatCell title="今日游戏人数" value={games?.playersToday} />
        <StatCell title="游戏记录总数" value={games?.recordCount} sub={games ? `今日 ${games.recordsToday} 局` : undefined} />
        <StatCell title="聊天消息总数" value={chat?.messageCount} sub={chat ? `今日 ${chat.messagesToday} 条` : undefined} />
        <StatCell
          title="词库数量"
          value={dictionaries ? dictionaries.systemDictionaryCount + dictionaries.customDictionaryCount : undefined}
          sub={dictionaries ? `系统 ${dictionaries.systemDictionaryCount} / 自定义 ${dictionaries.customDictionaryCount}` : undefined}
        />
        <StatCell title="已发送通知" value={chat?.notificationCount} />
      </div>
    </div>
  );
}
