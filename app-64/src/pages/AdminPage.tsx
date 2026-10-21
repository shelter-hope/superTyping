// 管理员页面
import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Users, FileText, Bug, LayoutDashboard, TrendingUp, SlidersHorizontal, BookOpen, BarChart3, FlaskConical } from 'lucide-react';
import UserManagement from '@/components/admin/UserManagement';
import GameRecordManagement from '@/components/admin/GameRecordManagement';
import MonsterConfigManagement from '@/components/admin/MonsterConfigManagement';
import AdaptLogTrend from '@/components/admin/AdaptLogTrend';
import SystemParamsManagement from '@/components/admin/SystemParamsManagement';
import SystemDictionaryManagement from '@/components/admin/SystemDictionaryManagement';
import LeaderboardManagement from '@/components/admin/LeaderboardManagement';
import CharacterStatsOverview from '@/components/admin/CharacterStatsOverview';
import LabManagement from '@/components/admin/LabManagement';

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
  | 'lab';

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');

  const tabs = [
    { id: 'dashboard' as TabType, name: '数据概览', icon: LayoutDashboard },
    { id: 'users' as TabType, name: '用户管理', icon: Users },
    { id: 'records' as TabType, name: '游戏记录', icon: FileText },
    { id: 'monsters' as TabType, name: '怪物配置', icon: Bug },
    { id: 'trend' as TabType, name: 'Dₜ趋势', icon: TrendingUp },
    { id: 'systemParams' as TabType, name: '系统参数', icon: SlidersHorizontal },
    { id: 'systemDicts' as TabType, name: '系统词库', icon: BookOpen },
    { id: 'leaderboard' as TabType, name: '排行榜条目', icon: BarChart3 },
    { id: 'charStats' as TabType, name: '字符统计', icon: BarChart3 },
    { id: 'lab' as TabType, name: '实验室管理', icon: FlaskConical },
  ];

  return (
    <div className="flex min-h-[calc(100vh-4rem)] flex-col md:flex-row">
      <aside className="border-b bg-muted/30 md:w-64 md:border-b-0 md:border-r">
        <div className="p-4 md:p-6">
          <h2 className="mb-4 text-lg font-bold">管理后台</h2>
          <nav className="flex gap-2 overflow-x-auto md:block md:space-y-2">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <Button
                  key={tab.id}
                  variant={activeTab === tab.id ? 'default' : 'ghost'}
                  className="shrink-0 justify-start md:w-full"
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
      <main className="flex-1 p-4 md:p-8">
        {activeTab === 'dashboard' && <DashboardView />}
        {activeTab === 'users' && <UserManagement />}
        {activeTab === 'records' && <GameRecordManagement />}
        {activeTab === 'monsters' && <MonsterConfigManagement />}
        {activeTab === 'trend' && <AdaptLogTrend />}
        {activeTab === 'systemParams' && <SystemParamsManagement />}
        {activeTab === 'systemDicts' && <SystemDictionaryManagement />}
        {activeTab === 'leaderboard' && <LeaderboardManagement />}
        {activeTab === 'charStats' && <CharacterStatsOverview />}
        {activeTab === 'lab' && <LabManagement />}
      </main>
    </div>
  );
}

function DashboardView() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">数据概览</h1>
        <p className="text-muted-foreground mt-2">
          系统运行状态和关键指标
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="w-5 h-5" />
              用户管理
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              管理系统用户，包括查看、编辑、删除用户信息，以及设置管理员权限
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileText className="w-5 h-5" />
              游戏记录
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              查看和管理所有游戏记录，支持搜索、筛选、编辑和删除操作
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bug className="w-5 h-5" />
              怪物配置
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              调整不同类型怪物的积分值，影响游戏难度和得分计算
            </p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>功能说明</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <h3 className="font-medium mb-2">👥 用户管理</h3>
            <ul className="text-sm text-muted-foreground space-y-1 ml-4">
              <li>• 分页展示所有用户</li>
              <li>• 按用户名或昵称搜索</li>
              <li>• 编辑用户基本信息</li>
              <li>• 切换用户管理员身份</li>
              <li>• 清空用户的游戏数据</li>
            </ul>
          </div>

          <div>
            <h3 className="font-medium mb-2">📊 游戏记录管理</h3>
            <ul className="text-sm text-muted-foreground space-y-1 ml-4">
              <li>• 分页展示游戏记录</li>
              <li>• 按用户ID搜索</li>
              <li>• 按游戏模式筛选</li>
              <li>• 编辑记录数据</li>
              <li>• 删除游戏记录</li>
            </ul>
          </div>

          <div>
            <h3 className="font-medium mb-2">👾 怪物配置管理</h3>
            <ul className="text-sm text-muted-foreground space-y-1 ml-4">
              <li>• 查看所有怪物类型</li>
              <li>• 修改怪物积分值</li>
              <li>• 实时生效，影响新游戏</li>
            </ul>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
