// 系统词库管理
import { useEffect, useState } from 'react';
import { adminGetSystemDictionaries, adminGetSystemDictionaryWords, type AdminPageResult } from '@/db/api';
import type { SystemDictionary } from '@/types/database';
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
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Search, BookOpen } from 'lucide-react';

interface SystemDictionaryWord {
  wordId: number;
  word: string;
  totalCount?: number;
  errorCount?: number;
  difficultyLevel?: number;
}

export default function SystemDictionaryManagement() {
  const [items, setItems] = useState<SystemDictionary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [loading, setLoading] = useState(false);
  const [wordDialogOpen, setWordDialogOpen] = useState(false);
  const [currentDict, setCurrentDict] = useState<SystemDictionary | null>(null);
  const [words, setWords] = useState<SystemDictionaryWord[]>([]);
  const [wordLoading, setWordLoading] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    loadData();
  }, [page, name, category]);

  async function loadData() {
    setLoading(true);
    try {
      const res: AdminPageResult<SystemDictionary> = await adminGetSystemDictionaries({
        page,
        pageSize,
        name: name || undefined,
        category: category || undefined,
      });
      setItems(res.items || []);
      setTotal(res.total || 0);
    } catch (e) {
      toast({
        title: '加载系统词库失败',
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

  async function openWords(dict: SystemDictionary) {
    setCurrentDict(dict);
    setWordDialogOpen(true);
    setWordLoading(true);
    try {
      const res = await adminGetSystemDictionaryWords(dict.dictId, 1, 200);
      setWords((res.items as SystemDictionaryWord[]) || []);
    } catch (e) {
      toast({
        title: '加载单词失败',
        description: e instanceof Error ? e.message : '未知错误',
        variant: 'destructive',
      });
      setWords([]);
    } finally {
      setWordLoading(false);
    }
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>系统词库管理</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <Input
              className="max-w-xs"
              placeholder="按名称搜索..."
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
            <Input
              className="max-w-xs"
              placeholder="按分类搜索..."
              value={category}
              onChange={(e) => setCategory(e.target.value)}
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
                      <TableHead>分类</TableHead>
                      <TableHead>单词数</TableHead>
                      <TableHead>状态</TableHead>
                      <TableHead>创建时间</TableHead>
                      <TableHead className="text-right">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {items.map((d) => (
                      <TableRow key={d.dictId}>
                        <TableCell className="font-medium">{d.name}</TableCell>
                        <TableCell>{d.category || '-'}</TableCell>
                        <TableCell>{d.wordCount ?? 0}</TableCell>
                        <TableCell>{d.isActive ? '启用' : '停用'}</TableCell>
                        <TableCell>
                          {d.createTime ? new Date(d.createTime).toLocaleDateString() : '-'}
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openWords(d)}
                          >
                            <BookOpen className="w-4 h-4 mr-1" />
                            单词
                          </Button>
                        </TableCell>
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
                  共 {total} 个词库，第 {page} / {totalPages} 页
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

      <Dialog open={wordDialogOpen} onOpenChange={setWordDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>系统词库单词列表</DialogTitle>
          </DialogHeader>
          <div className="space-y-2 mb-2">
            {currentDict && (
              <>
                <p className="text-sm">
                  <Label className="mr-2">词库名称:</Label>
                  {currentDict.name}
                </p>
                {currentDict.description && (
                  <p className="text-sm text-muted-foreground">
                    {currentDict.description}
                  </p>
                )}
              </>
            )}
          </div>
          {wordLoading ? (
            <div className="flex justify-center py-6">
              <Loader2 className="w-6 h-6 animate-spin text-primary" />
            </div>
          ) : (
            <div className="max-h-[360px] overflow-auto border rounded-md">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>单词</TableHead>
                    <TableHead>练习次数</TableHead>
                    <TableHead>错误次数</TableHead>
                    <TableHead>难度</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {words.map((w) => (
                    <TableRow key={w.wordId}>
                      <TableCell className="font-mono text-xs">{w.word}</TableCell>
                      <TableCell>{w.totalCount ?? '-'}</TableCell>
                      <TableCell>{w.errorCount ?? '-'}</TableCell>
                      <TableCell>{w.difficultyLevel ?? '-'}</TableCell>
                    </TableRow>
                  ))}
                  {words.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} className="text-center text-muted-foreground py-4">
                        暂无单词
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>
          )}
          <DialogFooter className="mt-3">
            <Button variant="outline" onClick={() => setWordDialogOpen(false)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

