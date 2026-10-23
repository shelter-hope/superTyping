// 旧结算页会把任意一场对局写成无尽模式。正式结算已经在对局页完成并上传。
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';

export default function ResultPage() {
  return (
    <div className="container max-w-xl py-16 text-center space-y-4">
      <h1 className="text-3xl font-bold">结算已在对局页完成</h1>
      <p className="text-muted-foreground">
        游戏结束时会在对局页面保存记录。这里不再重复上传，避免把限时或词源对局记成无尽模式。
      </p>
      <div className="flex justify-center gap-3">
        <Button asChild>
          <Link to="/prepare">再来一局</Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/profile">查看个人数据</Link>
        </Button>
      </div>
    </div>
  );
}
