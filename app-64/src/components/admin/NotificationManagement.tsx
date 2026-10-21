// 系统公告 / 通知管理：发布公告（全员或指定用户），查看与删除已发送的通知
import { useEffect, useState } from 'react';
import { adminDeleteNotification, adminGetSystemNotifications, adminPublishNotification } from '@/db/api';
import type { SystemNotification } from '@/types/database';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Loader2, Search, Send, Trash2 } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

const TYPE_NAMES: Record<number, string> = {
  1: '好友申请',
  2: '系统公告',
  3: '游戏奖励',
  4: '任务提醒',
  5: '异常通知',
};

export default function NotificationManagement() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [targetUserId, setTargetUserId] = useState('');
  const [publishing, setPublishing] = useState(false);

  const [items, setItems] = useState<SystemNotification[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [filterUserId, setFilterUserId] = useState('');
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    loadData();
  }, [page]);

  async function loadData() {
    setLoading(true);
    try {
      const uid = filterUserId.trim();
      const res = await adminGetSystemNotifications({
        page,
        pageSize,
        userId: uid ? Number(uid) : undefined,
      });
      setItems(res.items || []);
      setTotal(res.total || 0);
    } catch (e) {
      toast({ title: '加载通知失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    } finally {
      setLoading(false);
    }
  }

  async function handlePublish() {
    const uid = targetUserId.trim();
    if (uid && (!Number.isInteger(Number(uid)) || Number(uid) < 1)) {
      toast({ title: '用户 ID 须为正整数', variant: 'destructive' });
      return;
    }
    const scope = uid ? `用户 ${uid}` : '全部用户';
    if (!confirm(`确定向${scope}发布公告「${title.trim()}」吗？`)) return;

    setPublishing(true);
    try {
      const count = await adminPublishNotification({
        title: title.trim(),
        content: content.trim(),
        userId: uid ? Number(uid) : undefined,
      });
      toast({ title: '发布成功', description: `已发送给 ${count} 位用户` });
      setTitle('');
      setContent('');
      setTargetUserId('');
      setPage(1);
      loadData();
    } catch (e) {
      toast({ title: '发布失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    } finally {
      setPublishing(false);
    }
  }

  async function handleDelete(n: SystemNotification) {
    if (!confirm(`确定删除发给用户 ${n.userId} 的通知「${n.notificationTitle}」吗？`)) return;
    try {
      await adminDeleteNotification(n.id);
      loadData();
    } catch (e) {
      toast({ title: '删除失败', description: e instanceof Error ? e.message : '未知错误', variant: 'destructive' });
    }
  }

  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const canPublish = title.trim().length > 0 && content.trim().length > 0 && !publishing;

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>发布系统公告</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="notice-title">标题</Label>
            <Input id="notice-title" value={title} maxLength={100} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="notice-content">内容</Label>
            <Textarea
              id="notice-content"
              value={content}
              maxLength={500}
              className="min-h-[100px]"
              onChange={(e) => setContent(e.target.value)}
            />
            <p className="text-xs text-muted-foreground text-right">{content.length} / 500</p>
          </div>
          <div className="space-y-2">
            <Label htmlFor="notice-user">接收用户 ID（留空发给全部用户）</Label>
            <Input
              id="notice-user"
              value={targetUserId}
              placeholder="全部用户"
              onChange={(e) => setTargetUserId(e.target.value)}
            />
          </div>
          <Button onClick={handlePublish} disabled={!canPublish}>
            {publishing ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Send className="w-4 h-4 mr-2" />}
            发布
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>已发送通知</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <Input
              placeholder="按接收用户 ID 筛选..."
              value={filterUserId}
              onChange={(e) => setFilterUserId(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && (setPage(1), loadData())}
            />
            <Button
              onClick={() => {
                setPage(1);
                loadData();
              }}
            >
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
                      <TableHead>接收用户</TableHead>
                      <TableHead>类型</TableHead>
                      <TableHead>标题</TableHead>
                      <TableHead>内容</TableHead>
                      <TableHead>已读</TableHead>
                      <TableHead>发送时间</TableHead>
                      <TableHead className="text-right">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {items.map((n) => (
                      <TableRow key={n.id}>
                        <TableCell className="font-mono text-xs">{n.userId}</TableCell>
                        <TableCell>{TYPE_NAMES[n.notificationType] ?? n.notificationType}</TableCell>
                        <TableCell className="font-medium">{n.notificationTitle}</TableCell>
                        <TableCell className="max-w-[280px] truncate">{n.notificationContent}</TableCell>
                        <TableCell>{n.isRead ? '是' : '否'}</TableCell>
                        <TableCell>{n.sendTime ? new Date(n.sendTime).toLocaleString() : '-'}</TableCell>
                        <TableCell className="text-right">
                          <Button variant="ghost" size="sm" onClick={() => handleDelete(n)}>
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                    {items.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-6">
                          暂无通知
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>

              <div className="flex items-center justify-between">
                <div className="text-sm text-muted-foreground">
                  共 {total} 条，第 {page} / {totalPages} 页
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
    </div>
  );
}
