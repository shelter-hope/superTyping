import React from "react";
import { Link } from "react-router-dom";

/* 页脚 = 版权页（colophon）：品牌、目录、口径说明 */
const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="mt-auto border-t border-border bg-background/60">
      <div className="container py-12">
        <div className="grid grid-cols-1 gap-10 md:grid-cols-12">
          <div className="md:col-span-5">
            <div className="flex items-baseline gap-2">
              <span className="mb-[3px] h-[6px] w-[6px] rounded-full bg-accent" aria-hidden />
              <span className="display-serif text-[19px] tracking-[0.02em]">打字进化</span>
            </div>
            <p className="mt-4 max-w-sm text-[13px] leading-[1.9] text-muted-foreground">
              一台记录你误解与速度的机器：把错字交给遗传算法，把难度交给一个会自我修正的积分器。
              每打完一局，它对你的了解都更准一点。
            </p>
            <p className="label-mono mt-6 flex items-center gap-2 text-[10px]">
              <span className="h-[5px] w-[5px] rounded-full bg-success" aria-hidden />
              本地运行 · 离线可用
            </p>
          </div>

          <div className="md:col-span-3">
            <p className="label-mono mb-4">训练</p>
            <ul className="space-y-2.5 text-[13px]">
              <li><Link to="/prepare" className="text-muted-foreground transition-colors hover:text-foreground">开始一局</Link></li>
              <li><Link to="/trends" className="text-muted-foreground transition-colors hover:text-foreground">趋势分析</Link></li>
              <li><Link to="/dictionary" className="text-muted-foreground transition-colors hover:text-foreground">词库管理</Link></li>
            </ul>
          </div>

          <div className="md:col-span-2">
            <p className="label-mono mb-4">竞技</p>
            <ul className="space-y-2.5 text-[13px]">
              <li><Link to="/leaderboard" className="text-muted-foreground transition-colors hover:text-foreground">排行榜</Link></li>
              <li><Link to="/lab" className="text-muted-foreground transition-colors hover:text-foreground">群体智能实验室</Link></li>
            </ul>
          </div>

          <div className="md:col-span-2">
            <p className="label-mono mb-4">口径</p>
            <ul className="space-y-2.5 text-[13px] text-muted-foreground">
              <li>准确率 = 正确击键 / 总击键</li>
              <li>难度 D ∈ [0.15, 0.95]</li>
              <li>速度按字符/分钟计</li>
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-col gap-2 border-t border-border pt-6 text-[11px] text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p className="mono-num tracking-[0.04em]">© {currentYear} 打字进化</p>
          <p className="tracking-[0.04em]">逐键归档，只在本机</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
