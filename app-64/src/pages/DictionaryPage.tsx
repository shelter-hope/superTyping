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
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
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
import { Upload, Trash2, FileText, Loader2, AlertCircle } from 'lucide-react';

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
      const data = await getUserDictionaries(user.userId);
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
      // 3. 创建词库记录 (Includes file upload)
      await createCustomDictionary(
        user.userId,
        uploadForm.name,
        [], // Words array ignored by backend if file provided
        uploadForm.file,
        false // isPublic default false
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
      <div className="container max-w-4xl py-8">
        <Card>
          <CardContent className="py-8 text-center">
            <p className="text-muted-foreground">请先登录</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container max-w-4xl py-8">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">词库管理</h1>
            <p className="text-muted-foreground mt-2">
              上传自定义词库文件，用于词源界定模式
            </p>
          </div>
          <Button onClick={() => setShowUploadDialog(true)}>
            <Upload className="w-4 h-4 mr-2" />
            上传词库
          </Button>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>使用说明</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="flex items-start gap-2">
              <AlertCircle className="w-5 h-5 text-primary mt-0.5" />
              <div className="space-y-1">
                <p className="text-sm">
                  <strong>文件格式：</strong>仅支持 .txt 文本文件
                </p>
                <p className="text-sm">
                  <strong>文件内容：</strong>每行一个词语或短语
                </p>
                <p className="text-sm">
                  <strong>文件大小：</strong>不超过 1MB
                </p>
                <p className="text-sm">
                  <strong>文件命名：</strong>文件名不能包含中文字符
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {loading ? (
          <div className="flex justify-center py-12">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        ) : dictionaries.length === 0 ? (
          <Card>
            <CardContent className="py-12 text-center">
              <FileText className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground mb-4">还没有词库</p>
              <Button onClick={() => setShowUploadDialog(true)}>
                <Upload className="w-4 h-4 mr-2" />
                上传第一个词库
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="grid gap-4">
            {dictionaries.map((dict) => (
              <Card key={dict.dictId}>
                <CardContent className="py-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <FileText className="w-8 h-8 text-primary" />
                      <div>
                        <h3 className="font-medium">{dict.name}</h3>
                        <div className="flex items-center gap-4 text-sm text-muted-foreground mt-1">
                          <span>词语数量: {dict.wordCount}</span>
                          {dict.fileName && <span>文件: {dict.fileName}</span>}
                          <span>创建时间: {new Date(dict.createTime).toLocaleDateString()}</span>
                        </div>
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(dict)}
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        <Dialog open={showUploadDialog} onOpenChange={setShowUploadDialog}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>上传词库</DialogTitle>
              <DialogDescription>
                选择一个文本文件，每行一个词语或短语
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="dict-name">词库名称</Label>
                <Input
                  id="dict-name"
                  value={uploadForm.name}
                  onChange={(e) => setUploadForm({ ...uploadForm, name: e.target.value })}
                  placeholder="例如：编程术语、常用单词"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="dict-file">选择文件</Label>
                <input
                  ref={fileInputRef}
                  id="dict-file"
                  type="file"
                  accept=".txt"
                  onChange={handleFileSelect}
                  className="w-full px-3 py-2 border rounded-md"
                />
                {uploadForm.file && (
                  <p className="text-sm text-muted-foreground">
                    已选择: {uploadForm.file.name} ({(uploadForm.file.size / 1024).toFixed(2)} KB)
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
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    上传中...
                  </>
                ) : (
                  <>
                    <Upload className="w-4 h-4 mr-2" />
                    上传
                  </>
                )}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}
