// 404页面
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Home } from 'lucide-react';

export default function NotFoundPage() {
  return (
    <div className="container flex items-center justify-center min-h-[calc(100vh-4rem)]">
      <div className="text-center space-y-6">
        <div className="space-y-2">
          <h1 className="text-9xl font-bold gradient-text">404</h1>
          <h2 className="text-3xl font-bold">页面未找到</h2>
          <p className="text-muted-foreground">
            抱歉，您访问的页面不存在
          </p>
        </div>
        <Link to="/">
          <Button size="lg">
            <Home className="mr-2 h-5 w-5" />
            返回首页
          </Button>
        </Link>
      </div>
    </div>
  );
}
