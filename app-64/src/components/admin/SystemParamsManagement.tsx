// 系统参数管理
import { useEffect, useState } from 'react';
import { adminGetSystemParams, type AdminPageResult } from '@/db/api';
import type { SystemParam } from '@/types/database';
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
import { Loader2, Search } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function SystemParamsManagement() {
  const [items, setItems] = useState<SystemParam[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [paramName, setParamName] = useState('');
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    loadData();
  }, [page, paramName]);

  async function loadData() {
    setLoading(true);
    try {
      const res: AdminPageResult<SystemParam> = await adminGetSystemParams({
        page,
        pageSize,
        paramName: paramName || undefined,
      });
      setItems(res.items || []);
      setTotal(res.total || 0);
    } catch (e) {
      toast({
        title: '加载系统参数失败',
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
        <CardTitle>系统参数管理</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex gap-2">
          <Input
            placeholder="按参数名搜索..."
            value={paramName}
            onChange={(e) => setParamName(e.target.value)}
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
                    <TableHead>名称</TableHead>
                    <TableHead>值</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>说明</TableHead>
                    <TableHead>系统参数</TableHead>
                    <TableHead>启用</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((p) => (
                    <TableRow key={p.paramId}>
                      <TableCell className="font-mono text-xs">{p.paramName}</TableCell>
                      <TableCell className="font-mono text-xs max-w-[240px] truncate">
                        {p.paramValue}
                      </TableCell>
                      <TableCell>{p.paramType || '-'}</TableCell>
                      <TableCell className="max-w-[260px] truncate">
                        {p.description || '-'}
                      </TableCell>
                      <TableCell>{p.isSystem ? '是' : '否'}</TableCell>
                      <TableCell>{p.isActive ? '是' : '否'}</TableCell>
                    </TableRow>
                  ))}
                  {items.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground py-6">
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

