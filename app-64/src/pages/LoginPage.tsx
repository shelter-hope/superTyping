// 登录页面：左宣言 / 右表单（发丝线分隔，无卡片阴影）
import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { Loader2 } from 'lucide-react';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const submittingRef = useRef(false);
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submittingRef.current) return;

    if (!username || !password) {
      toast({
        title: '请填写完整信息',
        description: '用户名和密码不能为空',
        variant: 'destructive',
      });
      return;
    }

    submittingRef.current = true;
    setLoading(true);
    try {
      await signIn(username, password);
      toast({
        title: '登录成功',
        description: '欢迎回来！',
      });
      navigate('/');
    } catch (error) {
      toast({
        title: '登录失败',
        description: error instanceof Error ? error.message : '用户名或密码错误',
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
        <p className="section-eyebrow">账户 · sign in</p>
        <h1 className="mt-8 display-serif text-[clamp(2.3rem,4.6vw,3.5rem)] leading-[1.08] tracking-[-0.01em]">
          欢迎回来
        </h1>
        <p className="mt-6 max-w-md text-[14px] leading-[2] text-muted-foreground">
          档案还在原处：难度、弱点字符、遗传代数都按上一次结算继续。你只需要把手放回键盘上。
        </p>

        <div className="mt-10 max-w-md border-t border-border pt-6">
          <p className="label-mono mb-3 text-[10px]">本地演示账号</p>
          <dl className="space-y-2 text-[12.5px]">
            <div className="flex items-baseline gap-4">
              <dt className="mono-num w-16 text-muted-foreground">普通</dt>
              <dd className="mono-num">test / test123</dd>
            </div>
            <div className="flex items-baseline gap-4">
              <dt className="mono-num w-16 text-muted-foreground">管理员</dt>
              <dd className="mono-num">admin / admin123</dd>
            </div>
          </dl>
        </div>
      </div>

      <div className="lg:col-span-5 lg:col-start-8">
        <form
          onSubmit={handleSubmit}
          className="border border-border bg-card p-7 sm:p-9"
          noValidate
        >
          <div className="flex items-baseline justify-between border-b border-border pb-4">
            <p className="display-serif text-[15px]">登录</p>
            <span className="label-mono text-[9px]">session</span>
          </div>

          <div className="mt-7 space-y-6">
            <div className="space-y-2">
              <Label htmlFor="username" className="label-mono text-[10px]">
                用户名
              </Label>
              <Input
                id="username"
                type="text"
                placeholder="请输入用户名"
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
                placeholder="请输入密码"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                autoComplete="current-password"
              />
            </div>
          </div>

          <Button type="submit" size="lg" className="mt-8 w-full" disabled={loading}>
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            登录
          </Button>

          <p className="mt-6 text-center text-[12px] text-muted-foreground">
            还没有账号？{' '}
            <Link
              to="/register"
              className="text-foreground underline decoration-accent decoration-2 underline-offset-4 hover:text-accent"
            >
              立即注册
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
