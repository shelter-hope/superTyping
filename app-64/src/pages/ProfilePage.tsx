// 个人信息页面
import { formatNum } from '@/utils';
import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { getUserGameRecords, getMostErrorCharacters, getUserStats, getErrorComparison, getAiTrainingSummary, type UserStats } from '@/db/api';
import type { AiTrainingSummary } from '@/db/api';
import type { GameRecord, CharacterStat } from '@/types/database';
import { Loader2, Copy, Sparkles, MessageCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import UserAvatar from '@/components/profile/UserAvatar';
import EditProfileDialog from '@/components/profile/EditProfileDialog';
import { getDisplayName } from '@/lib/avatars';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';

export default function ProfilePage() {
  const { user, profile } = useAuth();
  const [records, setRecords] = useState<GameRecord[]>([]);
  const [charStats, setCharStats] = useState<CharacterStat[]>([]);
  const [userStats, setUserStats] = useState<UserStats | null>(null);
  const [errorComparison, setErrorComparison] = useState<{ thisWeekErrors: number; lastWeekErrors: number; change: number; changeRate?: number } | null>(null);
  const [loading, setLoading] = useState(true);
  const [aiSummary, setAiSummary] = useState<AiTrainingSummary | null>(null);
  const [aiSummaryLoading, setAiSummaryLoading] = useState(false);
  const { toast } = useToast();

  const displayProfile = profile || user;

  async function loadAiSummary() {
    setAiSummaryLoading(true);
    try {
      const data = await getAiTrainingSummary();
      setAiSummary(data);
      toast({ title: '已获取训练摘要', description: '可复制下方内容到 AI 助手系统提示中使用。' });
    } catch (e) {
      console.error('获取训练摘要失败:', e);
      toast({ title: '获取失败', description: e instanceof Error ? e.message : '请稍后重试', variant: 'destructive' });
    } finally {
      setAiSummaryLoading(false);
    }
  }

  function copySummaryToClipboard() {
    if (!aiSummary?.contextForPrompt) return;
    navigator.clipboard.writeText(aiSummary.contextForPrompt).then(
      () => toast({ title: '已复制', description: '训练摘要已复制到剪贴板。' }),
      () => toast({ title: '复制失败', variant: 'destructive' })
    );
  }

  useEffect(() => {
    if (user) {
      loadData();
    }
  }, [user?.userId]);

  async function loadData() {
    if (!user) return;
    try {
        // 分别处理每个请求
    const recordsData = await getUserGameRecords(user.userId, 10);
    setRecords(recordsData);
    
    // 按错误数取 top 20，保证「错误最多」且含大小写（后端 /weak 按 error_count DESC）
    const statsData = await getMostErrorCharacters(user.userId, 20);
    setCharStats(statsData);
    
    const stats = await getUserStats(user.userId);
    setUserStats(stats);

    const errComp = await getErrorComparison(user.userId);
    if (errComp) {
      setErrorComparison({
        thisWeekErrors: errComp.thisWeekErrors ?? 0,
        lastWeekErrors: errComp.lastWeekErrors ?? 0,
        change: errComp.change ?? 0,
        changeRate: errComp.changeRate,
      });
    }
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  // 确保至少有基本用户信息
  if (!displayProfile) {
    return (
      <div className="container max-w-6xl py-8 text-center text-muted-foreground">
        无法获取用户信息
      </div>
    );
  }

  return (
    <div className="container max-w-6xl py-8">
      <div className="space-y-6">
        {/* 个人信息卡片 */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-6">
              <UserAvatar profile={displayProfile} size="xl" />
              <div className="flex-1">
                <h1 className="text-3xl font-bold">{getDisplayName(displayProfile)}</h1>
                <p className="text-muted-foreground">@{displayProfile.username}</p>
                {displayProfile.role === 'admin' && (
                  <span className="inline-block mt-2 px-2 py-1 text-xs bg-primary text-primary-foreground rounded">
                    管理员
                  </span>
                )}
              </div>
              <EditProfileDialog />
            </div>
          </CardContent>
        </Card>

        {/* 统计卡片 */}
        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>游戏场次</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold text-primary">{userStats?.totalGames || 0}</div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>最高分数</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold text-secondary">{userStats?.bestScore || 0}</div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>平均准确率</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold text-accent">{(userStats?.averageAccuracy || 0).toFixed(1)}%</div>
            </CardContent>
          </Card>
        </div>

        {/* 近期进步情况 */}
        {errorComparison != null && (
          <Card>
            <CardHeader>
              <CardTitle>近期进步情况</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground">本周错误次数</p>
                  <p className="text-2xl font-bold">{errorComparison.thisWeekErrors}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">上周错误次数</p>
                  <p className="text-2xl font-bold">{errorComparison.lastWeekErrors}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">变化</p>
                  <p className={`text-2xl font-bold ${errorComparison.change < 0 ? 'text-green-600' : errorComparison.change > 0 ? 'text-orange-600' : 'text-muted-foreground'}`}>
                    {errorComparison.change > 0 ? '+' : ''}{errorComparison.change}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">变化率</p>
                  <p className="text-2xl font-bold text-muted-foreground">
                    {errorComparison.changeRate != null ? `${(errorComparison.changeRate * 100).toFixed(0)}%` : '-'}
                  </p>
                </div>
              </div>
              <p className="text-sm text-muted-foreground mt-2">
                {errorComparison.change < 0 ? '相比上周错误减少，继续加油。' : errorComparison.change > 0 ? '相比上周错误增多，可到「趋势分析」查看训练建议。' : '与上周持平。'}
              </p>
            </CardContent>
          </Card>
        )}

        {/* 训练数据摘要（供 AI 助手使用） */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Sparkles className="w-5 h-5" />
              训练数据摘要（供 AI 助手使用）
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm text-muted-foreground">
              获取你的打字训练摘要，可复制到 ChatGPT、Claude 等 AI 助手的系统提示中；或直接使用站内 AI 对话（方案 A）。
            </p>
            <div className="flex flex-wrap gap-2">
              <Link to="/ai-chat">
                <Button type="button" variant="default" size="sm">
                  <MessageCircle className="w-4 h-4" />
                  <span className="ml-2">打开 AI 对话</span>
                </Button>
              </Link>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={loadAiSummary}
                disabled={aiSummaryLoading}
              >
                {aiSummaryLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                <span className="ml-2">{aiSummaryLoading ? '获取中…' : '获取摘要'}</span>
              </Button>
              {aiSummary != null && (
                <Button type="button" variant="outline" size="sm" onClick={copySummaryToClipboard}>
                  <Copy className="w-4 h-4" />
                  <span className="ml-2">复制到剪贴板</span>
                </Button>
              )}
            </div>
            {aiSummary != null && (
              <div className="rounded-md border bg-muted/30 p-3">
                <pre className="text-xs whitespace-pre-wrap break-words font-sans max-h-48 overflow-y-auto">
                  {aiSummary.contextForPrompt}
                </pre>
                {aiSummary.focusChars?.length ? (
                  <p className="text-xs text-muted-foreground mt-2">
                    建议重点字符：{aiSummary.focusChars.slice(0, 15).join('、')}
                    {aiSummary.focusChars.length > 15 ? '…' : ''}
                  </p>
                ) : null}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>最近游戏记录</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {records.map(record => (
                <div key={record.recordId} className="flex justify-between items-center p-3 border rounded-lg">
                  <div>
                    <div className="font-medium">积分: {record.totalScore}</div>
                    <div className="text-sm text-muted-foreground">
                      轮数: {record.roundNum} | 准确率: {formatNum(record.avgAccuracy)}%
                    </div>
                  </div>
                  <div className="text-sm text-muted-foreground">
                    {record.createTime ? new Date(record.createTime).toLocaleDateString() : '-'}
                  </div>
                </div>
              ))}
              {records.length === 0 && (
                <div className="text-center text-muted-foreground py-8">
                  还没有游戏记录
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>字符统计（错误最多）</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-4 md:grid-cols-8 gap-2">
              {charStats.map(stat => (
                <div key={stat.statId} className="p-2 border rounded text-center">
                  <div className="text-2xl font-bold">{stat.charValue}</div>
                  <div className="text-xs text-muted-foreground">
                    错误: {stat.errorCount}
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {(stat.averageAccuracy ?? 0).toFixed(0)}%
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
