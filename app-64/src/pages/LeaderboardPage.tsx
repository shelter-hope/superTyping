// 排行榜页面
import { useEffect, useRef, useState } from 'react';
import { Search, X } from 'lucide-react';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';
import { getLeaderboard, getUserRank, searchLeaderboard } from '@/db/api';
import { useToast } from '@/hooks/use-toast';
import type { LeaderboardEntry } from '@/types/database';
import { formatNum } from '@/utils';

type BoardMode = 'endless' | 'dictionary' | 'limit';

const MODES: BoardMode[] = ['endless', 'dictionary', 'limit'];

const MODE_META: Record<BoardMode, { label: string; en: string }> = {
  endless: { label: '无尽适应', en: 'ENDLESS' },
  dictionary: { label: '词源界定', en: 'DICTIONARY' },
  limit: { label: '限时冲刺', en: 'TIMED' },
};

function displayName(entry: LeaderboardEntry) {
  return entry.nickname || entry.username || `用户 ${entry.userId}`;
}

function entryKey(entry: LeaderboardEntry) {
  return String(entry.recordId ?? entry.entryId ?? entry.userId);
}

/** 载入态：一个等宽小标签 + 一根扫光细线，不用转圈图标 */
function LoadingNote({ label }: { label: string }) {
  return (
    <div className="mt-10">
      <div className="flex items-center gap-3">
        <span className="label-mono text-[10px]">{label}</span>
        <span className="block h-[1px] w-24 overflow-hidden bg-border">
          <span className="block h-full w-1/3 animate-loading-sweep bg-foreground" />
        </span>
      </div>
      <div className="mt-8 border-t border-border">
        {[0, 1, 2].map((row) => (
          <div key={row} className="h-10 border-b border-border" />
        ))}
      </div>
    </div>
  );
}

