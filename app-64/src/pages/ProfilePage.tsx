// 个人档案：纸与墨（档案页）
// 左栏身份块 → 右栏发丝线指标 / 近期进步 / 最近对局 / 易错字符 / 训练摘要
import { useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Copy, Loader2, MessageCircle, Sparkles } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import UserAvatar from '@/components/profile/UserAvatar';
import EditProfileDialog from '@/components/profile/EditProfileDialog';
import { getDisplayName } from '@/lib/avatars';
import { useToast } from '@/hooks/use-toast';
import { formatNum } from '@/utils';
import {
  getAiTrainingSummary,
  getErrorComparison,
  getMostErrorCharacters,
  getUserGameRecords,
  getUserStats,
  type UserStats,
} from '@/db/api';
import type { AiTrainingSummary } from '@/db/api';
import type { GameRecord, CharacterStat } from '@/types/database';

function dateLabel(value?: string | null) {
  return value ? new Date(value).toLocaleDateString() : '—';
}

/** 小节报头：衬线标题 + 右侧等宽注脚 */
function SectionHeading({ title, note }: { title: string; note: string }) {
  return (
    <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
      <h2 className="font-serif text-xl leading-none">{title}</h2>
      <span className="label-mono text-[10px]">{note}</span>
    </div>
  );
}

