// 词库管理页面
import { useState, useEffect, useRef } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import {
  getUserDictionaries,
  createCustomDictionary,
  deleteDictionary,
} from '@/db/api';
import type { CustomDictionary } from '@/types/database'; // camelCase
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { PageBody, PageHeader } from '@/components/common/PageHeader';
import { Upload, Trash2, Loader2 } from 'lucide-react';

const FILE_RULES = [
  ['文件格式', '仅支持 .txt 纯文本'],
  ['文件内容', '每行一个词语或短语'],
  ['文件大小', '不超过 1MB'],
  ['文件命名', '不能包含中文字符'],
] as const;

export default function DictionaryPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [dictionaries, setDictionaries] = useState<CustomDictionary[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [showUploadDialog, setShowUploadDialog] = useState(false);
  const [uploadForm, setUploadForm] = useState({
    name: '',
    file: null as File | null,
  });

  useEffect(() => {
    if (user) {
      loadDictionaries();
    }
  }, [user]);

  async function loadDictionaries() {
    if (!user) return;

    setLoading(true);
    try {
      const data = await getUserDictionaries();
      setDictionaries(data);
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

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    // 验证文件类型
    const validTypes = ['text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!validTypes.includes(file.type) && !file.name.endsWith('.txt')) {
      toast({
        title: '文件类型错误',
        description: '仅支持 .txt 文件',
        variant: 'destructive',
      });
      return;
    }

    // 验证文件名
    if (/[\u4e00-\u9fa5]/.test(file.name)) {
      toast({
        title: '文件名错误',
        description: '文件名不能包含中文字符，请使用英文文件名',
        variant: 'destructive',
      });
      return;
    }

    // 验证文件大小
    if (file.size > 1048576) {
      toast({
        title: '文件过大',
        description: '文件大小不能超过1MB',
        variant: 'destructive',
      });
      return;
    }

    setUploadForm({
      ...uploadForm,
      file,
      name: uploadForm.name || file.name.replace(/\.[^/.]+$/, ''),
    });
  }

  async function handleUpload() {
    if (!user || !uploadForm.file || !uploadForm.name) {
      toast({
        title: '信息不完整',
        description: '请填写词库名称并选择文件',
        variant: 'destructive',
      });
      return;
    }

    setUploading(true);
    try {
      await createCustomDictionary(
        uploadForm.name,
        uploadForm.file,
        false
      );

      toast({
        title: '上传成功',
        description: `词库"${uploadForm.name}"已创建`,
      });

      // 重置表单
      setUploadForm({ name: '', file: null });
      setShowUploadDialog(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }

      // 刷新列表
      loadDictionaries();
    } catch (error) {
      toast({
        title: '上传失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(dictionary: CustomDictionary) {
    if (!confirm(`确定要删除词库"${dictionary.name}"吗？此操作不可恢复！`)) {
      return;
    }

    try {
      await deleteDictionary(dictionary.dictId);
      toast({
        title: '删除成功',
        description: `词库"${dictionary.name}"已删除`,
      });
      loadDictionaries();
    } catch (error) {
      toast({
        title: '删除失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    }
  }

  if (!user) {
    return (
      <PageBody>
        <PageHeader
          eyebrow="词表 · LEXICON"
          title="词库管理"
          description="登录后可以上传与管理自定义词表。"
        />
        <p className="mt-12 border-y border-border py-12 text-center text-[13px] text-muted-foreground">
          请先登录
        </p>
      </PageBody>
    );
  }

  return (
    <PageBody>
      <PageHeader
        eyebrow="词表 · LEXICON"
        title="词库管理"
        description="上传纯文本词表，每行一个词语或短语。词源界定模式只从这些词里出题。"
        actions={
          <Button onClick={() => setShowUploadDialog(true)}>
            <Upload className="mr-2 h-4 w-4" />
            上传词库
          </Button>
        }
      />

      <dl className="mt-8 grid grid-cols-1 divide-y divide-border border-y border-foreground/80 sm:grid-cols-4 sm:divide-x sm:divide-y-0">
        {FILE_RULES.map(([label, value]) => (
          <div key={label} className="px-2 py-4 sm:px-4">
            <dt className="label-mono text-[9px]">{label}</dt>
            <dd className="mt-2 text-[12.5px] leading-5 text-muted-foreground">{value}</dd>
          </div>
        ))}
      </dl>

      <section className="mt-10 border-t border-border pt-6">
        <div className="flex flex-wrap items-baseline justify-between gap-3">
          <div>
            <p className="label-mono text-[9px]">词表 · my dictionaries</p>
            <h2 className="mt-2 font-serif text-xl leading-none">我的词表</h2>
          </div>
          <span className="mono-num text-[11px] text-muted-foreground">
            {loading ? '读取中' : `${dictionaries.length} 份`}
          </span>
        </div>

        <div className="mt-6">
          {loading ? (
            <div className="border-y border-border py-10">
              <div className="flex items-baseline justify-between">
                <span className="label-mono text-[9px]">载入中</span>
                <span className="label-mono text-[9px]">reading · lexicon</span>
              </div>
              <span className="mt-4 block h-px w-full overflow-hidden bg-border">
                <span className="block h-full w-1/3 animate-loading-sweep bg-foreground" />
              </span>
            </div>
          ) : dictionaries.length === 0 ? (
            <div className="border-y border-border py-14 text-center">
              <p className="font-serif text-[19px]">还没有词表</p>
              <p className="mt-2 text-[12.5px] leading-6 text-muted-foreground">
                上传第一份 .txt，词源界定模式就会从里面取题。
              </p>
              <Button variant="outline" className="mt-5" onClick={() => setShowUploadDialog(true)}>
                <Upload className="mr-2 h-4 w-4" />
                上传第一份词表
              </Button>
            </div>
          ) : (
            <>
              <div className="hidden items-baseline justify-between border-t border-border py-2 sm:flex">
                <span className="label-mono text-[9px]">词表 / 来源</span>
                <div className="flex items-baseline gap-8">
                  <span className="label-mono w-10 text-right text-[9px]">条目</span>
                  <span className="label-mono w-8 text-right text-[9px]">操作</span>
                </div>
              </div>
              <ul className="divide-y divide-border border-y border-border">
                {dictionaries.map((dict) => (
                  <li
                    key={dict.dictId}
                    className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-8"
                  >
                    <div className="min-w-0">
                      <h3 className="truncate font-serif text-[17px] leading-snug">{dict.name}</h3>
                      <p className="mt-1.5 flex flex-wrap items-baseline gap-x-3 gap-y-1 text-[11px] leading-5 text-muted-foreground">
                        {dict.fileName ? (
                          <span className="truncate font-serif text-[12px]">{dict.fileName}</span>
                        ) : (
                          <span className="font-serif text-[12px]">自定义词表</span>
                        )}
                        <span className="mono-num">
                          {dict.createTime ? new Date(dict.createTime).toLocaleDateString() : '—'}
                        </span>
                      </p>
                    </div>
                    <div className="flex items-center justify-between gap-8 sm:justify-end">
                      <div className="text-right">
                        <p className="mono-num text-[14px] leading-none">{dict.wordCount}</p>
                        <p className="label-mono mt-1 text-[9px] sm:hidden">条目</p>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDelete(dict)}
                        aria-label={`删除词表 ${dict.name}`}
                        className="text-muted-foreground hover:text-accent"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </section>

      <Dialog open={showUploadDialog} onOpenChange={setShowUploadDialog}>
        <DialogContent>
          <DialogHeader>
            <span className="label-mono text-[9px]">词表 · import</span>
            <DialogTitle>上传词库</DialogTitle>
            <DialogDescription>
              选择一个 .txt 文本文件，每行一个词语或短语。
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="dict-name" className="label-mono text-[10px]">
                词库名称
              </Label>
              <Input
                id="dict-name"
                value={uploadForm.name}
                onChange={(e) => setUploadForm({ ...uploadForm, name: e.target.value })}
                placeholder="例如：编程术语、常用单词"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="dict-file" className="label-mono text-[10px]">
                选择文件
              </Label>
              <input
                ref={fileInputRef}
                id="dict-file"
                type="file"
                accept=".txt"
                onChange={handleFileSelect}
                className="w-full cursor-pointer border border-border bg-transparent px-3 py-2 text-[12px] text-foreground file:mr-3 file:border-0 file:bg-foreground file:px-2.5 file:py-1 file:font-mono file:text-[11px] file:text-background"
              />
              {uploadForm.file && (
                <p className="text-[11.5px] leading-5 text-muted-foreground">
                  已选择{' '}
                  <span className="mono-num text-foreground">{uploadForm.file.name}</span>
                  <span className="mono-num"> · {(uploadForm.file.size / 1024).toFixed(2)} KB</span>
                </p>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setShowUploadDialog(false);
                setUploadForm({ name: '', file: null });
                if (fileInputRef.current) {
                  fileInputRef.current.value = '';
                }
              }}
              disabled={uploading}
            >
              取消
            </Button>
            <Button
              onClick={handleUpload}
              disabled={!uploadForm.name || !uploadForm.file || uploading}
            >
              {uploading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  上传中...
                </>
              ) : (
                <>
                  <Upload className="mr-2 h-4 w-4" />
                  上传
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageBody>
  );
}
