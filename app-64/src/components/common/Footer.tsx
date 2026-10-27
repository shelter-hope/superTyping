import React from 'react';
import { Link } from 'react-router-dom';

/* 工作台页脚：轻目录，不承担打字营销长文 */
const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="mt-auto border-t border-border bg-background/60">
      <div className="container py-10">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-12">
          <div className="md:col-span-5">
            <div className="flex items-baseline gap-2">
              <span className="mb-[3px] h-[6px] w-[6px] rounded-full bg-accent" aria-hidden />
              <span className="display-serif text-[19px] tracking-[0.02em]">实验台</span>
            </div>
            <p className="mt-4 max-w-sm text-[13px] leading-[1.9] text-muted-foreground">
              综合性实验工作台：打字进化、群体智能与其它可插拔应用共享同一壳层。
            </p>
          </div>

          <div className="md:col-span-3">
            <p className="label-mono mb-4">应用</p>
            <ul className="space-y-2.5 text-[13px]">
              <li>
                <Link to="/apps/typing" className="text-muted-foreground transition-colors hover:text-foreground">
                  打字进化
                </Link>
              </li>
              <li>
                <Link to="/apps/lab" className="text-muted-foreground transition-colors hover:text-foreground">
                  群体智能
                </Link>
              </li>
              <li>
                <Link to="/apps/lab/history" className="text-muted-foreground transition-colors hover:text-foreground">
                  实验历史
                </Link>
              </li>
            </ul>
          </div>

          <div className="md:col-span-4">
            <p className="label-mono mb-4">入口</p>
            <ul className="space-y-2.5 text-[13px]">
              <li>
                <Link to="/" className="text-muted-foreground transition-colors hover:text-foreground">
                  启动器
                </Link>
              </li>
              <li>
                <Link to="/apps/typing/prepare" className="text-muted-foreground transition-colors hover:text-foreground">
                  开始一局
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-col gap-2 border-t border-border pt-6 text-[11px] text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p className="mono-num tracking-[0.04em]">© {currentYear} 实验台</p>
          <p className="tracking-[0.04em]">模块可插拔 · 数据按应用隔离</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
