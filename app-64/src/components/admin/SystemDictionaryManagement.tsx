// 系统词库管理
import { useEffect, useState } from 'react';
import { adminGetSystemDictionaries, adminGetSystemDictionaryWords, adminGetCustomDictionaries, type AdminPageResult } from '@/db/api';
import {
  adminCreateSystemDictionary,
  adminDeleteSystemDictionary,
  adminDeleteSystemWord,
  adminExportSystemDictionary,
  adminImportSystemDictionary,
  adminSetCustomDictionaryVisibility,
  adminSetSystemDictionaryActive,
  adminCreateSystemWord,
} from '@/services/admin';
import type { CustomDictionary } from '@/types/database';
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
  const [customItems, setCustomItems] = useState<CustomDictionary[]>([]);
  const [newName, setNewName] = useState('');
  const [newWord, setNewWord] = useState('');
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
      const custom = await adminGetCustomDictionaries({ page: 1, pageSize: 20 });
      setCustomItems(custom.items || []);
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
            <Input className="max-w-xs" placeholder="新词库名称" value={newName} onChange={(e) => setNewName(e.target.value)} />
            <Button variant="outline" onClick={() => void createDict()}>新建</Button>
            <label className="text-sm">
              导入 txt/csv
              <input className="ml-2 text-sm" type="file" accept=".txt,.csv,text/plain,text/csv" onChange={(e) => void importFile(e.target.files?.[0])} />
            </label>
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
                        <TableCell className="text-right space-x-1">
                          <Button variant="ghost" size="sm" onClick={() => openWords(d)}>
                            <BookOpen className="w-4 h-4 mr-1" />
                            单词
                          </Button>
                          <Button variant="ghost" size="sm" onClick={() => void toggleDict(d)}>{d.isActive ? '停用' : '启用'}</Button>
                          <Button variant="ghost" size="sm" onClick={() => void exportDict(d.dictId)}>导出</Button>
                          <Button variant="ghost" size="sm" onClick={() => void removeDict(d.dictId)}>删除</Button>
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
                    <TableHead />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {words.map((w) => (
                    <TableRow key={w.wordId}>
                      <TableCell className="font-mono text-xs">{w.word}</TableCell>
                      <TableCell>{w.totalCount ?? '-'}</TableCell>
                      <TableCell>{w.errorCount ?? '-'}</TableCell>
                      <TableCell>{w.difficultyLevel ?? '-'}</TableCell>
                      <TableCell><Button variant="ghost" size="sm" onClick={() => void removeWord(w.wordId)}>删除</Button></TableCell>
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
          <div className="flex gap-2">
            <Input placeholder="新增单词" value={newWord} onChange={(e) => setNewWord(e.target.value)} />
            <Button onClick={() => void addWord()}>添加</Button>
          </div>
          <DialogFooter className="mt-3">
            <Button variant="outline" onClick={() => setWordDialogOpen(false)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Card className="mt-6">
        <CardHeader><CardTitle>用户词库审核</CardTitle></CardHeader>
        <CardContent className="space-y-2">
          {customItems.map((item) => (
            <div key={item.dictId} className="flex flex-wrap items-center justify-between gap-2 rounded border px-3 py-2">
              <span>{item.name} · 用户 {item.userId} · {item.isPublic ? '公开' : '未公开'}</span>
              <Button size="sm" variant="outline" onClick={() => void setVisibility(item)}>{item.isPublic ? '下架' : '公开'}</Button>
            </div>
          ))}
          {customItems.length === 0 && <p className="text-sm text-muted-foreground">没有待审核的自定义词库。</p>}
        </CardContent>
      </Card>
    </>
  );

  async function createDict() {
    if (!newName.trim()) return;
    await adminCreateSystemDictionary({ name: newName.trim(), isActive: true });
    setNewName('');
    await loadData();
  }

  async function toggleDict(dict: SystemDictionary) {
    await adminSetSystemDictionaryActive(dict.dictId, !dict.isActive);
    await loadData();
  }

  async function removeDict(dictId: number) {
    await adminDeleteSystemDictionary(dictId);
    await loadData();
  }

  async function exportDict(dictId: number) {
    const file = await adminExportSystemDictionary(dictId);
    const blob = new Blob([file.content], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  async function importFile(file?: File) {
    if (!file) return;
    if (!/\.(txt|csv)$/i.test(file.name) || file.size > 1024 * 1024) {
      toast({ title: '只接受 1MB 以内的 txt 或 csv', variant: 'destructive' });
      return;
    }
    const text = await file.text();
    await adminImportSystemDictionary({ filename: file.name, text, name: file.name.replace(/\.(txt|csv)$/i, '') });
    toast({ title: '导入完成' });
    await loadData();
  }

  async function addWord() {
    if (!currentDict || !newWord.trim()) return;
    await adminCreateSystemWord({ dictId: currentDict.dictId, word: newWord.trim(), difficultyLevel: 1 });
    setNewWord('');
    await openWords(currentDict);
  }

  async function removeWord(wordId: number) {
    await adminDeleteSystemWord(wordId);
    if (currentDict) await openWords(currentDict);
  }

  async function setVisibility(item: CustomDictionary) {
    await adminSetCustomDictionaryVisibility(item.dictId, !item.isPublic);
    await loadData();
  }
}

