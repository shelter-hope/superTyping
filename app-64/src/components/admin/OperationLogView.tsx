// 后台操作日志：admin-svc 对所有写操作（POST/PATCH/PUT/DELETE）自动记录
import { Fragment, useEffect, useState } from 'react';
import { adminGetOperationLogs, type AdminOperationLogEntry } from '@/db/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
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
import { Loader2, Search } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function OperationLogView() {
  const [items, setItems] = useState<AdminOperationLogEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [operatorId, setOperatorId] = useState('');
  const [path, setPath] = useState('');
  const [method, setMethod] = useState('all');
  const [result, setResult] = useState('all');
  const [loading, setLoading] = useState(false);
  const [expanded, setExpanded] = useState<number | null>(null);
  const { toast } = useToast();

  useEffect(() => {
    loadData();
  }, [page, method, result]);

  async function loadData() {
    setLoading(true);
    try {
      const uid = operatorId.trim();
      const res = await adminGetOperationLogs({
        page,
        pageSize,
        operatorId: uid ? Number(uid) : undefined,
        path: path.trim() || undefined,
        method: method === 'all' ? undefined : method,
        success: result === 'all' ? undefined : result === 'ok',
      });
      setItems(res.items || []);
      setTotal(res.total || 0);
    } catch (e) {
      toast({ title: '加载操作日志失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  }

  function handleSearch() {
    setPage(1);
    loadData();
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <Card>
      <CardHeader>
        <CardTitle>操作日志</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2">
          <Input
            className="w-40"
            placeholder="操作人 ID"
            value={operatorId}
            onChange={(e) => setOperatorId(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
          <Input
            className="flex-1 min-w-[200px]"
            placeholder="接口路径包含..."
            value={path}
            onChange={(e) => setPath(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
          <Select value={method} onValueChange={(v) => { setPage(1); setMethod(v); }}>
            <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部方法</SelectItem>
              <SelectItem value="POST">POST</SelectItem>
              <SelectItem value="PATCH">PATCH</SelectItem>
              <SelectItem value="PUT">PUT</SelectItem>
              <SelectItem value="DELETE">DELETE</SelectItem>
            </SelectContent>
          </Select>
          <Select value={result} onValueChange={(v) => { setPage(1); setResult(v); }}>
            <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部结果</SelectItem>
              <SelectItem value="ok">成功</SelectItem>
              <SelectItem value="fail">失败</SelectItem>
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
                    <TableHead>时间</TableHead>
                    <TableHead>操作人</TableHead>
                    <TableHead>方法</TableHead>
                    <TableHead>路径</TableHead>
                    <TableHead>状态码</TableHead>
                    <TableHead>耗时</TableHead>
                    <TableHead>IP</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((log) => (
                    <Fragment key={log.id}>
                      <TableRow
                        className="cursor-pointer"
                        onClick={() => setExpanded(expanded === log.id ? null : log.id)}
                      >
                        <TableCell className="whitespace-nowrap">{new Date(log.createTime).toLocaleString()}</TableCell>
                        <TableCell className="font-mono text-xs">{log.operatorId ?? '-'}</TableCell>
                        <TableCell className="font-mono text-xs">{log.method}</TableCell>
                        <TableCell className="font-mono text-xs max-w-[320px] truncate">
                          {log.path}{log.queryString ? `?${log.queryString}` : ''}
                        </TableCell>
                        <TableCell className={log.success ? '' : 'text-destructive font-medium'}>
                          {log.responseStatus ?? '-'}
                        </TableCell>
                        <TableCell>{log.durationMs != null ? `${log.durationMs} ms` : '-'}</TableCell>
                        <TableCell className="font-mono text-xs">{log.ip ?? '-'}</TableCell>
                      </TableRow>
                      {expanded === log.id && (
                        <TableRow key={`${log.id}-body`}>
                          <TableCell colSpan={7}>
                            <pre className="text-xs whitespace-pre-wrap break-all bg-muted/40 rounded p-3">
                              {log.requestBody || '（无请求体）'}
                            </pre>
                          </TableCell>
                        </TableRow>
                      )}
                    </Fragment>
                  ))}
                  {items.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-muted-foreground py-6">
                        暂无操作日志
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between">
              <div className="text-sm text-muted-foreground">
                共 {total} 条，第 {page} / {totalPages} 页（点击行查看请求体）
              </div>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={page === 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
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
