// 注册页面：左宣言 / 右表单
import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Loader2 } from 'lucide-react';

const RULES = [
  ['用户名', '字母、数字与下划线'],
  ['密码', '至少 6 个字符'],
  ['数据', '逐键记录只存在本机'],
];

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const submittingRef = useRef(false);
  const { signUp } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submittingRef.current) return;

    if (!username || !password || !confirmPassword) {
      toast({
        title: '请填写完整信息',
        description: '所有字段都不能为空',
        variant: 'destructive',
      });
      return;
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      toast({
        title: '用户名格式错误',
        description: '用户名只能包含字母、数字和下划线',
        variant: 'destructive',
      });
      return;
    }

    if (password.length < 6) {
      toast({
        title: '密码太短',
        description: '密码至少需要6个字符',
        variant: 'destructive',
      });
      return;
    }

    if (password !== confirmPassword) {
      toast({
        title: '密码不匹配',
        description: '两次输入的密码不一致',
        variant: 'destructive',
      });
      return;
    }

    submittingRef.current = true;
    setLoading(true);
    try {
      await signUp(username, password);
      toast({
        title: '注册成功',
        description: '欢迎加入打字进化游戏！',
      });
      navigate('/');
    } catch (error) {
      const message = error instanceof Error ? error.message : '注册过程中出现错误';
      toast({
        title: message.includes('用户名已存在') ? '用户名已被占用' : '注册失败',
        description: message.includes('用户名已存在')
          ? '该账号可能已注册成功，请直接登录'
          : message,
        variant: 'destructive',
      });
    } finally {
      submittingRef.current = false;
      setLoading(false);
    }
  }

  return (
    <div className="container grid min-h-[calc(100vh-9rem)] items-center gap-14 py-14 lg:grid-cols-12 lg:gap-8">
      <div className="lg:col-span-6">
        <p className="section-eyebrow">账户 · create</p>
        <h1 className="mt-8 display-serif text-[clamp(2.3rem,4.6vw,3.5rem)] leading-[1.08] tracking-[-0.01em]">
          建一份档案
        </h1>
        <p className="mt-6 max-w-md text-[14px] leading-[2] text-muted-foreground">
          从第一局开始，它会记录你在哪个键上犹豫、在哪里停顿。
          三十局之后，你会看见一条只属于你的错字曲线。
        </p>

        <dl className="mt-10 max-w-md divide-y divide-border border-t border-border">
          {RULES.map(([k, v]) => (
            <div key={k} className="flex items-baseline gap-6 py-3">
              <dt className="label-mono w-16 shrink-0 text-[10px]">{k}</dt>
              <dd className="text-[12.5px] text-muted-foreground">{v}</dd>
            </div>
          ))}
        </dl>
      </div>

      <div className="lg:col-span-5 lg:col-start-8">
        <form
          onSubmit={handleSubmit}
          className="border border-border bg-card p-7 sm:p-9"
          noValidate
        >
          <div className="flex items-baseline justify-between border-b border-border pb-4">
            <p className="display-serif text-[15px]">注册</p>
            <span className="label-mono text-[9px]">new profile</span>
          </div>

          <div className="mt-7 space-y-6">
            <div className="space-y-2">
              <Label htmlFor="username" className="label-mono text-[10px]">
                用户名
              </Label>
              <Input
                id="username"
                type="text"
                placeholder="字母、数字和下划线"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={loading}
                autoComplete="username"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password" className="label-mono text-[10px]">
                密码
              </Label>
              <Input
                id="password"
                type="password"
                placeholder="至少 6 个字符"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                autoComplete="new-password"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirmPassword" className="label-mono text-[10px]">
                确认密码
              </Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="再次输入密码"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                disabled={loading}
                autoComplete="new-password"
              />
            </div>
          </div>

          <Button type="submit" size="lg" className="mt-8 w-full" disabled={loading}>
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            注册
          </Button>

          <p className="mt-6 text-center text-[12px] text-muted-foreground">
            已有档案？{' '}
            <Link
              to="/login"
              className="text-foreground underline decoration-accent decoration-2 underline-offset-4 hover:text-accent"
            >
              立即登录
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
