// Dₜ 趋势：按日聚合的用户模型适配日志，用于管理页
import { useState } from 'react';
import { adminGetAdaptLogsTrend, type AdaptLogTrendPoint } from '@/db/api';
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
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Loader2, TrendingUp } from 'lucide-react';

export default function AdaptLogTrend() {
  const [userId, setUserId] = useState('');
  const [gameMode, setGameMode] = useState<string>('');
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().slice(0, 10);
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AdaptLogTrendPoint[]>([]);
  const { toast } = useToast();

  async function handleFetch() {
    const uid = userId.trim();
    if (!uid) {
      toast({ title: '请输入用户 ID', variant: 'destructive' });
      return;
    }
    const uidNum = Number(uid);
    if (Number.isNaN(uidNum) || uidNum < 1) {
      toast({ title: '用户 ID 须为正整数', variant: 'destructive' });
      return;
    }
    if (!startDate || !endDate) {
      toast({ title: '请选择开始与结束日期', variant: 'destructive' });
      return;
    }
    setLoading(true);
    try {
      const list = await adminGetAdaptLogsTrend({
        userId: uidNum,
        gameMode: gameMode || undefined,
        startDate,
        endDate,
      });
      setData(list);
      toast({ title: '加载成功', description: `共 ${list.length} 个日期数据点` });
    } catch (error) {
      toast({
        title: '加载失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
      setData([]);
    } finally {
      setLoading(false);
    }
  }

  const chartData = data.map((d) => ({
    date: d.date,
    Dt: d.avgAfterDifficulty ?? 0,
    count: d.count,
    maxRound: d.maxRound,
  }));

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5" />
            Dₜ 趋势（按日聚合）
          </CardTitle>
          <p className="text-sm text-muted-foreground">
            查询指定用户、时间范围内的难度系数 Dₜ 按日聚合，用于观察自适应难度变化趋势。
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
            <div className="space-y-2">
              <Label>用户 ID</Label>
              <Input
                placeholder="例如 1"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>游戏模式</Label>
              <Select value={gameMode || 'all'} onValueChange={(v) => setGameMode(v === 'all' ? '' : v)}>
                <SelectTrigger>
                  <SelectValue placeholder="全部" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部</SelectItem>
                  <SelectItem value="ENDLESS_ADAPT">无尽适应</SelectItem>
                  <SelectItem value="WORD_SOURCE">词源界定</SelectItem>
                  <SelectItem value="LIMIT_TIME">限时冲刺</SelectItem>
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
            <div className="space-y-2 flex items-end">
              <Button onClick={handleFetch} disabled={loading}>
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : '查询'}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {chartData.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>趋势图（当日平均 Dₜ）</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis domain={[0, 1]} />
                  <Tooltip formatter={(value: number) => [value?.toFixed(2), 'Dₜ']} />
                  <Line type="monotone" dataKey="Dt" stroke="hsl(var(--primary))" strokeWidth={2} dot={{ r: 3 }} name="Dₜ" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      )}

      {data.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>数据表</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>日期</TableHead>
                  <TableHead>当日平均 Dₜ</TableHead>
                  <TableHead>条数</TableHead>
                  <TableHead>最大轮次</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((row) => (
                  <TableRow key={row.date}>
                    <TableCell>{row.date}</TableCell>
                    <TableCell>{row.avgAfterDifficulty != null ? row.avgAfterDifficulty.toFixed(2) : '-'}</TableCell>
                    <TableCell>{row.count}</TableCell>
                    <TableCell>{row.maxRound}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
