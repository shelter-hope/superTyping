// 排行榜页面
import { formatNum } from '@/utils';
import { useEffect, useState, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { getLeaderboard, getUserRank, searchLeaderboard } from '@/db/api';
import { useAuth } from '@/contexts/AuthContext';
import type { LeaderboardEntry } from '@/types/database';
import { Trophy, Medal, Award, Loader2, Search, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

type BoardMode = 'endless' | 'dictionary' | 'limit';

function displayName(entry: LeaderboardEntry) {
  return entry.nickname || entry.username || `用户 ${entry.userId}`;
}

function entryKey(entry: LeaderboardEntry) {
  return String(entry.recordId ?? entry.entryId ?? entry.userId);
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
  const highlightedRef = useRef<HTMLDivElement>(null);
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

  function getRankIcon(rank: number) {
    if (rank === 1) return <Trophy className="w-6 h-6 text-yellow-500" />;
    if (rank === 2) return <Medal className="w-6 h-6 text-gray-400" />;
    if (rank === 3) return <Award className="w-6 h-6 text-amber-600" />;
    return <span className="w-6 text-center font-bold">{rank}</span>;
  }

  function renderLeaderboard(entries: LeaderboardEntry[]) {
    if (entries.length === 0) {
      return (
        <div className="text-center text-muted-foreground py-12">
          暂无排行数据
        </div>
      );
    }

    return (
      <div className="space-y-2">
        {entries.map((entry, index) => {
          const rank = entry.rank || index + 1;
          const isHighlighted = entryKey(entry) === highlightedId;
          return (
            <div
              key={entry.recordId ?? entry.entryId ?? index}
              ref={isHighlighted ? highlightedRef : null}
              className={`flex items-center gap-4 p-4 border rounded-lg transition-all duration-300 ${
                isHighlighted
                  ? 'bg-primary/10 border-primary shadow-lg scale-105'
                  : 'hover:bg-muted/50'
              }`}
            >
              <div className="flex-shrink-0">
                {getRankIcon(rank)}
              </div>
              <div className="flex-1 min-w-0">
                <div className={`font-medium truncate ${isHighlighted ? 'text-primary' : ''}`}>
                  {displayName(entry)}
                </div>
                <div className="text-sm text-muted-foreground">
                  轮数: {entry.roundNum} | 准确率: {formatNum(entry.avgAccuracy)}% | 
                  速度: {formatNum(entry.avgSpeed, 0)} 字符/分钟
                </div>
              </div>
              <div className={`text-2xl font-bold ${isHighlighted ? 'text-primary' : 'text-primary'}`}>
                {entry.totalScore}
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="container max-w-4xl py-8">
      <div className="space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-bold gradient-text">排行榜</h1>
          <p className="text-muted-foreground">查看全球玩家排名</p>
        </div>

        {/* 搜索框 */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Input
                  placeholder="输入玩家名进行搜索..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                />
                {searchTerm && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7 p-0"
                    onClick={handleClearSearch}
                  >
                    <X className="w-4 h-4" />
                  </Button>
                )}
              </div>
              <Button onClick={() => void handleSearch()} disabled={searching}>
                <Search className="w-4 h-4 mr-2" />
                {searching ? '搜索中…' : '搜索'}
              </Button>
            </div>
          </CardContent>
        </Card>

        {user && (
          <p className="text-center text-sm text-muted-foreground">
            {myRank ? `你在当前榜的排名：第 ${myRank} 名` : '你还没有进入当前榜'}
          </p>
        )}

        <Tabs defaultValue="endless" value={activeTab} onValueChange={(v) => {
          setActiveTab(v as BoardMode);
          setHighlightedId(null);
        }} className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="endless">无尽适应</TabsTrigger>
            <TabsTrigger value="dictionary">词源界定</TabsTrigger>
            <TabsTrigger value="limit">限时冲刺</TabsTrigger>
          </TabsList>
          {(['endless', 'dictionary', 'limit'] as BoardMode[]).map((mode) => (
            <TabsContent key={mode} value={mode} className="mt-6">
              <Card>
                <CardHeader>
                  <CardTitle>
                    {mode === 'endless' ? '无尽适应' : mode === 'dictionary' ? '词源界定' : '限时冲刺'} - 前50名
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {renderLeaderboard(boards[mode])}
                </CardContent>
              </Card>
            </TabsContent>
          ))}
        </Tabs>
      </div>
    </div>
  );
}
