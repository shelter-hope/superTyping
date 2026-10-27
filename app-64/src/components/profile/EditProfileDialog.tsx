// 个人信息编辑对话框
import { useEffect, useRef, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { updateMyProfile, uploadAvatar } from '@/db/api';
import { resolveBackendAssetUrl } from '@/db/client';
import { PRESET_AVATARS, getDefaultAvatar } from '@/lib/avatars';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Pencil, Loader2, Upload } from 'lucide-react';

const ACCEPT_IMAGE = 'image/jpeg,image/png,image/gif,image/webp';

export default function EditProfileDialog() {
  const { user, profile } = useAuth();
  const { toast } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [nickname, setNickname] = useState(profile?.nickname || '');
  const [selectedAvatar, setSelectedAvatar] = useState(
    profile?.avatar || getDefaultAvatar(profile?.username || '')
  );
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [filePreview, setFilePreview] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setNickname(profile?.nickname || '');
      setSelectedAvatar(profile?.avatar || getDefaultAvatar(profile?.username || ''));
    }
  }, [open, profile?.nickname, profile?.avatar, profile?.username]);

  useEffect(() => {
    if (!open) {
      setSelectedFile(null);
      if (filePreview) {
        URL.revokeObjectURL(filePreview);
        setFilePreview(null);
      }
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }, [open]);

  async function handleSave() {
    if (!user || !profile) return;

    setLoading(true);
    try {
      await updateMyProfile({
        nickname: nickname.trim() || undefined,
        avatar: selectedAvatar,
      });

      toast({
        title: '保存成功',
        description: '个人信息已更新',
      });

      setOpen(false);
      // 刷新页面以更新显示
      window.location.reload();
    } catch (error) {
      console.error('保存失败:', error);
      toast({
        title: '保存失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      toast({ title: '请选择图片文件', description: '支持 JPG、PNG、GIF、WebP', variant: 'destructive' });
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      toast({ title: '图片过大', description: '请选择 2MB 以内的图片', variant: 'destructive' });
      return;
    }
    setSelectedFile(file);
    const url = URL.createObjectURL(file);
    setFilePreview(url);
  }

  async function handleUseUploadedAvatar() {
    if (!user || !selectedFile) return;
    setUploading(true);
    try {
      const avatarUrl = await uploadAvatar(user.userId, selectedFile);
      setSelectedAvatar(avatarUrl);
      if (filePreview) URL.revokeObjectURL(filePreview);
      setFilePreview(null);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      toast({ title: '已选择头像', description: '点击保存后生效' });
    } catch (error) {
      console.error('上传头像失败:', error);
      toast({
        title: '上传失败',
        description: error instanceof Error ? error.message : '请稍后重试',
        variant: 'destructive',
      });
    } finally {
      setUploading(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <Pencil />
          编辑资料
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="font-serif text-xl">编辑个人信息</DialogTitle>
          <DialogDescription className="text-[12.5px] leading-[1.7]">
            修改你的头像和昵称
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 pt-2">
          {/* 昵称输入 */}
          <div className="space-y-2">
            <Label htmlFor="nickname" className="text-[13px]">
              昵称
            </Label>
            <Input
              id="nickname"
              placeholder="输入昵称（留空则显示用户名）"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              maxLength={20}
            />
            <p className="mono-num text-[11px] text-muted-foreground">@{profile?.username}</p>
          </div>

          {/* 当前头像预览 */}
          <div>
            <div className="label-mono text-[10px]">当前头像</div>
            <div className="mt-2 flex items-center gap-4 border-t border-border pt-3">
              <div className="shrink-0 border border-foreground/20 p-1">
                <img
                  src={resolveBackendAssetUrl(selectedAvatar) || getDefaultAvatar(profile?.username || '')}
                  alt="当前头像"
                  className="h-20 w-20 rounded-full object-cover"
                />
              </div>
              <p className="text-[12px] leading-[1.7] text-muted-foreground">点击下方头像选择新头像</p>
            </div>
          </div>

          {/* 预设头像选择 */}
          <div>
            <div className="label-mono text-[10px]">选择预设头像</div>
            <div className="mt-2 grid grid-cols-5 gap-3 border-t border-border pt-3">
              {PRESET_AVATARS.map((avatar, index) => (
                <button
                  key={index}
                  type="button"
                  onClick={() => setSelectedAvatar(avatar)}
                  aria-pressed={selectedAvatar === avatar}
                  className={`aspect-square w-full border p-1 transition-colors ${
                    selectedAvatar === avatar
                      ? 'border-accent'
                      : 'border-border hover:border-foreground/40'
                  }`}
                >
                  <img src={avatar} alt={`头像 ${index + 1}`} className="h-full w-full rounded-full object-cover" />
                </button>
              ))}
            </div>
          </div>

          {/* 选择本地图片作为头像 */}
          <div>
            <div className="label-mono text-[10px]">选择图片文件</div>
            <input
              ref={fileInputRef}
              type="file"
              accept={ACCEPT_IMAGE}
              className="hidden"
              onChange={handleFileChange}
            />
            <div className="mt-2 flex flex-wrap items-center gap-3 border-t border-border pt-3">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
              >
                <Upload />
                选择图片
              </Button>
              {selectedFile && (
                <>
                  <div className="flex min-w-0 items-center gap-2">
                    {filePreview && (
                      <img
                        src={filePreview}
                        alt="预览"
                        className="h-14 w-14 rounded-full border border-border object-cover"
                      />
                    )}
                    <span className="mono-num max-w-[180px] truncate text-[11px] text-muted-foreground">
                      {selectedFile.name}
                    </span>
                  </div>
                  <Button type="button" size="sm" onClick={handleUseUploadedAvatar} disabled={uploading}>
                    {uploading ? <Loader2 className="animate-spin" /> : null}
                    使用
                  </Button>
                </>
              )}
            </div>
            <p className="mt-2 text-[12px] leading-[1.7] text-muted-foreground">
              支持 JPG、PNG、GIF、WebP，建议 2MB 以内
            </p>
          </div>
        </div>

        {/* 操作按钮 */}
        <div className="flex justify-end gap-2 border-t border-border pt-4">
          <Button variant="outline" onClick={() => setOpen(false)} disabled={loading}>
            取消
          </Button>
          <Button onClick={handleSave} disabled={loading}>
            {loading && <Loader2 className="animate-spin" />}
            保存
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
