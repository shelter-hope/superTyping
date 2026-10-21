// 游戏记录管理组件
import { formatNum } from '@/utils';
import { useState, useEffect } from 'react';
import { adminGetGameRecords, adminUpdateGameRecord, adminDeleteGameRecord } from '@/db/api';
import type { GameRecord, GameMode } from '@/types/database';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Search, Edit, Trash2, Loader2 } from 'lucide-react';

export default function GameRecordManagement() {
  const [records, setRecords] = useState<GameRecord[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [searchTerm, setSearchTerm] = useState('');
  const [gameMode, setGameMode] = useState<GameMode | 'all'>('all');
  const [loading, setLoading] = useState(true);
  const [editingRecord, setEditingRecord] = useState<GameRecord | null>(null);
  const [editForm, setEditForm] = useState({
    totalScore: 0,
    roundNum: 0,
    avgSpeed: 0,
    peakSpeed: 0,
    avgAccuracy: 0,
    peakAccuracy: 0,
  });
  const { toast } = useToast();

  useEffect(() => {
    loadRecords();
  }, [page, searchTerm, gameMode]);

  async function loadRecords() {
    setLoading(true);
    try {
      const userId = searchTerm.trim() ? Number(searchTerm.trim()) : undefined;
      const result = await adminGetGameRecords(
        page,
        pageSize,
        Number.isFinite(userId) ? userId : undefined,
        gameMode === 'all' ? undefined : gameMode
      );
      setRecords(result.records);
      setTotal(result.total);
    } catch (error) {
      toast({
        title: '加载失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }

  function handleSearch() {
    setPage(1);
    loadRecords();
  }

  function openEditDialog(record: GameRecord) {
    setEditingRecord(record);
    setEditForm({
      totalScore: record.totalScore,
      roundNum: record.roundNum,
      avgSpeed: record.avgSpeed,
      peakSpeed: record.peakSpeed,
      avgAccuracy: record.avgAccuracy,
      peakAccuracy: record.peakAccuracy,
    });
  }

  async function handleUpdateRecord() {
    if (!editingRecord) return;

    try {
      await adminUpdateGameRecord(editingRecord.recordId, editForm);
      toast({
        title: '更新成功',
        description: '游戏记录已更新',
      });
      setEditingRecord(null);
      loadRecords();
    } catch (error) {
      toast({
        title: '更新失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    }
  }

  async function handleDeleteRecord(record: GameRecord) {
    if (!confirm('确定要删除这条游戏记录吗？此操作不可恢复！')) {
      return;
    }

    try {
      await adminDeleteGameRecord(record.recordId);
      toast({
        title: '删除成功',
        description: '游戏记录已删除',
      });
      loadRecords();
    } catch (error) {
      toast({
        title: '删除失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    }
  }

  const totalPages = Math.ceil(total / pageSize);

  return (
    <Card>
      <CardHeader>
        <CardTitle>游戏记录管理</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex gap-2">
          <Input
            placeholder="搜索用户ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          />
          <Select
            value={gameMode}
            onValueChange={(value) => {
              setGameMode(value as GameMode | 'all');
              setPage(1);
            }}
          >
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部模式</SelectItem>
              <SelectItem value="endless">无尽适应</SelectItem>
              <SelectItem value="dictionary">词源界定</SelectItem>
            </SelectContent>
          </Select>
          <Button onClick={handleSearch}>
            <Search className="w-4 h-4 mr-2" />
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
                    <TableHead>用户ID</TableHead>
                    <TableHead>模式</TableHead>
                    <TableHead>分数</TableHead>
                    <TableHead>轮数</TableHead>
                    <TableHead>平均速度</TableHead>
                    <TableHead>平均准确率</TableHead>
                    <TableHead>时间</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {records.map((record) => (
                    <TableRow key={record.recordId}>
                      <TableCell className="font-mono text-xs">
                        {record.userId}
                      </TableCell>
                      <TableCell>
                        {record.gameMode === 'endless' ? '无尽适应' : '词源界定'}
                      </TableCell>
                      <TableCell className="font-medium">{record.totalScore}</TableCell>
                      <TableCell>{record.roundNum}</TableCell>
                      <TableCell>{formatNum(record.avgSpeed)}</TableCell>
                      <TableCell>{formatNum(record.avgAccuracy)}%</TableCell>
                      <TableCell>{record.createTime ? new Date(record.createTime).toLocaleString() : '-'}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEditDialog(record)}
                          >
                            <Edit className="w-4 h-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeleteRecord(record)}
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
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
                  onClick={() => setPage(page - 1)}
                >
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === totalPages}
                  onClick={() => setPage(page + 1)}
                >
                  下一页
                </Button>
              </div>
            </div>
          </>
        )}

        <Dialog open={!!editingRecord} onOpenChange={() => setEditingRecord(null)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>编辑游戏记录</DialogTitle>
              <DialogDescription>修改游戏记录的数据</DialogDescription>
            </DialogHeader>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="totalScore">分数</Label>
                <Input
                  id="totalScore"
                  type="number"
                  value={editForm.totalScore}
                  onChange={(e) => setEditForm({ ...editForm, totalScore: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="roundNum">轮数</Label>
                <Input
                  id="roundNum"
                  type="number"
                  value={editForm.roundNum}
                  onChange={(e) => setEditForm({ ...editForm, roundNum: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="avgSpeed">平均速度</Label>
                <Input
                  id="avgSpeed"
                  type="number"
                  step="0.1"
                  value={editForm.avgSpeed}
                  onChange={(e) => setEditForm({ ...editForm, avgSpeed: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="peakSpeed">峰值速度</Label>
                <Input
                  id="peakSpeed"
                  type="number"
                  step="0.1"
                  value={editForm.peakSpeed}
                  onChange={(e) => setEditForm({ ...editForm, peakSpeed: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="avgAccuracy">平均准确率</Label>
                <Input
                  id="avgAccuracy"
                  type="number"
                  step="0.1"
                  value={editForm.avgAccuracy}
                  onChange={(e) => setEditForm({ ...editForm, avgAccuracy: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="peakAccuracy">峰值准确率</Label>
                <Input
                  id="peakAccuracy"
                  type="number"
                  step="0.1"
                  value={editForm.peakAccuracy}
                  onChange={(e) => setEditForm({ ...editForm, peakAccuracy: Number(e.target.value) })}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setEditingRecord(null)}>
                取消
              </Button>
              <Button onClick={handleUpdateRecord}>保存</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  );
}
