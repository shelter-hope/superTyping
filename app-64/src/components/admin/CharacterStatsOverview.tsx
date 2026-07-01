// 字符统计总览
import { useEffect, useState } from 'react';
import { adminGetCharacterStats } from '@/db/api';
import type { CharacterStat } from '@/types/database';
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
import { useToast } from '@/hooks/use-toast';
import { Loader2, Search } from 'lucide-react';

export default function CharacterStatsOverview() {
  const [items, setItems] = useState<CharacterStat[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [userId, setUserId] = useState('');
  const [charType, setCharType] = useState('');
  const [charValue, setCharValue] = useState('');
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    loadData();
  }, [page, userId, charType, charValue]);

  async function loadData() {
    setLoading(true);
    try {
      const uid = userId.trim();
      const uidNum = uid ? Number(uid) : undefined;
      const res = await adminGetCharacterStats({
        page,
        pageSize,
        userId: uidNum && !Number.isNaN(uidNum) ? uidNum : undefined,
        charType: charType || undefined,
        charValue: charValue || undefined,
      });
      setItems(res.items || []);
      setTotal(res.total || 0);
    } catch (e) {
      toast({
        title: '加载字符统计失败',
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
        <CardTitle>字符统计总览</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2">
          <Input
            className="max-w-[120px]"
            placeholder="用户 ID"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
          <Input
            className="max-w-[140px]"
            placeholder="字符类型，如 lower"
            value={charType}
            onChange={(e) => setCharType(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
          <Input
            className="max-w-[80px]"
            placeholder="字符"
            value={charValue}
            onChange={(e) => setCharValue(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
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
                    <TableHead>用户 ID</TableHead>
                    <TableHead>字符</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>总尝试</TableHead>
                    <TableHead>正确</TableHead>
                    <TableHead>错误</TableHead>
                    <TableHead>平均准确率</TableHead>
                    <TableHead>最近准确率</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((s) => (
                    <TableRow key={s.statId}>
                      <TableCell className="font-mono text-xs">{s.userId}</TableCell>
                      <TableCell className="font-mono text-xs">{s.charValue}</TableCell>
                      <TableCell>{s.charType}</TableCell>
                      <TableCell>{s.totalAttempts}</TableCell>
                      <TableCell>{s.correctAttempts}</TableCell>
                      <TableCell className="text-red-600">{s.errorCount}</TableCell>
                      <TableCell>
                        {s.averageAccuracy != null ? `${s.averageAccuracy.toFixed(2)}%` : '-'}
                      </TableCell>
                      <TableCell>
                        {s.recentAccuracy != null ? `${s.recentAccuracy.toFixed(2)}%` : '-'}
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

