import { useState } from 'react';
import { adminGetUserModelAdaptLogs, type UserModelAdaptLogDTO } from '@/db/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useToast } from '@/hooks/use-toast';
import { History, Loader2 } from 'lucide-react';

function formatDifficulty(value: number | null) {
  return value == null ? '—' : Number(value).toFixed(4);
}

function summarizeParams(raw: string | null) {
  if (!raw) return '—';
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    const accuracy = parsed.accuracy ?? parsed.avgAccuracy;
    const speed = parsed.speed ?? parsed.avgSpeed;
    const ruleVersion = parsed.ruleVersion;
    const monsterCount = parsed.monsterCount;
    return [
      accuracy != null ? `准确率 ${accuracy}` : null,
      speed != null ? `速度 ${speed}` : null,
      monsterCount != null ? `怪数 ${monsterCount}` : null,
      ruleVersion != null ? `规则 v${ruleVersion}` : null,
    ]
      .filter(Boolean)
      .join(' · ');
  } catch {
    return raw.slice(0, 80);
  }
}

export default function AdaptLogHistory() {
  const [userId, setUserId] = useState('');
  const [gameMode, setGameMode] = useState<string>('');
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().slice(0, 10);
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [logs, setLogs] = useState<UserModelAdaptLogDTO[]>([]);
  const [total, setTotal] = useState(0);
  const { toast } = useToast();
  const pageSize = 20;

  async function handleFetch(nextPage = 1) {
    const uid = userId.trim();
    const params: {
      page: number;
      pageSize: number;
      userId?: number;
      gameMode?: string;
      startDate?: string;
      endDate?: string;
    } = { page: nextPage, pageSize };
    if (uid) {
      const uidNum = Number(uid);
      if (Number.isNaN(uidNum) || uidNum < 1) {
        toast({ title: '用户 ID 须为正整数', variant: 'destructive' });
        return;
      }
      params.userId = uidNum;
    }
    if (gameMode) params.gameMode = gameMode;
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;

    setLoading(true);
    try {
      const result = await adminGetUserModelAdaptLogs(params);
      setLogs(result.logs);
      setTotal(result.total);
      setPage(nextPage);
      toast({ title: '加载成功', description: `共 ${result.total} 条难度变换记录` });
    } catch (error) {
      toast({
        title: '加载失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
      setLogs([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <History className="w-5 h-5" />
          难度变换明细
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          每轮结算与局末汇总的 before→after 记录，可按用户、模式、时间筛选。
        </p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
          <div className="space-y-2">
            <Label>用户 ID（可选）</Label>
            <Input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="留空查全部" />
          </div>
          <div className="space-y-2">
            <Label>游戏模式</Label>
            <Select value={gameMode || '__all__'} onValueChange={(v) => setGameMode(v === '__all__' ? '' : v)}>
              <SelectTrigger>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="__all__">全部</SelectItem>
                <SelectItem value="ENDLESS">无尽</SelectItem>
                <SelectItem value="TIMED">限时</SelectItem>
                <SelectItem value="CHALLENGE">挑战</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>开始日期</Label>
            <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>结束日期</Label>
            <Input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </div>
          <div className="flex items-end">
            <Button onClick={() => void handleFetch(1)} disabled={loading} className="w-full">
              {loading ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null}
              查询
            </Button>
          </div>
        </div>

        <div className="overflow-x-auto rounded border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>时间</TableHead>
                <TableHead>用户</TableHead>
                <TableHead>模式</TableHead>
                <TableHead>轮次</TableHead>
                <TableHead>前 → 后</TableHead>
                <TableHead>触发</TableHead>
                <TableHead>关键参数</TableHead>
                <TableHead>会话/记录</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {logs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center text-muted-foreground">
                    暂无数据，请先查询
                  </TableCell>
                </TableRow>
              ) : (
                logs.map((log) => (
                  <TableRow key={log.logId}>
                    <TableCell className="whitespace-nowrap text-xs">
                      {log.logTime ? new Date(log.logTime).toLocaleString() : '—'}
                    </TableCell>
                    <TableCell>{log.userId}</TableCell>
                    <TableCell>{log.gameMode ?? '—'}</TableCell>
                    <TableCell>{log.roundNum}</TableCell>
                    <TableCell className="font-mono text-xs">
                      {formatDifficulty(log.beforeDifficulty)} → {formatDifficulty(log.afterDifficulty)}
                    </TableCell>
                    <TableCell className="text-xs">{log.triggerRule ?? '—'}</TableCell>
                    <TableCell className="max-w-[240px] truncate text-xs" title={log.adjustParams ?? ''}>
                      {summarizeParams(log.adjustParams)}
                    </TableCell>
                    <TableCell className="text-xs">
                      {log.sessionId != null ? `S${log.sessionId}` : '—'}
                      {' / '}
                      {log.gameRecordId != null ? `R${log.gameRecordId}` : '—'}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            第 {page} / {totalPages} 页 · 共 {total} 条
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={loading || page <= 1} onClick={() => void handleFetch(page - 1)}>
              上一页
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={loading || page >= totalPages}
              onClick={() => void handleFetch(page + 1)}
            >
              下一页
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
