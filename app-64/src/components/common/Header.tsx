// 头部导航组件：报头式（brand + 竖线 + 目录），层级靠发丝线，不靠阴影
import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { User, LogOut, Settings, Shield } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import routes from '@/routes';
import UserAvatar from '@/components/profile/UserAvatar';
import { getDisplayName } from '@/lib/avatars';
import { getUnreadCount } from '@/services/social';
import { useImSocket } from '@/hooks/use-im-socket';
import type { ImEvent } from '@/services/imSocket';

export default function Header() {
  const { user, profile, signOut, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();
  const [unread, setUnread] = useState(0);

  const onImEvent = useCallback((event: ImEvent) => {
    if (event.type === 'UNREAD') {
      setUnread(event.count);
      return;
    }
    if (event.type === 'FRIEND_REQUEST' || event.type === 'MESSAGE') {
      getUnreadCount().then(setUnread).catch(() => undefined);
    }
  }, []);

  useImSocket(onImEvent, !!user);

  useEffect(() => {
    if (!user) {
      setUnread(0);
      return;
    }
    getUnreadCount().then(setUnread).catch(() => setUnread(0));
  }, [user]);

  async function handleSignOut() {
    try {
      await signOut();
      toast({
        title: '已退出登录',
        description: '期待您的再次光临！',
      });
      navigate('/');
    } catch (error) {
      toast({
        title: '退出失败',
        description: error instanceof Error ? error.message : '未知错误',
        variant: 'destructive',
      });
    }
  }

  const visibleRoutes = routes.filter(route => route.visible && (!route.requireAuth || user));

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border bg-background">
      <div className="container flex h-[60px] items-center justify-between gap-6">
        <div className="flex min-w-0 items-center gap-6">
          <Link to="/" className="group flex items-baseline gap-2">
            <span className="mb-[3px] h-[6px] w-[6px] shrink-0 rounded-full bg-accent" aria-hidden />
            <span className="display-serif whitespace-nowrap text-[17px] tracking-[0.02em]">打字进化</span>
            <span className="label-mono hidden text-[10px] text-muted-foreground/70 lg:inline">
              type·evolve
            </span>
          </Link>

          <details className="relative md:hidden">
            <summary className="label-mono cursor-pointer list-none border border-border px-2 py-1 text-[10px] text-foreground">
              目录
            </summary>
            <div className="absolute left-0 z-50 mt-2 flex w-52 flex-col border border-border bg-popover p-1 shadow-sm">
              {visibleRoutes.map(route => (
                <Link
                  key={route.path}
                  to={route.path}
                  className="px-3 py-2 text-[13px] text-foreground hover:bg-muted"
                >
                  {route.name}
                </Link>
              ))}
            </div>
          </details>

          <nav className="hidden items-center gap-5 md:flex">
            {visibleRoutes.map(route => {
              const active = location.pathname === route.path;
              return (
                <Link
                  key={route.path}
                  to={route.path}
                  className={`group relative border-b-2 py-1 text-[13px] tracking-[0.02em] transition-colors ${
                    active
                      ? 'border-accent text-foreground'
                      : 'border-transparent text-muted-foreground hover:border-border hover:text-foreground'
                  }`}
                >
                  {route.name}
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="flex shrink-0 items-center gap-3">
          {user ? (
            <Link to="/messages" className="hidden items-center gap-1.5 text-[13px] text-muted-foreground transition-colors hover:text-foreground sm:flex">
              消息
              {unread > 0 && (
                <span className="mono-num flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-accent px-1 text-[10px] leading-none text-accent-foreground">
                  {unread}
                </span>
              )}
            </Link>
          ) : null}
          {user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="h-9 gap-2 px-2">
                  <UserAvatar profile={profile} size="sm" />
                  <span className="hidden max-w-[9rem] truncate text-[13px] sm:inline">
                    {getDisplayName(profile)}
                  </span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuItem onClick={() => navigate('/profile')}>
                  <User className="w-4 h-4 mr-2" />
                  个人信息
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => navigate('/settings')}>
                  <Settings className="w-4 h-4 mr-2" />
                  设置
                </DropdownMenuItem>
                {isAdmin && (
                  <>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => navigate('/admin')}>
                      <Shield className="w-4 h-4 mr-2" />
                      管理后台
                    </DropdownMenuItem>
                  </>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleSignOut}>
                  <LogOut className="w-4 h-4 mr-2" />
                  退出登录
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <>
              <Link
                to="/login"
                className="text-[13px] text-muted-foreground transition-colors hover:text-foreground"
              >
                登录
              </Link>
              <Link to="/register">
                <Button size="sm">注册</Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