export default function LeaderboardPage() {
  const { user } = useAuth();
  const [boards, setBoards] = useState<Record<BoardMode, LeaderboardEntry[]>>({
    endless: [],
    dictionary: [],
    limit: [],
  });
  const [myRank, setMyRank] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [highlightedId, setHighlightedId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<BoardMode>('endless');
  const highlightedRef = useRef<HTMLLIElement>(null);
  const { toast } = useToast();

  useEffect(() => {
    loadLeaderboards();
  }, []);

  useEffect(() => {
    // 当高亮的元素改变时，滚动到该元素
    if (highlightedRef.current) {
      highlightedRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [highlightedId]);

  useEffect(() => {
    if (!user) {
      setMyRank(null);
      return;
    }
    getUserRank(user.userId, activeTab)
      .then((rank) => setMyRank(rank))
      .catch(() => setMyRank(null));
  }, [user, activeTab]);

  async function loadLeaderboards() {
    try {
      const [endless, dictionary, limit] = await Promise.all([
        getLeaderboard('endless', 50),
        getLeaderboard('dictionary', 50),
        getLeaderboard('limit', 50),
      ]);
      setBoards({ endless, dictionary, limit });
    } catch (error) {
      console.error('加载排行榜失败:', error);
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch() {
    const keyword = searchTerm.trim();
    if (!keyword) {
      toast({
        title: '请输入玩家名',
        description: '请输入要搜索的玩家用户名',
        variant: 'destructive',
      });
      return;
    }

    setSearching(true);
    try {
      const found = await searchLeaderboard(keyword, activeTab);
      const match = found[0];
      if (!match) {
        setHighlightedId(null);
        toast({
          title: '未找到玩家',
          description: `在当前模式的排行榜中未找到“${keyword}”`,
          variant: 'destructive',
        });
        return;
      }
      setBoards((current) => {
        const list = current[activeTab];
        if (list.some((entry) => entry.userId === match.userId)) return current;
        return { ...current, [activeTab]: [match, ...list] };
      });
      setHighlightedId(entryKey(match));
      toast({
        title: '找到玩家',
        description: `${displayName(match)} 排名第 ${match.rank || '—'} 位，得分 ${match.totalScore}`,
      });
    } catch (error) {
      toast({
        title: '搜索失败',
        description: error instanceof Error ? error.message : '请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setSearching(false);
    }
  }

  function handleClearSearch() {
    setSearchTerm('');
    setHighlightedId(null);
  }

  function renderLeaderboard(entries: LeaderboardEntry[]) {
    if (entries.length === 0) {
      return (
        <p className="border-y border-border py-10 text-[13px] text-muted-foreground">暂无排行数据</p>
      );
    }

    return (
      <div>
        <div className="hidden grid-cols-[3rem_minmax(0,1fr)_4.5rem_6rem_7rem_6rem] items-center gap-4 py-2 md:grid">
          <span className="label-mono text-[9px]">名次</span>
          <span className="label-mono text-[9px]">玩家</span>
          <span className="label-mono text-[9px] text-right">轮数</span>
          <span className="label-mono text-[9px] text-right">准确率</span>
          <span className="label-mono text-[9px] text-right">速度 · CPM</span>
          <span className="label-mono text-[9px] text-right">得分</span>
        </div>

        <ol className="divide-y divide-border border-y border-border">
          {entries.map((entry, index) => {
            const rank = entry.rank || index + 1;
            const isTop = rank <= 3;
            const isHighlighted = entryKey(entry) === highlightedId;
            return (
              <li
                key={entry.recordId ?? entry.entryId ?? index}
                ref={isHighlighted ? highlightedRef : null}
                aria-current={isHighlighted ? 'true' : undefined}
                className={`grid grid-cols-[2.5rem_minmax(0,1fr)_auto] items-start gap-x-4 py-3 transition-colors duration-200 md:grid-cols-[3rem_minmax(0,1fr)_4.5rem_6rem_7rem_6rem] md:items-center ${
                  isHighlighted ? 'bg-accent/[0.07]' : 'hover:bg-foreground/[0.02]'
                }`}
              >
                <span
                  className={`mono-num pt-0.5 text-[12px] md:pt-0 ${isTop ? 'text-accent' : 'text-muted-foreground'}`}
                >
                  {String(rank).padStart(2, '0')}
                </span>

                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="truncate text-[13.5px] leading-5">{displayName(entry)}</span>
                    {isTop ? (
                      <span className="text-[8px] leading-none text-accent" aria-hidden>
                        ●
                      </span>
                    ) : null}
                  </div>
                  <div className="mono-num mt-1 flex flex-wrap items-baseline gap-x-3 gap-y-0.5 text-[11px] text-muted-foreground md:hidden">
                    <span>轮 {entry.roundNum}</span>
                    <span>准 {formatNum(entry.avgAccuracy)}%</span>
                    <span>速 {formatNum(entry.avgSpeed, 0)}</span>
                  </div>
                </div>

                <span className="mono-num hidden text-right text-[12px] text-muted-foreground md:block">
                  {entry.roundNum}
                </span>
                <span className="mono-num hidden text-right text-[12px] text-muted-foreground md:block">
                  {formatNum(entry.avgAccuracy)}%
                </span>
                <span className="mono-num hidden text-right text-[12px] text-muted-foreground md:block">
                  {formatNum(entry.avgSpeed, 0)}
                </span>
                <span
                  className={`mono-num text-right text-[15px] leading-5 ${isHighlighted ? 'text-accent' : 'text-foreground'}`}
                >
                  {entry.totalScore}
                </span>
              </li>
            );
          })}
        </ol>
      </div>
    );
  }

  return (
    <PageBody>
      <PageHeader
        eyebrow="竞技 · RANKING"
        title="排行榜"
        description="每种模式分别按总分排行，只展示前 50 名。搜索可以定位到榜单里的某一个玩家。"
      />

      {loading ? (
        <LoadingNote label="载入榜单 · LOADING" />
      ) : (
        <>
          <div className="mt-8 flex flex-wrap items-end justify-between gap-x-8 gap-y-4">
            <div className="w-full max-w-md">
              <label htmlFor="leaderboard-search" className="label-mono mb-2 block">
                查找玩家 · SEARCH
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Input
                    id="leaderboard-search"
                    placeholder="输入玩家名进行搜索..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                    className="pr-9"
                  />
                  {searchTerm && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="absolute right-1 top-1/2 h-7 w-7 -translate-y-1/2"
                      onClick={handleClearSearch}
                      aria-label="清空搜索"
                    >
                      <X className="h-3.5 w-3.5" />
                    </Button>
                  )}
                </div>
                <Button className="shrink-0" onClick={() => void handleSearch()} disabled={searching}>
                  <Search />
                  {searching ? '搜索中…' : '搜索'}
                </Button>
              </div>
            </div>

            {user ? (
              <dl className="flex items-baseline gap-3 pb-2">
                <dt className="label-mono text-[9px]">我的排名 · MY RANK</dt>
                <dd className="mono-num text-[13px]">
                  {myRank ? `第 ${myRank} 名` : '未进入当前榜'}
                </dd>
              </dl>
            ) : null}
          </div>

          <section className="mt-10 border-t border-border pt-6">
            <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 pb-3">
              <div className="flex flex-wrap items-baseline gap-x-3">
                <h2 className="font-serif text-xl leading-none">榜单</h2>
                <span className="label-mono text-[9px]">TOP 50</span>
              </div>
              <span className="label-mono text-[9px]">
                {MODE_META[activeTab].label} · {MODE_META[activeTab].en}
              </span>
            </div>

            <Tabs
              value={activeTab}
              onValueChange={(v) => {
                setActiveTab(v as BoardMode);
                setHighlightedId(null);
              }}
              className="gap-0"
            >
              <TabsList className="grid h-auto w-full grid-cols-3 gap-px rounded-none border border-border bg-border p-0">
                {MODES.map((mode) => (
                  <TabsTrigger
                    key={mode}
                    value={mode}
                    className="h-auto rounded-none border-0 bg-background px-2 py-2 text-center text-[13px] font-normal text-muted-foreground hover:text-foreground data-[state=active]:bg-foreground data-[state=active]:text-background data-[state=active]:shadow-none"
                  >
                    {MODE_META[mode].label}
                  </TabsTrigger>
                ))}
              </TabsList>

              {MODES.map((mode) => (
                <TabsContent key={mode} value={mode} className="mt-5">
                  {renderLeaderboard(boards[mode])}
                </TabsContent>
              ))}
            </Tabs>
          </section>
        </>
      )}
    </PageBody>
  );
}