/** 发丝线指标格：label-mono 标签 + mono-num 数字 */
function StatCell({
  label,
  value,
  note,
  tone = 'text-foreground',
}: {
  label: string;
  value: ReactNode;
  note?: string;
  tone?: string;
}) {
  return (
    <div className="border-r border-b border-border px-4 py-4">
      <div className="label-mono text-[10px]">{label}</div>
      <div className={`mono-num mt-2 text-[26px] leading-none ${tone}`}>{value}</div>
      {note ? <div className="label-mono mt-2 text-[9px] opacity-60">{note}</div> : null}
    </div>
  );
}

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
      <PageBody>
        <PageHeader
          eyebrow="档案 · PROFILE"
          title="个人档案"
          description="逐键档案：对局成绩、易错字符与训练摘要都记在这里。"
        />
        <div className="mt-8 border-t border-border pt-6">
          <span className="label-mono text-[10px]">读取档案中 · loading</span>
          <div className="mt-5 space-y-3">
            {['w-2/3', 'w-1/2', 'w-5/6', 'w-3/5'].map((width) => (
              <div key={width} className={`h-5 animate-pulse bg-muted ${width}`} />
            ))}
          </div>
        </div>
      </PageBody>
    );
  }

  // 确保至少有基本用户信息
  if (!displayProfile) {
    return (
      <PageBody>
        <PageHeader eyebrow="档案 · PROFILE" title="个人档案" />
        <p className="mt-8 border-t border-border pt-6 text-[13px] text-muted-foreground">
          无法获取用户信息
        </p>
      </PageBody>
    );
  }

  const changeTone =
    errorComparison && errorComparison.change < 0
      ? 'text-success'
      : errorComparison && errorComparison.change > 0
        ? 'text-accent'
        : 'text-muted-foreground';

  const stats: { label: string; value: ReactNode; note: string }[] = [
    { label: '总对局', value: userStats?.totalGames || 0, note: 'games' },
    { label: '平均准确率', value: `${(userStats?.averageAccuracy || 0).toFixed(1)}%`, note: 'accuracy' },
    { label: '最高分数', value: userStats?.bestScore || 0, note: 'best score' },
    { label: '累计字符', value: userStats?.totalCharactersTyped || 0, note: 'characters' },
    { label: '累计词数', value: userStats?.totalWordsTyped || 0, note: 'words' },
    { label: '排名', value: userStats?.ranking ? `#${userStats.ranking}` : '—', note: 'ranking' },
  ];

  return (
    <PageBody>
      <PageHeader
        eyebrow="档案 · PROFILE"
        title="个人档案"
        description="逐键档案：对局成绩、易错字符与训练摘要都记在这里。"
        actions={<EditProfileDialog />}
      />

      <div className="mt-8 grid gap-10 md:grid-cols-12 md:gap-8">
        {/* ─── 左栏：身份 ─── */}
        <aside className="md:col-span-4">
          <div className="flex items-start gap-4">
            <div className="shrink-0 border border-foreground/20 p-1.5">
              <UserAvatar profile={displayProfile} size="xl" />
            </div>
            <div className="min-w-0 pt-0.5">
              <h2 className="font-serif text-[24px] leading-tight break-words">{getDisplayName(displayProfile)}</h2>
              <p className="mono-num mt-2 text-[11px] text-muted-foreground">@{displayProfile.username}</p>
              {displayProfile.role === 'admin' ? (
                <span className="label-mono mt-3 inline-block border border-accent/40 px-1.5 py-1 text-[9px] text-accent">
                  管理员 · admin
                </span>
              ) : (
                <span className="label-mono mt-3 inline-block border border-border px-1.5 py-1 text-[9px]">
                  训练者 · trainee
                </span>
              )}
            </div>
          </div>

          {displayProfile.signature ? (
            <p className="pull-quote mt-6 text-[12.5px] leading-[1.9] text-muted-foreground">
              {displayProfile.signature}
            </p>
          ) : null}

          <dl className="mt-7 divide-y divide-border border-y border-border">
            {[
              ['档案编号', `#${displayProfile.userId}`],
              ['注册时间', dateLabel(displayProfile.createTime)],
              ['最近登录', dateLabel(displayProfile.lastLoginTime)],
            ].map(([label, value]) => (
              <div key={label} className="flex items-baseline justify-between gap-4 py-3">
                <dt className="label-mono text-[10px]">{label}</dt>
                <dd className="mono-num text-[12px]">{value}</dd>
              </div>
            ))}
          </dl>
        </aside>

        {/* ─── 右栏：数据 ─── */}
        <div className="md:col-span-8">
          <section>
            <SectionHeading title="档案数据" note="stats · 全部对局" />
            <div className="mt-4 grid grid-cols-2 border-t border-l border-border sm:grid-cols-3">
              {stats.map((item) => (
                <StatCell key={item.label} label={item.label} value={item.value} note={item.note} />
              ))}
            </div>
          </section>

          {errorComparison != null && (
            <section className="mt-10 border-t border-border pt-6">
              <SectionHeading title="近期进步" note="week over week" />
              <div className="mt-4 grid grid-cols-2 border-t border-l border-border sm:grid-cols-4">
                <StatCell label="本周错误" value={errorComparison.thisWeekErrors} note="errors" />
                <StatCell label="上周错误" value={errorComparison.lastWeekErrors} note="errors" />
                <StatCell
                  label="变化"
                  value={`${errorComparison.change > 0 ? '+' : ''}${errorComparison.change}`}
                  note="delta"
                  tone={changeTone}
                />
                <StatCell
                  label="变化率"
                  value={errorComparison.changeRate != null ? `${(errorComparison.changeRate * 100).toFixed(0)}%` : '-'}
                  note="rate"
                  tone="text-muted-foreground"
                />
              </div>
              <p className="pull-quote mt-4 text-[12.5px] leading-[1.9] text-muted-foreground">
                {errorComparison.change < 0 ? '相比上周错误减少，继续加油。' : errorComparison.change > 0 ? '相比上周错误增多，可到「趋势分析」查看训练建议。' : '与上周持平。'}
              </p>
            </section>
          )}

          <section className="mt-10 border-t border-border pt-6">
            <SectionHeading title="最近对局" note={`records · 显示 ${records.length} 条`} />
            {records.length > 0 ? (
              <ol className="mt-4 border-t border-border">
                {records.map((record, index) => (
                  <li
                    key={record.recordId}
                    className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-1 border-b border-border py-3"
                  >
                    <div className="flex flex-wrap items-baseline gap-x-4 gap-y-1">
                      <span className="label-mono text-[9px]">{String(index + 1).padStart(2, '0')}</span>
                      <span className="mono-num text-[13px]">{record.totalScore} 分</span>
                      <span className="mono-num text-[11px] text-muted-foreground">轮 {record.roundNum}</span>
                      <span className="mono-num text-[11px] text-muted-foreground">
                        准确率 {formatNum(record.avgAccuracy)}%
                      </span>
                    </div>
                    <span className="mono-num text-[11px] text-muted-foreground">
                      {record.createTime ? new Date(record.createTime).toLocaleDateString() : '-'}
                    </span>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="mt-4 border-t border-border py-6 text-[13px] text-muted-foreground">
                还没有游戏记录
              </p>
            )}
          </section>

          <section className="mt-10 border-t border-border pt-6">
            <SectionHeading title="易错字符" note="errors · 按错误次数" />
            {charStats.length > 0 ? (
              <ol className="mt-4 grid border-t border-l border-border sm:grid-cols-2">
                {charStats.map((stat, index) => (
                  <li
                    key={stat.statId}
                    className="flex items-baseline justify-between gap-3 border-r border-b border-border px-4 py-3"
                  >
                    <div className="flex min-w-0 items-baseline gap-3">
                      <span className="label-mono text-[9px]">{String(index + 1).padStart(2, '0')}</span>
                      <span className="font-serif text-[19px] leading-none">
                        {stat.charValue === ' ' ? '空格' : stat.charValue}
                      </span>
                      <span className="mono-num text-[10px] text-muted-foreground">
                        准确率 {formatNum(stat.averageAccuracy, 0)}%
                      </span>
                    </div>
                    <span className="mono-num shrink-0 text-[12px] text-accent">{stat.errorCount}</span>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="mt-4 border-t border-border py-6 text-[13px] text-muted-foreground">
                还没有字符数据：打完一局，错过的字会记在这里。
              </p>
            )}
          </section>

          <section className="mt-10 border-t border-border pt-6">
            <SectionHeading title="训练摘要" note="ai context" />
            <p className="mt-3 max-w-xl text-[13px] leading-[1.95] text-muted-foreground">
              获取你的打字训练摘要，可复制到 ChatGPT、Claude 等 AI 助手的系统提示中；或直接使用站内 AI 对话。
            </p>

            <div className="mt-4 flex flex-wrap items-center gap-2">
              <Button size="sm" asChild>
                <Link to="/ai-assistant">
                  <MessageCircle />
                  打开 AI 对话
                </Link>
              </Button>
              <Button type="button" variant="outline" size="sm" onClick={loadAiSummary} disabled={aiSummaryLoading}>
                {aiSummaryLoading ? <Loader2 className="animate-spin" /> : <Sparkles />}
                {aiSummaryLoading ? '获取中…' : '获取摘要'}
              </Button>
              {aiSummary != null && (
                <Button type="button" variant="outline" size="sm" onClick={copySummaryToClipboard}>
                  <Copy />
                  复制到剪贴板
                </Button>
              )}
            </div>

            {aiSummary != null && (
              <div className="mt-4 border border-border">
                <div className="flex items-center justify-between gap-3 border-b border-border px-4 py-2.5">
                  <span className="label-mono text-[9px]">system prompt · 可复制</span>
                  <span className="mono-num text-[10px] text-muted-foreground">{aiSummary.suggestedMode}</span>
                </div>
                <pre className="max-h-48 overflow-y-auto px-4 py-3 font-sans text-[12px] leading-[1.9] whitespace-pre-wrap break-words">
                  {aiSummary.contextForPrompt}
                </pre>
                {aiSummary.focusChars?.length ? (
                  <p className="border-t border-border px-4 py-2.5 text-[12px] text-muted-foreground">
                    建议重点字符：{aiSummary.focusChars.slice(0, 15).join('、')}
                    {aiSummary.focusChars.length > 15 ? '…' : ''}
                  </p>
                ) : null}
              </div>
            )}
          </section>
        </div>
      </div>
    </PageBody>
  );
}
