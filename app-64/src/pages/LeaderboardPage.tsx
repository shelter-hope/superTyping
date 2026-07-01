// 排行榜页面
import { useEffect, useState, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { getLeaderboard } from '@/db/api';
import type { LeaderboardEntry } from '@/types/database';
import { Trophy, Medal, Award, Loader2, Search, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function LeaderboardPage() {
  const [endlessLeaderboard, setEndlessLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [dictionaryLeaderboard, setDictionaryLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [highlightedId, setHighlightedId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'endless' | 'dictionary'>('endless');
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

  async function loadLeaderboards() {
    try {
      const [endless, dictionary] = await Promise.all([
        getLeaderboard('endless', 50),
        getLeaderboard('dictionary', 50),
      ]);
      setEndlessLeaderboard(endless);
      setDictionaryLeaderboard(dictionary);
    } catch (error) {
      console.error('加载排行榜失败:', error);
    } finally {
      setLoading(false);
    }
  }

  function handleSearch() {
    if (!searchTerm.trim()) {
      toast({
        title: '请输入玩家名',
        description: '请输入要搜索的玩家用户名',
        variant: 'destructive',
      });
      return;
    }

    const currentLeaderboard = activeTab === 'endless' ? endlessLeaderboard : dictionaryLeaderboard;
    const foundIndex = currentLeaderboard.findIndex(
      entry => entry.username.toLowerCase().includes(searchTerm.toLowerCase())
    );
    const found = foundIndex >= 0 ? currentLeaderboard[foundIndex] : null;
    const rank = foundIndex >= 0 ? foundIndex + 1 : 0;

    if (found) {
      setHighlightedId(found.recordId?.toString() ?? '');
      toast({
        title: '找到玩家',
        description: `${found.username} 排名第 ${rank} 位，得分 ${found.totalScore}`,
      });
    } else {
      setHighlightedId(null);
      toast({
        title: '未找到玩家',
        description: `在当前模式的排行榜中未找到"${searchTerm}"`,
        variant: 'destructive',
      });
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
          const rank = index + 1;
          const isHighlighted = entry.recordId?.toString() === highlightedId;
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
                  {entry.username}
                </div>
                <div className="text-sm text-muted-foreground">
                  轮数: {entry.roundNum} | 准确率: {entry.avgAccuracy.toFixed(1)}% | 
                  速度: {entry.avgSpeed.toFixed(0)} 字符/分钟
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
              <Button onClick={handleSearch}>
                <Search className="w-4 h-4 mr-2" />
                搜索
              </Button>
            </div>
          </CardContent>
        </Card>

        <Tabs defaultValue="endless" value={activeTab} onValueChange={(v) => {
          setActiveTab(v as 'endless' | 'dictionary');
          setHighlightedId(null); // 切换标签时清除高亮
        }} className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="endless">无尽适应模式</TabsTrigger>
            <TabsTrigger value="dictionary">词源界定模式</TabsTrigger>
          </TabsList>
          <TabsContent value="endless" className="mt-6">
            <Card>
              <CardHeader>
                <CardTitle>无尽适应模式 - 前50名</CardTitle>
              </CardHeader>
              <CardContent>
                {renderLeaderboard(endlessLeaderboard)}
              </CardContent>
            </Card>
          </TabsContent>
          <TabsContent value="dictionary" className="mt-6">
            <Card>
              <CardHeader>
                <CardTitle>词源界定模式 - 前50名</CardTitle>
              </CardHeader>
              <CardContent>
                {renderLeaderboard(dictionaryLeaderboard)}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
