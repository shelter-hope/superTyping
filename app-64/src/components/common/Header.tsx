// 头部导航组件
import { useEffect, useState } from 'react';
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
import { connectIm, subscribeIm } from '@/features/im/imClient';

export default function Header() {
  const { user, profile, signOut, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (!user) return;
    getUnreadCount().then(setUnread).catch(() => setUnread(0));
    connectIm().catch(() => undefined);
    return subscribeIm((event) => {
      if (event.event === 'unread' && typeof event.count === 'number') setUnread(event.count);
      if (event.event === 'friend_request' || event.event === 'notification') {
        getUnreadCount().then(setUnread).catch(() => undefined);
      }
    });
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
    <header className="sticky top-0 z-50 w-full border-b border-border/80 bg-[linear-gradient(180deg,hsl(216_21%_6%/0.98),hsl(216_18%_9%/0.92))] shadow-[0_18px_34px_hsl(216_35%_2%/0.42)] backdrop-blur supports-[backdrop-filter]:bg-background/80">
      <div className="container relative flex h-16 items-center justify-between before:absolute before:bottom-0 before:left-8 before:h-px before:w-40 before:bg-primary/50 before:shadow-[0_0_12px_hsl(var(--primary)/0.35)]">
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-3">
            <span className="status-light" />
            <img src="../../../public/favicon.png" alt="Logo" className="h-8 w-8 border border-border bg-muted/50 p-1" />
            <span className="industrial-heading text-2xl gradient-text">打字进化</span>
          </Link>
          
          <nav className="hidden items-center gap-2 md:flex">
            {visibleRoutes.map(route => (
              <Link key={route.path} to={route.path}>
                <Button
                  variant={location.pathname === route.path ? 'default' : 'ghost'}
                  size="sm"
                >
                  {route.name}
                </Button>
              </Link>
            ))}
          </nav>
        </div>

        <div className="flex items-center gap-2">
          {user ? (
            <Link to="/messages" className="relative">
              <Button variant="ghost" size="sm">消息{unread > 0 ? ` ${unread}` : ''}</Button>
              {unread > 0 && <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[10px] text-primary-foreground">{unread}</span>}
            </Link>
          ) : null}
          {user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="gap-2">
                  <UserAvatar profile={profile} size="sm" />
                  <span className="hidden sm:inline">{getDisplayName(profile)}</span>
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
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  登录
                </Button>
              </Link>
              <Link to="/register">
                <Button size="sm">
                  注册
                </Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
