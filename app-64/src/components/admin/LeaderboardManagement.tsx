// 排行榜条目管理
import { useEffect, useState } from 'react';
import { adminGetLeaderboardEntries } from '@/db/api';
import type { LeaderboardEntry } from '@/types/database';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Search } from 'lucide-react';

export default function LeaderboardManagement() {
  const [items, setItems] = useState<LeaderboardEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [userId, setUserId] = useState('');
  const [gameMode, setGameMode] = useState<string>('all');
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    loadData();
  }, [page, userId, gameMode]);

  async function loadData() {
    setLoading(true);
    try {
      const uid = userId.trim();
      const uidNum = uid ? Number(uid) : undefined;
      const res = await adminGetLeaderboardEntries({
        page,
        pageSize,
        userId: uidNum && !Number.isNaN(uidNum) ? uidNum : undefined,
        gameMode: gameMode === 'all' ? undefined : gameMode,
      });
      setItems(res.items || []);
      setTotal(res.total || 0);
    } catch (e) {
      toast({
        title: '加载排行榜失败',
        description: e instanceof Error ? e.message : '未知错误',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  function handleSearch() {
    setPage(1);
    loadData();
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>排行榜条目管理</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2">
          <Input
            className="max-w-xs"
            placeholder="按用户 ID 搜索..."
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
          <Select
            value={gameMode}
            onValueChange={(v) => {
              setGameMode(v);
              setPage(1);
            }}
          >
            <SelectTrigger className="w-40">
              <SelectValue placeholder="全部模式" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部模式</SelectItem>
              <SelectItem value="ENDLESS_ADAPT">无尽适应</SelectItem>
              <SelectItem value="WORD_SOURCE">词源界定</SelectItem>
              <SelectItem value="LIMIT_TIME">限时冲刺</SelectItem>
            </SelectContent>
          </Select>
          <Button onClick={handleSearch}>
            <Search className="w-4 h-4 mr-1" />
            搜索
          </Button>
        </div>

        {loading ? (
          <div className="flex justify-center py-8">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        ) : (
          <>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>用户</TableHead>
                    <TableHead>模式</TableHead>
                    <TableHead>分数</TableHead>
                    <TableHead>轮数</TableHead>
                    <TableHead>平均速度</TableHead>
                    <TableHead>平均准确率</TableHead>
                    <TableHead>名次</TableHead>
                    <TableHead>时间</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((e) => (
                    <TableRow key={e.entryId ?? `${e.userId}-${e.recordId}`}>
                      <TableCell>
                        <div className="flex flex-col">
                          <span className="font-medium">{e.nickname || e.username}</span>
                          <span className="text-xs text-muted-foreground">ID: {e.userId}</span>
                        </div>
                      </TableCell>
                      <TableCell>{e.gameMode}</TableCell>
                      <TableCell className="font-medium">{e.totalScore}</TableCell>
                      <TableCell>{e.roundNum}</TableCell>
                      <TableCell>{e.avgSpeed.toFixed(1)}</TableCell>
                      <TableCell>{e.avgAccuracy.toFixed(1)}%</TableCell>
                      <TableCell>{e.rank}</TableCell>
                      <TableCell>
                        {e.createTime ? new Date(e.createTime).toLocaleString() : '-'}
                      </TableCell>
                    </TableRow>
                  ))}
                  {items.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={8} className="text-center text-muted-foreground py-6">
                        暂无数据
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between">
              <div className="text-sm text-muted-foreground">
                共 {total} 条记录，第 {page} / {totalPages} 页
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 1}
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                >
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === totalPages}
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                >
                  下一页
                </Button>
              </div>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}

