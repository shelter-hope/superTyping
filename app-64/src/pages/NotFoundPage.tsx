// 404页面
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';

export default function NotFoundPage() {
  return (
    <div className="container flex min-h-[calc(100vh-4rem)] max-w-3xl flex-col justify-center py-16">
      <div className="flex items-center gap-3">
        <span className="status-light mt-0.5" />
        <span className="label-mono text-muted-foreground">地址无效 · NOT FOUND</span>
      </div>

      <div className="mt-6 grid gap-10 md:grid-cols-[minmax(0,1fr)_260px] md:items-end">
        <div>
          <span className="mono-num block text-[104px] leading-[0.85] tracking-tight md:text-[140px]">404</span>
          <h1 className="mt-6 font-serif text-[30px] leading-tight md:text-[38px]">这个地址没有排过字</h1>
          <p className="mt-4 max-w-[44ch] text-[13px] leading-6 text-muted-foreground">
            页面可能已经改过名，或者链接里少了一段。回到首页重新开始即可。
          </p>
        </div>

        <div className="border border-border">
          <div className="border-b border-border px-4 py-2.5">
            <span className="label-mono text-muted-foreground">可以去的地方</span>
          </div>
          <div className="flex flex-col gap-2 p-4">
            <Link to="/" className="text-[13px] underline decoration-border underline-offset-4 hover:decoration-foreground">
              首页
            </Link>
            <Link to="/apps/typing/prepare" className="text-[13px] underline decoration-border underline-offset-4 hover:decoration-foreground">
              配置一局
            </Link>
            <Link to="/apps/typing/leaderboard" className="text-[13px] underline decoration-border underline-offset-4 hover:decoration-foreground">
              排行榜
            </Link>
          </div>
        </div>
      </div>

      <div className="mt-12 flex items-center gap-3 border-t border-border pt-6">
        <Button asChild>
          <Link to="/">返回首页 →</Link>
        </Button>
      </div>
    </div>
  );
}
